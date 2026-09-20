import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import Tasks from "./Tasks.jsx";

const mocks = vi.hoisted(() => ({
  getAllTasks: vi.fn(),
  createTask: vi.fn(),
  updateTask: vi.fn(),
  archiveTask: vi.fn(),
  requestChanges: vi.fn(),
  acceptTask: vi.fn(),
  declineTask: vi.fn(),
  updateChecklistItem: vi.fn(),
  getTaskActivities: vi.fn(),
  getAttachments: vi.fn(),
  uploadAttachment: vi.fn(),
  downloadAttachment: vi.fn(),
  updateProgress: vi.fn(),
}));

vi.mock("../services/taskService.js", () => ({
  default: {
    getAllTasks: mocks.getAllTasks,
    createTask: mocks.createTask,
    updateTask: mocks.updateTask,
    archiveTask: mocks.archiveTask,
    requestChanges: mocks.requestChanges,
    acceptTask: mocks.acceptTask,
    declineTask: mocks.declineTask,
    updateChecklistItem: mocks.updateChecklistItem,
    getTaskActivities: mocks.getTaskActivities,
    getAttachments: mocks.getAttachments,
    uploadAttachment: mocks.uploadAttachment,
    downloadAttachment: mocks.downloadAttachment,
    updateProgress: mocks.updateProgress,
  },
}));

const task = {
  id: 1,
  title: "Prepare monthly report",
  description: "Summarize the monthly performance figures.",
  employeeName: "Alex Smith",
  status: "IN_PROGRESS",
  progress: 60,
  timeAllowedMinutes: 120,
  deadline: "2026-09-20T10:00:00Z",
  createdByUserId: 1,
};

describe("Tasks", () => {
  beforeEach(() => {
    mocks.getAllTasks.mockReset();
    mocks.createTask.mockReset();
    mocks.updateTask.mockReset();
    mocks.archiveTask.mockReset();
    mocks.requestChanges.mockReset();
    mocks.acceptTask.mockReset();
    mocks.declineTask.mockReset();
    mocks.updateChecklistItem.mockReset();
    mocks.getTaskActivities.mockReset();
    mocks.getTaskActivities.mockResolvedValue([]);
    mocks.getAttachments.mockReset();
    mocks.getAttachments.mockResolvedValue([]);
    mocks.uploadAttachment.mockReset();
    mocks.downloadAttachment.mockReset();
    mocks.updateProgress.mockReset();
    localStorage.clear();
    localStorage.setItem("userId", "1");
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("shows a loading state while tasks are loading", () => {
    mocks.getAllTasks.mockReturnValue(new Promise(() => {}));

    render(<Tasks />);

    expect(screen.getByText("Loading tasks...")).toBeInTheDocument();
  });

  it("shows an empty state when there are no tasks", async () => {
    mocks.getAllTasks.mockResolvedValue([]);

    render(<Tasks />);

    expect(await screen.findByText("No tasks yet")).toBeInTheDocument();
  });

  it("displays task details, status, and progress", async () => {
    mocks.getAllTasks.mockResolvedValue([task]);

    render(<Tasks />);

    expect(await screen.findByText(task.title)).toBeInTheDocument();
    expect(screen.getByTestId("task-1-status")).toHaveTextContent("In Progress");
    expect(screen.getByText("2h 0m")).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: `${task.title} progress` }))
      .toHaveAttribute("aria-valuenow", "60");
  });

  it("opens task details and loads its activity history", async () => {
    mocks.getAllTasks.mockResolvedValue([{
      ...task,
      checklistItems: [{ id: 12, description: "Review data", completed: true }],
    }]);
    mocks.getTaskActivities.mockResolvedValue([{
      type: "TASK_ACCEPTED",
      message: "Task accepted.",
      actorUsername: "alex",
      createdAt: "2026-09-19T08:00:00Z",
    }]);

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    const dialog = await screen.findByRole("dialog", { name: "Task Details" });
    expect(dialog).toHaveTextContent("Assigned employee");
    expect(dialog).toHaveTextContent("Review data");
    expect(dialog).toHaveTextContent("Activity History");
    expect(dialog).toHaveTextContent("Task accepted.");
    expect(mocks.getTaskActivities).toHaveBeenCalledWith(1);
  });

  it("loads attachments only after opening the details for a task", async () => {
    mocks.getAllTasks.mockResolvedValue([task]);

    render(<Tasks />);

    await screen.findByText(task.title);
    expect(mocks.getAttachments).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "View Details" }));
    await waitFor(() => expect(mocks.getAttachments).toHaveBeenCalledWith(task.id));
  });

  it("shows a retryable load error", async () => {
    mocks.getAllTasks
      .mockRejectedValueOnce(new ApiError(500, { message: "Tasks are unavailable" }))
      .mockResolvedValueOnce([task]);

    render(<Tasks />);

    expect(await screen.findByRole("alert"))
      .toHaveTextContent("The server encountered a problem. Please try again later.");
    expect(screen.queryByText("No tasks yet")).not.toBeInTheDocument();

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Retry" }));
      await Promise.resolve();
      await Promise.resolve();
    });

    await waitFor(() => expect(mocks.getAllTasks).toHaveBeenCalledTimes(2));
    expect(await screen.findByText(task.title)).toBeInTheDocument();
  });

  it("polls tasks every 10 seconds without showing the full-page loading state", async () => {
    vi.useFakeTimers();
    mocks.getAllTasks
      .mockResolvedValueOnce([task])
      .mockResolvedValueOnce([{ ...task, progress: 70 }]);

    render(<Tasks />);

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText(task.title)).toBeInTheDocument();
    expect(screen.queryByText("Loading tasks...")).not.toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(10000);
      await Promise.resolve();
    });

    expect(mocks.getAllTasks).toHaveBeenCalledTimes(2);
    expect(screen.queryByText("Loading tasks...")).not.toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: `${task.title} progress` }))
      .toHaveAttribute("aria-valuenow", "70");
  });

  it("filters tasks by status and treats overdue tasks separately from in-progress tasks", async () => {
    const now = Date.now();
    mocks.getAllTasks.mockResolvedValue([
      { ...task, id: 1, title: "Pending task", status: "PENDING", deadline: null },
      { ...task, id: 2, title: "Active task", status: "IN_PROGRESS", deadline: new Date(now + 3600000).toISOString() },
      { ...task, id: 3, title: "Late task", status: "IN_PROGRESS", deadline: new Date(now - 60000).toISOString() },
      { ...task, id: 4, title: "Finished task", status: "COMPLETED", deadline: null },
      { ...task, id: 5, title: "Declined task", status: "DECLINED", deadline: null },
    ]);

    render(<Tasks />);

    await screen.findByText("Late task");
    fireEvent.click(screen.getByRole("button", { name: "In Progress" }));
    expect(screen.getByText("Active task")).toBeInTheDocument();
    expect(screen.queryByText("Late task")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Overdue" }));
    expect(screen.getByText("Late task")).toBeInTheDocument();
    expect(screen.getByText("Deadline status")).toBeInTheDocument();
    expect(screen.queryByText("Active task")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Completed" }));
    expect(screen.getByText("Finished task")).toBeInTheDocument();
    expect(screen.queryByText("Late task")).not.toBeInTheDocument();
  });

  it("updates the selected filter when polling returns changed tasks", async () => {
    vi.useFakeTimers();
    const pendingTask = { ...task, id: 1, title: "Review report", status: "PENDING", deadline: null };
    const completedTask = { ...pendingTask, status: "COMPLETED", progress: 100 };
    mocks.getAllTasks
      .mockResolvedValueOnce([pendingTask])
      .mockResolvedValueOnce([completedTask]);

    render(<Tasks />);

    await act(async () => {
      await Promise.resolve();
    });
    fireEvent.click(screen.getByRole("button", { name: "Completed" }));
    expect(screen.getByText("No completed tasks")).toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(10000);
      await Promise.resolve();
    });

    expect(mocks.getAllTasks).toHaveBeenCalledTimes(2);
    expect(screen.getByText("Review report")).toBeInTheDocument();
    expect(screen.queryByText("No completed tasks")).not.toBeInTheDocument();
  });

  it("keeps the current task list when a background refresh fails", async () => {
    vi.useFakeTimers();
    mocks.getAllTasks
      .mockResolvedValueOnce([task])
      .mockRejectedValueOnce(new ApiError(500, { message: "Tasks are unavailable" }));

    render(<Tasks />);

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText(task.title)).toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(10000);
      await Promise.resolve();
    });

    expect(screen.getByText(task.title)).toBeInTheDocument();
    expect(screen.queryByText("Loading tasks...")).not.toBeInTheDocument();
  });

  it("cleans up task polling when unmounted", async () => {
    vi.useFakeTimers();
    mocks.getAllTasks.mockResolvedValue([task]);
    const clearIntervalSpy = vi.spyOn(window, "clearInterval");

    const { unmount } = render(<Tasks />);

    await act(async () => {
      await Promise.resolve();
    });
    unmount();

    await act(async () => {
      vi.advanceTimersByTime(20000);
    });

    expect(clearIntervalSpy).toHaveBeenCalled();
    expect(mocks.getAllTasks).toHaveBeenCalledTimes(1);
  });

  it("prevents overlapping polling requests", async () => {
    vi.useFakeTimers();
    let resolveRequest;
    mocks.getAllTasks.mockImplementation(() => new Promise((resolve) => {
      resolveRequest = resolve;
    }));

    render(<Tasks />);

    await act(async () => {
      vi.advanceTimersByTime(30000);
    });
    expect(mocks.getAllTasks).toHaveBeenCalledTimes(1);

    await act(async () => {
      resolveRequest([task]);
      await Promise.resolve();
    });
  });

  it("shows Assign Task only to an administrator", async () => {
    localStorage.setItem("role", "ADMIN");
    mocks.getAllTasks.mockResolvedValue([]);

    render(<Tasks />);

    expect(await screen.findByRole("button", { name: "Assign Task" })).toBeInTheDocument();
  });

  it("does not show Assign Task to an employee", async () => {
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([]);

    render(<Tasks />);

    await screen.findByText("No tasks yet");
    expect(screen.queryByRole("button", { name: "Assign Task" })).not.toBeInTheDocument();
  });

  it("shows Edit Task only to an administrator for pending and in-progress tasks", async () => {
    localStorage.setItem("role", "ADMIN");
    mocks.getAllTasks.mockResolvedValue([
      { ...task, id: 1, status: "PENDING" },
      { ...task, id: 2, title: "Active task", status: "IN_PROGRESS" },
      { ...task, id: 3, title: "Completed task", status: "COMPLETED" },
      { ...task, id: 4, title: "Declined task", status: "DECLINED" },
    ]);

    render(<Tasks />);

    await screen.findByText("Active task");
    expect(screen.getAllByRole("button", { name: "Edit Task" })).toHaveLength(2);
  });

  it("shows management actions only to the administrator who created the task", async () => {
    localStorage.setItem("role", "ADMIN");
    localStorage.setItem("userId", "7");
    mocks.getAllTasks.mockResolvedValue([
      { ...task, id: 1, title: "Own active task", status: "IN_PROGRESS", createdByUserId: "7" },
      { ...task, id: 2, title: "Own completed task", status: "COMPLETED", progress: 100, createdByUserId: 7 },
      { ...task, id: 3, title: "Another active task", status: "IN_PROGRESS", createdByUserId: 8 },
      { ...task, id: 4, title: "Another completed task", status: "COMPLETED", progress: 100, createdByUserId: "8" },
    ]);

    render(<Tasks />);

    await screen.findByText("Own active task");
    expect(screen.getAllByRole("button", { name: "Edit Task" })).toHaveLength(1);
    expect(screen.getAllByRole("button", { name: "Archive Task" })).toHaveLength(2);
    expect(screen.getAllByRole("button", { name: "Request Changes" })).toHaveLength(1);
  });

  it("does not show Edit Task to an employee", async () => {
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([{ ...task, status: "PENDING" }]);

    render(<Tasks />);

    await screen.findByText(task.title);
    expect(screen.queryByRole("button", { name: "Edit Task" })).not.toBeInTheDocument();
  });

  it("shows Request Changes only to an administrator for completed tasks", async () => {
    const completedTask = { ...task, status: "COMPLETED", progress: 100 };
    localStorage.setItem("role", "ADMIN");
    mocks.getAllTasks.mockResolvedValue([completedTask]);

    const { unmount } = render(<Tasks />);

    expect(await screen.findByRole("button", { name: "Request Changes" })).toBeInTheDocument();

    unmount();
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([completedTask]);
    render(<Tasks />);

    await screen.findByText(completedTask.title);
    expect(screen.queryByRole("button", { name: "Request Changes" })).not.toBeInTheDocument();
  });

  it("shows archive confirmation and removes the task after a successful archive", async () => {
    localStorage.setItem("role", "ADMIN");
    mocks.getAllTasks
      .mockResolvedValueOnce([task])
      .mockResolvedValueOnce([]);
    mocks.archiveTask.mockResolvedValue({ ...task, archived: true });

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "Archive Task" }));
    const dialog = screen.getByRole("dialog", { name: "Archive Task?" });
    expect(dialog).toHaveTextContent("Are you sure you want to archive this task?");
    fireEvent.click(within(dialog).getByRole("button", { name: "Archive" }));

    await waitFor(() => expect(mocks.archiveTask).toHaveBeenCalledWith(1));
    await waitFor(() => expect(mocks.getAllTasks).toHaveBeenCalledTimes(2));
    expect(screen.queryByText(task.title)).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Task archived successfully.");
    expect(screen.queryByRole("dialog", { name: "Archive Task?" })).not.toBeInTheDocument();
  });

  it("does not show Archive Task to an employee", async () => {
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([task]);

    render(<Tasks />);

    await screen.findByText(task.title);
    expect(screen.queryByRole("button", { name: "Archive Task" })).not.toBeInTheDocument();
  });

  it("locks archive requests until the current request completes", async () => {
    localStorage.setItem("role", "ADMIN");
    let resolveArchive;
    mocks.getAllTasks.mockResolvedValue([task]);
    mocks.archiveTask.mockImplementation(() => new Promise((resolve) => {
      resolveArchive = resolve;
    }));

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "Archive Task" }));
    const dialog = screen.getByRole("dialog", { name: "Archive Task?" });
    const archiveButton = within(dialog).getByRole("button", { name: "Archive" });
    fireEvent.click(archiveButton);
    fireEvent.click(archiveButton);

    expect(mocks.archiveTask).toHaveBeenCalledTimes(1);
    resolveArchive({ ...task, archived: true });
    await waitFor(() => expect(screen.getByRole("status"))
      .toHaveTextContent("Task archived successfully."));
  });

  it("lets an employee accept a pending task and refreshes its deadline", async () => {
    const pendingTask = { ...task, status: "PENDING", progress: 0, deadline: null };
    const acceptedTask = {
      ...pendingTask,
      status: "IN_PROGRESS",
      deadline: "2026-09-20T10:00:00Z",
    };
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks
      .mockResolvedValueOnce([pendingTask])
      .mockResolvedValueOnce([acceptedTask]);
    mocks.acceptTask.mockResolvedValue(acceptedTask);

    render(<Tasks />);

    await screen.findByRole("button", { name: "Accept" });
    fireEvent.click(screen.getByRole("button", { name: "Accept" }));
    const dialog = screen.getByRole("dialog", { name: "Accept Task?" });
    fireEvent.click(within(dialog).getByRole("button", { name: "Accept Task" }));

    await waitFor(() => expect(mocks.acceptTask).toHaveBeenCalledWith(1));
    await waitFor(() => expect(mocks.getAllTasks).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.getByTestId("task-1-status")).toHaveTextContent("In Progress"));
    expect(screen.getByRole("status")).toHaveTextContent("Task accepted successfully.");
    expect(screen.queryByText("Not set")).not.toBeInTheDocument();
  });

  it("lets an employee decline a pending task and refreshes the task status", async () => {
    const pendingTask = { ...task, status: "PENDING", progress: 0, deadline: null };
    const declinedTask = { ...pendingTask, status: "DECLINED" };
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks
      .mockResolvedValueOnce([pendingTask])
      .mockResolvedValueOnce([declinedTask]);
    mocks.declineTask.mockResolvedValue(declinedTask);

    render(<Tasks />);

    await screen.findByRole("button", { name: "Decline" });
    fireEvent.click(screen.getByRole("button", { name: "Decline" }));
    const dialog = screen.getByRole("dialog", { name: "Decline Task?" });
    fireEvent.click(within(dialog).getByRole("button", { name: "Decline Task" }));

    await waitFor(() => expect(mocks.declineTask).toHaveBeenCalledWith(1));
    await waitFor(() => expect(screen.getByTestId("task-1-status")).toHaveTextContent("Declined"));
    expect(screen.getByRole("status")).toHaveTextContent("Task declined successfully.");
  });

  it("shows action errors and never renders employee controls for an administrator", async () => {
    const pendingTask = { ...task, status: "PENDING", progress: 0, deadline: null };
    localStorage.setItem("role", "ADMIN");
    mocks.getAllTasks.mockResolvedValue([pendingTask]);

    render(<Tasks />);

    await screen.findByText(pendingTask.title);
    expect(screen.queryByRole("button", { name: "Accept" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Decline" })).not.toBeInTheDocument();
  });

  it("shows an error when accepting a task fails", async () => {
    const pendingTask = { ...task, status: "PENDING", progress: 0, deadline: null };
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([pendingTask]);
    mocks.acceptTask.mockRejectedValue(new ApiError(409, { message: "Task is no longer pending" }));

    render(<Tasks />);

    await screen.findByRole("button", { name: "Accept" });
    fireEvent.click(screen.getByRole("button", { name: "Accept" }));
    fireEvent.click(within(screen.getByRole("dialog", { name: "Accept Task?" }))
      .getByRole("button", { name: "Accept Task" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Task is no longer pending");
  });

  it("formats long durations and remaining time in days, hours, and minutes", async () => {
    const durationMinutes = (10 * 24 * 60) + (8 * 60) + 30;
    const inProgressTask = {
      ...task,
      status: "IN_PROGRESS",
      timeAllowedMinutes: durationMinutes,
      deadline: new Date(Date.now() + (durationMinutes * 60 * 1000)).toISOString(),
    };
    mocks.getAllTasks.mockResolvedValue([inProgressTask]);

    render(<Tasks />);

    expect(await screen.findByText("10d 8h 30m")).toBeInTheDocument();
    expect(screen.getByText("10d 8h 30m remaining")).toBeInTheDocument();
  });

  it("marks an in-progress task as overdue after its deadline", async () => {
    mocks.getAllTasks.mockResolvedValue([{
      ...task,
      status: "IN_PROGRESS",
      deadline: new Date(Date.now() - 60000).toISOString(),
    }]);

    render(<Tasks />);

    expect(await screen.findByText("Deadline status")).toBeInTheDocument();
    expect(screen.getByText("Deadline status")).toBeInTheDocument();
    expect(screen.queryByText("Remaining time")).not.toBeInTheDocument();
  });

  it("updates remaining time locally every minute", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-09-20T10:00:00Z"));
    mocks.getAllTasks.mockResolvedValue([{
      ...task,
      deadline: "2026-09-20T18:25:00Z",
    }]);

    render(<Tasks />);

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText("8h 25m remaining")).toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(60000);
    });

    expect(screen.getByText("8h 24m remaining")).toBeInTheDocument();
  });

  it("derives overdue after the deadline passes without changing task status", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-09-20T10:00:00Z"));
    mocks.getAllTasks.mockResolvedValue([{
      ...task,
      deadline: "2026-09-20T10:00:30Z",
    }]);

    render(<Tasks />);

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText("1m remaining")).toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(60000);
    });

    expect(screen.getByText("Deadline status")).toBeInTheDocument();
    expect(screen.getByTestId("task-1-status")).toHaveTextContent("In Progress");
  });

  it("lets an employee complete checklist items and refreshes progress from the backend", async () => {
    const inProgressTask = {
      ...task,
      status: "IN_PROGRESS",
      progress: 50,
      checklistItems: [
        { id: 101, description: "Review figures", completed: false },
        { id: 102, description: "Send report", completed: true },
      ],
    };
    const completedTask = {
      ...inProgressTask,
      status: "COMPLETED",
      progress: 100,
      checklistItems: [
        { id: 101, description: "Review figures", completed: true },
        { id: 102, description: "Send report", completed: true },
      ],
    };
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks
      .mockResolvedValueOnce([inProgressTask])
      .mockResolvedValueOnce([completedTask]);
    mocks.updateChecklistItem.mockResolvedValue(completedTask);

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    expect(await screen.findByText("1 of 2 completed")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("checkbox", { name: "Review figures" }));

    await waitFor(() => expect(mocks.updateChecklistItem).toHaveBeenCalledWith(1, 101, true));
    expect(await screen.findByText("2 of 2 completed")).toBeInTheDocument();
    expect(screen.getByTestId("task-1-status")).toHaveTextContent("Completed");
    expect(screen.getByRole("progressbar", { name: `${task.title} progress` }))
      .toHaveAttribute("aria-valuenow", "100");
  });

  it("shows checklist items read-only to an administrator", async () => {
    localStorage.setItem("role", "ADMIN");
    mocks.getAllTasks.mockResolvedValue([{
      ...task,
      status: "IN_PROGRESS",
      checklistItems: [{ id: 101, description: "Review figures", completed: false }],
    }]);

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    const checkbox = await screen.findByRole("checkbox", { name: "Review figures" });
    expect(checkbox).toBeDisabled();
    expect(mocks.updateChecklistItem).not.toHaveBeenCalled();
  });

  it("shows an error when checklist update fails", async () => {
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([{
      ...task,
      status: "IN_PROGRESS",
      checklistItems: [{ id: 101, description: "Review figures", completed: false }],
    }]);
    mocks.updateChecklistItem.mockRejectedValue(new ApiError(409, {
      message: "Checklist items can only be updated for in-progress tasks",
    }));

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    fireEvent.click(await screen.findByRole("checkbox", { name: "Review figures" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Checklist items can only be updated for in-progress tasks"
    );
  });

  it("lets an employee update manual progress and refreshes it from the backend", async () => {
    const updatedTask = { ...task, progress: 80 };
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks
      .mockResolvedValueOnce([task])
      .mockResolvedValueOnce([updatedTask]);
    mocks.updateProgress.mockResolvedValue(updatedTask);

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    const progressInput = await screen.findByLabelText("Progress percentage");
    fireEvent.change(progressInput, { target: { value: "80" } });
    fireEvent.click(screen.getByRole("button", { name: "Update Progress" }));

    await waitFor(() => expect(mocks.updateProgress).toHaveBeenCalledWith(1, 80));
    await waitFor(() => expect(mocks.getAllTasks).toHaveBeenCalledTimes(2));
    expect(screen.getByRole("progressbar", { name: `${task.title} progress` }))
      .toHaveAttribute("aria-valuenow", "80");
  });

  it("prevents a manual progress decrease before making a request", async () => {
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([task]);

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    fireEvent.change(await screen.findByLabelText("Progress percentage"), { target: { value: "20" } });
    fireEvent.click(screen.getByRole("button", { name: "Update Progress" }));

    expect(screen.getByText("Progress cannot be decreased.")).toBeInTheDocument();
    expect(mocks.updateProgress).not.toHaveBeenCalled();
  });

  it("shows completion when a 100 percent update is confirmed by the backend", async () => {
    const completedTask = { ...task, status: "COMPLETED", progress: 100 };
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks
      .mockResolvedValueOnce([task])
      .mockResolvedValueOnce([completedTask]);
    mocks.updateProgress.mockResolvedValue(completedTask);

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    fireEvent.change(await screen.findByLabelText("Progress percentage"), { target: { value: "100" } });
    fireEvent.click(screen.getByRole("button", { name: "Update Progress" }));

    await waitFor(() => expect(screen.getByTestId("task-1-status")).toHaveTextContent("Completed"));
    expect(screen.getByRole("progressbar", { name: `${task.title} progress` }))
      .toHaveAttribute("aria-valuenow", "100");
  });

  it("locks a manual progress update while the request is pending", async () => {
    let resolveUpdate;
    const updatedTask = { ...task, progress: 80 };
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks
      .mockResolvedValueOnce([task])
      .mockResolvedValueOnce([updatedTask]);
    mocks.updateProgress.mockImplementation(() => new Promise((resolve) => {
      resolveUpdate = resolve;
    }));

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    fireEvent.change(await screen.findByLabelText("Progress percentage"), { target: { value: "80" } });
    fireEvent.click(screen.getByRole("button", { name: "Update Progress" }));
    fireEvent.click(screen.getByRole("button", { name: "Updating..." }));

    expect(mocks.updateProgress).toHaveBeenCalledTimes(1);
    resolveUpdate(updatedTask);
    await waitFor(() => expect(screen.getByRole("progressbar", { name: `${task.title} progress` }))
      .toHaveAttribute("aria-valuenow", "80"));
  });

  it("hides manual progress controls for checklist, non-in-progress, and administrator tasks", async () => {
    const checklistTask = {
      ...task,
      id: 2,
      title: "Checklist task",
      checklistItems: [{ id: 101, description: "Review figures", completed: false }],
    };
    const pendingTask = { ...task, id: 3, title: "Pending task", status: "PENDING" };
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([checklistTask, pendingTask]);

    const { unmount } = render(<Tasks />);

    await screen.findByText("Checklist task");
    expect(screen.queryByLabelText("Progress percentage")).not.toBeInTheDocument();

    fireEvent.click(screen.getAllByRole("button", { name: "View Details" })[0]);
    expect(screen.queryByLabelText("Progress percentage")).not.toBeInTheDocument();

    unmount();
    localStorage.setItem("role", "ADMIN");
    mocks.getAllTasks.mockResolvedValue([task]);
    render(<Tasks />);

    await screen.findByText(task.title);
    expect(screen.queryByLabelText("Progress percentage")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "View Details" }));
    expect(screen.queryByLabelText("Progress percentage")).not.toBeInTheDocument();
  });

  it("shows a backend error when manual progress update fails", async () => {
    localStorage.setItem("role", "EMPLOYEE");
    mocks.getAllTasks.mockResolvedValue([task]);
    mocks.updateProgress.mockRejectedValue(new ApiError(409, {
      message: "Only in-progress tasks can have their progress updated",
    }));

    render(<Tasks />);

    fireEvent.click(await screen.findByRole("button", { name: "View Details" }));
    fireEvent.click(await screen.findByRole("button", { name: "Update Progress" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Only in-progress tasks can have their progress updated"
    );
  });
});
