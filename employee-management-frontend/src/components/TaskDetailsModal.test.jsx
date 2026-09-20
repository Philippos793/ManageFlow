import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import TaskDetailsModal from "./TaskDetailsModal.jsx";

const mocks = vi.hoisted(() => ({
  getTaskActivities: vi.fn(),
  getAttachments: vi.fn(),
  uploadAttachment: vi.fn(),
  downloadAttachment: vi.fn(),
}));

vi.mock("../services/taskService.js", () => ({
  default: {
    getTaskActivities: mocks.getTaskActivities,
    getAttachments: mocks.getAttachments,
    uploadAttachment: mocks.uploadAttachment,
    downloadAttachment: mocks.downloadAttachment,
  },
}));

const task = {
  id: 8,
  title: "Prepare report",
  description: "Finish the monthly report.",
  employeeName: "Alex Smith",
  status: "IN_PROGRESS",
  progress: 50,
  timeAllowedMinutes: 1500,
  deadline: "2026-09-20T12:00:00Z",
  checklistItems: [{ id: 1, description: "Collect figures", completed: true }],
};

describe("TaskDetailsModal", () => {
  beforeEach(() => {
    mocks.getTaskActivities.mockReset();
    mocks.getAttachments.mockReset();
    mocks.getAttachments.mockResolvedValue([]);
  });

  it("shows organized task data and a chronological activity timeline", async () => {
    mocks.getTaskActivities.mockResolvedValue([
      { type: "TASK_CREATED", message: "Task created.", actorUsername: "admin", createdAt: "2026-09-18T08:00:00Z" },
      { type: "TASK_ACCEPTED", message: "Task accepted.", actorUsername: "alex", createdAt: "2026-09-19T08:00:00Z" },
    ]);

    render(<TaskDetailsModal open task={task} onClose={vi.fn()} isEmployee currentTime={new Date("2026-09-19T12:00:00Z").getTime()} />);

    expect(screen.getByRole("dialog", { name: "Task Details" })).toHaveTextContent("Prepare report");
    expect(screen.getByText("Alex Smith")).toBeInTheDocument();
    expect(screen.getByText("1d 1h 0m")).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Collect figures" })).not.toBeDisabled();
    expect(await screen.findByText("Task accepted.")).toBeInTheDocument();
    const timeline = screen.getByRole("list", { name: "Activity timeline" });
    expect(timeline).toHaveTextContent("Task created.");
    expect(timeline).toHaveTextContent("Task accepted.");
  });

  it("shows remaining time before the deadline, Overdue after it, and a new countdown after rework", async () => {
    mocks.getTaskActivities.mockResolvedValue([]);
    const currentTime = new Date("2026-09-20T10:00:00Z").getTime();
    const { rerender } = render(
      <TaskDetailsModal
        open
        task={{ ...task, deadline: "2026-09-20T11:00:00Z" }}
        onClose={vi.fn()}
        isEmployee
        currentTime={currentTime}
      />
    );

    expect(screen.getByText("Remaining time")).toBeInTheDocument();
    expect(screen.getByText("1h 0m remaining")).toBeInTheDocument();

    rerender(
      <TaskDetailsModal
        open
        task={{ ...task, deadline: "2026-09-20T09:59:00Z" }}
        onClose={vi.fn()}
        isEmployee
        currentTime={currentTime}
      />
    );

    expect(screen.getByText("Deadline status")).toBeInTheDocument();
    expect(screen.getByText("Overdue")).toBeInTheDocument();
    expect(screen.queryByText("Remaining time")).not.toBeInTheDocument();

    rerender(
      <TaskDetailsModal
        open
        task={{ ...task, deadline: "2026-09-20T12:30:00Z" }}
        onClose={vi.fn()}
        isEmployee
        currentTime={currentTime}
      />
    );

    expect(screen.getByText("Remaining time")).toBeInTheDocument();
    expect(screen.getByText("2h 30m remaining")).toBeInTheDocument();
    expect(screen.queryByText("Overdue")).not.toBeInTheDocument();
  });

  it("shows an activity history loading state", async () => {
    mocks.getTaskActivities.mockReturnValue(new Promise(() => {}));

    render(<TaskDetailsModal open task={task} onClose={vi.fn()} isEmployee />);

    expect(await screen.findByText("Loading activity history...")).toBeInTheDocument();
  });

  it("shows an empty activity state", async () => {
    mocks.getTaskActivities.mockResolvedValue([]);

    render(<TaskDetailsModal open task={task} onClose={vi.fn()} isEmployee />);

    expect(await screen.findByText("No activity recorded yet.")).toBeInTheDocument();
  });

  it("shows a retryable activity history error", async () => {
    mocks.getTaskActivities
      .mockRejectedValueOnce(new ApiError(500, { message: "Unavailable" }))
      .mockResolvedValueOnce([]);

    render(<TaskDetailsModal open task={task} onClose={vi.fn()} isEmployee />);

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    await waitFor(() => expect(mocks.getTaskActivities).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("No activity recorded yet.")).toBeInTheDocument();
  });

  it("closes from the close control", async () => {
    mocks.getTaskActivities.mockResolvedValue([]);
    const onClose = vi.fn();

    render(<TaskDetailsModal open task={task} onClose={onClose} isEmployee />);

    fireEvent.click(screen.getByRole("button", { name: "Close task details" }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
