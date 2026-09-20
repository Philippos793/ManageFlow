import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import TaskAttachments from "./TaskAttachments.jsx";

const mocks = vi.hoisted(() => ({
  getAttachments: vi.fn(),
  uploadAttachment: vi.fn(),
  downloadAttachment: vi.fn(),
}));

vi.mock("../services/taskService.js", () => ({
  default: {
    getAttachments: mocks.getAttachments,
    uploadAttachment: mocks.uploadAttachment,
    downloadAttachment: mocks.downloadAttachment,
  },
}));

const attachment = {
  id: 21,
  attachmentType: "ADMIN_RESOURCE",
  originalFilename: "instructions.pdf",
  fileSize: 1536,
  uploaderUsername: "admin",
  uploadedAt: "2026-09-20T10:30:00Z",
};

describe("TaskAttachments", () => {
  let originalCreateObjectUrl;
  let originalRevokeObjectUrl;

  beforeEach(() => {
    mocks.getAttachments.mockReset();
    mocks.uploadAttachment.mockReset();
    mocks.downloadAttachment.mockReset();
    originalCreateObjectUrl = URL.createObjectURL;
    originalRevokeObjectUrl = URL.revokeObjectURL;
    URL.createObjectURL = vi.fn(() => "blob:attachment");
    URL.revokeObjectURL = vi.fn();
  });

  afterEach(() => {
    URL.createObjectURL = originalCreateObjectUrl;
    URL.revokeObjectURL = originalRevokeObjectUrl;
    vi.restoreAllMocks();
  });

  it("shows a loading state and then the attachment metadata", async () => {
    mocks.getAttachments.mockResolvedValue([attachment]);

    render(<TaskAttachments taskId={1} isAdmin={false} />);

    expect(screen.getByText("Loading attachments...")).toBeInTheDocument();
    expect(await screen.findByText("instructions.pdf")).toBeInTheDocument();
    expect(screen.getByText(/1\.5 KB/)).toBeInTheDocument();
    expect(screen.queryByLabelText("Upload Admin Resource")).not.toBeInTheDocument();
  });

  it("lets an administrator upload a resource and refreshes attachments", async () => {
    const file = new File(["document"], "instructions.pdf", { type: "application/pdf" });
    mocks.getAttachments.mockResolvedValue([]);
    mocks.uploadAttachment.mockResolvedValue({ ...attachment });

    render(<TaskAttachments taskId={1} taskStatus="IN_PROGRESS" isAdmin />);

    await screen.findByText("No admin resources yet.");
    fireEvent.change(screen.getByLabelText("Upload Admin Resource"), { target: { files: [file] } });
    fireEvent.click(screen.getByRole("button", { name: "Upload" }));

    await waitFor(() => expect(mocks.uploadAttachment).toHaveBeenCalledWith(1, file));
    await waitFor(() => expect(mocks.getAttachments).toHaveBeenCalledTimes(2));
  });

  it("shows the server max-size validation message", async () => {
    const file = new File(["large"], "large.pdf", { type: "application/pdf" });
    mocks.getAttachments.mockResolvedValue([]);
    mocks.uploadAttachment.mockRejectedValue(new ApiError(400, {
      message: "Attachment exceeds the maximum allowed file size",
    }));

    render(<TaskAttachments taskId={1} taskStatus="IN_PROGRESS" isAdmin />);

    await screen.findByText("No admin resources yet.");
    fireEvent.change(screen.getByLabelText("Upload Admin Resource"), { target: { files: [file] } });
    fireEvent.click(screen.getByRole("button", { name: "Upload" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Attachment exceeds the maximum allowed file size"
    );
  });

  it("lets an assigned employee download an admin resource", async () => {
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
    mocks.getAttachments.mockResolvedValue([attachment]);
    mocks.downloadAttachment.mockResolvedValue(new Blob(["document"]));

    render(<TaskAttachments taskId={1} isAdmin={false} isEmployee taskStatus="IN_PROGRESS" />);

    fireEvent.click(await screen.findByRole("button", { name: "Download" }));

    await waitFor(() => expect(mocks.downloadAttachment).toHaveBeenCalledWith(1, 21));
    expect(clickSpy).toHaveBeenCalled();
  });

  it("lets an employee upload a submission only while the task is in progress", async () => {
    const file = new File(["submission"], "completed-report.pdf", { type: "application/pdf" });
    mocks.getAttachments.mockResolvedValue([]);
    mocks.uploadAttachment.mockResolvedValue({
      id: 22,
      attachmentType: "EMPLOYEE_SUBMISSION",
      originalFilename: "completed-report.pdf",
      fileSize: 10,
    });

    render(<TaskAttachments taskId={1} taskStatus="IN_PROGRESS" isAdmin={false} isEmployee />);

    await screen.findByText("No employee submissions yet.");
    fireEvent.change(screen.getByLabelText("Upload Employee Submission"), { target: { files: [file] } });
    fireEvent.click(screen.getByRole("button", { name: "Upload" }));

    await waitFor(() => expect(mocks.uploadAttachment).toHaveBeenCalledWith(1, file));
    await waitFor(() => expect(mocks.getAttachments).toHaveBeenCalledTimes(2));
  });

  it("does not show employee upload controls outside an in-progress task", async () => {
    mocks.getAttachments.mockResolvedValue([]);

    render(<TaskAttachments taskId={1} taskStatus="PENDING" isAdmin={false} isEmployee />);

    await screen.findByText("No employee submissions yet.");
    expect(screen.queryByLabelText("Upload Employee Submission")).not.toBeInTheDocument();
  });

  it("does not show employee upload controls for a declined task", async () => {
    mocks.getAttachments.mockResolvedValue([]);

    render(<TaskAttachments taskId={1} taskStatus="DECLINED" isAdmin={false} isEmployee />);

    await screen.findByText("No employee submissions yet.");
    expect(screen.queryByLabelText("Upload Employee Submission")).not.toBeInTheDocument();
  });

  it("does not show employee upload controls for a completed task", async () => {
    mocks.getAttachments.mockResolvedValue([]);

    render(<TaskAttachments taskId={1} taskStatus="COMPLETED" isAdmin={false} isEmployee />);

    await screen.findByText("No employee submissions yet.");
    expect(screen.queryByLabelText("Upload Employee Submission")).not.toBeInTheDocument();
  });

  it("separates admin resources from employee submissions for an administrator", async () => {
    const submission = {
      id: 22,
      attachmentType: "EMPLOYEE_SUBMISSION",
      originalFilename: "completed-report.pdf",
      fileSize: 2048,
      uploaderUsername: "alex",
      uploadedAt: "2026-09-21T10:30:00Z",
    };
    mocks.getAttachments.mockResolvedValue([attachment, submission]);

    render(<TaskAttachments taskId={1} taskStatus="IN_PROGRESS" isAdmin isEmployee={false} />);

    expect(await screen.findByText("Admin Resources")).toBeInTheDocument();
    expect(screen.getByText("Employee Submissions")).toBeInTheDocument();
    expect(screen.getByText("instructions.pdf")).toBeInTheDocument();
    expect(screen.getByText("completed-report.pdf")).toBeInTheDocument();
    expect(screen.getByText("Uploaded by admin")).toBeInTheDocument();
    expect(screen.getByText("Uploaded by alex")).toBeInTheDocument();
    expect(screen.getByText(/Sep 20, 2026/)).toBeInTheDocument();
    expect(screen.getByText(/Sep 21, 2026/)).toBeInTheDocument();
  });

  it("shows the appropriate bidirectional upload control only for active tasks", async () => {
    mocks.getAttachments.mockResolvedValue([]);
    const { rerender } = render(
      <TaskAttachments taskId={1} taskStatus="IN_PROGRESS" isAdmin isEmployee={false} />
    );

    expect(await screen.findByLabelText("Upload Admin Resource")).toBeInTheDocument();
    rerender(<TaskAttachments taskId={1} taskStatus="IN_PROGRESS" isAdmin={false} isEmployee />);
    expect(await screen.findByLabelText("Upload Employee Submission")).toBeInTheDocument();
    rerender(<TaskAttachments taskId={1} taskStatus="COMPLETED" isAdmin isEmployee={false} />);
    expect(screen.queryByLabelText("Upload Admin Resource")).not.toBeInTheDocument();
  });

  it("lets an administrator download an employee submission", async () => {
    const submission = {
      id: 22,
      attachmentType: "EMPLOYEE_SUBMISSION",
      originalFilename: "completed-report.pdf",
      fileSize: 2048,
    };
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
    mocks.getAttachments.mockResolvedValue([submission]);
    mocks.downloadAttachment.mockResolvedValue(new Blob(["submission"]));

    render(<TaskAttachments taskId={1} taskStatus="IN_PROGRESS" isAdmin isEmployee={false} />);

    fireEvent.click(await screen.findByRole("button", { name: "Download" }));

    await waitFor(() => expect(mocks.downloadAttachment).toHaveBeenCalledWith(1, 22));
    expect(clickSpy).toHaveBeenCalled();
  });
});
