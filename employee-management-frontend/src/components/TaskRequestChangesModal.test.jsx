import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import TaskRequestChangesModal from "./TaskRequestChangesModal.jsx";

const mocks = vi.hoisted(() => ({
  requestChanges: vi.fn(),
}));

vi.mock("../services/taskService.js", () => ({
  default: { requestChanges: mocks.requestChanges },
}));

const checklistTask = {
  id: 11,
  title: "Prepare report",
  status: "COMPLETED",
  checklistItems: [{ id: 21, description: "Review figures", completed: true }],
};

describe("TaskRequestChangesModal", () => {
  beforeEach(() => {
    mocks.requestChanges.mockReset();
  });

  it("requires feedback, additional time, and a new checklist item when the task has a checklist", () => {
    render(<TaskRequestChangesModal open task={checklistTask} onClose={vi.fn()} onRequested={vi.fn()} />);

    fireEvent.click(screen.getByRole("button", { name: "Request Changes" }));

    expect(screen.getByText("Feedback is required.")).toBeInTheDocument();
    expect(screen.getByText("Additional time must be greater than 0.")).toBeInTheDocument();
    expect(screen.getByText("Add at least one checklist item for the requested changes.")).toBeInTheDocument();
    expect(screen.getByLabelText("Feedback")).toHaveAttribute("aria-describedby", "request-changes-feedback-error");
    expect(screen.getByLabelText("Days")).toHaveAttribute("aria-describedby", "request-changes-time-error");
    expect(mocks.requestChanges).not.toHaveBeenCalled();
  });

  it("submits feedback, converted duration, and new checklist items", async () => {
    const onClose = vi.fn();
    const onRequested = vi.fn();
    const updatedTask = { ...checklistTask, status: "IN_PROGRESS", progress: 50 };
    mocks.requestChanges.mockResolvedValue(updatedTask);

    render(<TaskRequestChangesModal open task={checklistTask} onClose={onClose} onRequested={onRequested} />);

    fireEvent.change(screen.getByLabelText("Feedback"), { target: { value: "Please revise the totals." } });
    fireEvent.change(screen.getByLabelText("Days"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("Hours"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("Minutes"), { target: { value: "30" } });
    fireEvent.click(screen.getByRole("button", { name: "+ Add Checklist Item" }));
    fireEvent.change(screen.getByLabelText("New checklist item 1"), { target: { value: "Correct the totals" } });
    fireEvent.click(screen.getByRole("button", { name: "Request Changes" }));

    await waitFor(() => expect(mocks.requestChanges).toHaveBeenCalledWith(11, {
      feedback: "Please revise the totals.",
      additionalTimeMinutes: 1590,
      checklistItems: [{ description: "Correct the totals" }],
    }));
    expect(onRequested).toHaveBeenCalledWith(updatedTask);
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("shows the backend error and keeps the modal open when requesting changes fails", async () => {
    const taskWithoutChecklist = { ...checklistTask, checklistItems: [] };
    mocks.requestChanges.mockRejectedValue(new ApiError(409, {
      message: "Only completed tasks can have changes requested",
    }));

    render(<TaskRequestChangesModal open task={taskWithoutChecklist} onClose={vi.fn()} onRequested={vi.fn()} />);

    fireEvent.change(screen.getByLabelText("Feedback"), { target: { value: "Please revise." } });
    fireEvent.change(screen.getByLabelText("Minutes"), { target: { value: "30" } });
    fireEvent.click(screen.getByRole("button", { name: "Request Changes" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Only completed tasks can have changes requested"
    );
    expect(screen.getByRole("dialog", { name: "Request Changes" })).toBeInTheDocument();
  });
});
