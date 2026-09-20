import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import TaskEditModal from "./TaskEditModal.jsx";

const mocks = vi.hoisted(() => ({
  updateTask: vi.fn(),
}));

vi.mock("../services/taskService.js", () => ({
  default: { updateTask: mocks.updateTask },
}));

const task = {
  id: 11,
  title: "Prepare report",
  description: "September summary",
  timeAllowedMinutes: 1590,
  checklistItems: [
    { id: 21, description: "Review figures", completed: true },
    { id: 22, description: "Share draft", completed: false },
  ],
};

describe("TaskEditModal", () => {
  beforeEach(() => {
    mocks.updateTask.mockReset();
  });

  it("loads existing values, protects completed checklist items, and saves changes", async () => {
    const onClose = vi.fn();
    const onUpdated = vi.fn();
    mocks.updateTask.mockResolvedValue({ ...task, title: "Updated report" });

    render(<TaskEditModal open task={task} onClose={onClose} onUpdated={onUpdated} />);

    expect(screen.getByLabelText("Title")).toHaveValue("Prepare report");
    expect(screen.getByLabelText("Description")).toHaveValue("September summary");
    expect(screen.getByLabelText("Days")).toHaveValue(1);
    expect(screen.getByLabelText("Hours")).toHaveValue(2);
    expect(screen.getByLabelText("Minutes")).toHaveValue(30);
    expect(screen.getByRole("button", { name: "Remove checklist item 1" })).toBeDisabled();
    expect(screen.getByText("Completed items cannot be removed.")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Updated report" } });
    fireEvent.click(screen.getByRole("button", { name: "+ Add Checklist Item" }));
    fireEvent.change(screen.getByLabelText("Checklist item 3"), { target: { value: "Send final version" } });
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }));

    await waitFor(() => expect(mocks.updateTask).toHaveBeenCalledWith(11, {
      title: "Updated report",
      description: "September summary",
      timeAllowedMinutes: 1590,
      checklistItems: [
        { id: 21, description: "Review figures" },
        { id: 22, description: "Share draft" },
        { description: "Send final version" },
      ],
    }));
    expect(onUpdated).toHaveBeenCalledWith({ ...task, title: "Updated report" });
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("shows the backend error and keeps the modal open when saving fails", async () => {
    mocks.updateTask.mockRejectedValue(new ApiError(409, {
      message: "Completed and declined tasks cannot be edited",
    }));

    render(<TaskEditModal open task={task} onClose={vi.fn()} onUpdated={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Completed and declined tasks cannot be edited"
    );
    expect(screen.getByRole("dialog", { name: "Edit Task" })).toBeInTheDocument();
  });

  it("traps Tab navigation inside the dialog", () => {
    render(<TaskEditModal open task={task} onClose={vi.fn()} onUpdated={vi.fn()} />);

    const dialog = screen.getByRole("dialog", { name: "Edit Task" });
    dialog.focus();
    fireEvent.keyDown(document, { key: "Tab" });
    expect(screen.getByRole("button", { name: "Close edit task" })).toHaveFocus();

    fireEvent.keyDown(document, { key: "Tab", shiftKey: true });
    expect(screen.getByRole("button", { name: "Save Changes" })).toHaveFocus();
  });

  it("does not close with Escape while saving", async () => {
    let resolveUpdate;
    const onClose = vi.fn();
    mocks.updateTask.mockImplementation(() => new Promise((resolve) => {
      resolveUpdate = resolve;
    }));

    render(<TaskEditModal open task={task} onClose={onClose} onUpdated={vi.fn()} />);

    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }));
    expect(await screen.findByRole("button", { name: "Saving..." })).toBeDisabled();

    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).not.toHaveBeenCalled();

    resolveUpdate(task);
    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
  });
});
