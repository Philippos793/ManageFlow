import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import TaskNotifications from "./TaskNotifications.jsx";

const mocks = vi.hoisted(() => ({
  getNotifications: vi.fn(),
  markAsRead: vi.fn(),
}));

vi.mock("../services/notificationService.js", () => ({
  default: {
    getNotifications: mocks.getNotifications,
    markAsRead: mocks.markAsRead,
  },
}));

const unreadNotification = {
  id: 1,
  type: "TASK_ASSIGNED",
  message: "Task assigned: Prepare report",
  createdAt: "2026-09-17T09:00:00Z",
  readAt: null,
};

const readNotification = {
  id: 2,
  type: "TASK_ACCEPTED",
  message: "Task accepted: Prepare report",
  createdAt: "2026-09-17T10:00:00Z",
  readAt: "2026-09-17T10:05:00Z",
};

describe("TaskNotifications", () => {
  beforeEach(() => {
    mocks.getNotifications.mockReset();
    mocks.markAsRead.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("shows a loading state while notifications are loading", () => {
    mocks.getNotifications.mockReturnValue(new Promise(() => {}));

    render(<TaskNotifications />);

    fireEvent.click(screen.getByRole("button", { name: "Task notifications" }));
    expect(screen.getByText("Loading notifications...")).toBeInTheDocument();
  });

  it("shows the empty state when there are no notifications", async () => {
    mocks.getNotifications.mockResolvedValue([]);

    render(<TaskNotifications />);

    fireEvent.click(screen.getByRole("button", { name: "Task notifications" }));
    expect(await screen.findByText("You have no notifications.")).toBeInTheDocument();
  });

  it("loads and displays unread and read notifications", async () => {
    mocks.getNotifications.mockResolvedValue([unreadNotification, readNotification]);

    render(<TaskNotifications />);

    fireEvent.click(screen.getByRole("button", { name: "Task notifications" }));
    expect(await screen.findByText(unreadNotification.message)).toBeInTheDocument();
    expect(screen.getByText(readNotification.message)).toBeInTheDocument();
    expect(screen.getAllByText("Unread")).toHaveLength(1);
    expect(screen.getAllByText("Read")).toHaveLength(1);
    expect(screen.getByRole("button", { name: "Mark as read" })).toBeInTheDocument();
  });

  it("displays all supported task notification types with their English messages", async () => {
    const notifications = [
      { ...unreadNotification, id: 1, type: "TASK_ASSIGNED", message: "A task has been assigned to you." },
      { ...unreadNotification, id: 2, type: "TASK_ACCEPTED", message: "An assigned task has been accepted." },
      { ...unreadNotification, id: 3, type: "TASK_DECLINED", message: "An assigned task has been declined." },
      { ...unreadNotification, id: 4, type: "TASK_UPDATED", message: "A task assigned to you has been updated." },
      { ...unreadNotification, id: 5, type: "TASK_COMPLETED", message: "A task you assigned has been completed." },
      { ...unreadNotification, id: 6, type: "TASK_CHANGES_REQUESTED", message: "Changes have been requested for your task." },
    ];
    mocks.getNotifications.mockResolvedValue(notifications);

    render(<TaskNotifications />);

    fireEvent.click(screen.getByRole("button", { name: "Task notifications" }));
    for (const notification of notifications) {
      expect(await screen.findByText(notification.message)).toBeInTheDocument();
    }
    expect(screen.getByText("Assigned")).toBeInTheDocument();
    expect(screen.getByText("Accepted")).toBeInTheDocument();
    expect(screen.getByText("Declined")).toBeInTheDocument();
    expect(screen.getByText("Updated")).toBeInTheDocument();
    expect(screen.getByText("Completed")).toBeInTheDocument();
    expect(screen.getByText("Changes Requested")).toBeInTheDocument();
  });

  it("keeps duplicate notification IDs out of the UI and unread count", async () => {
    mocks.getNotifications.mockResolvedValue([
      unreadNotification,
      { ...unreadNotification, message: "Task assigned: Prepare updated report" },
    ]);
    const onUnreadCountChange = vi.fn();

    render(<TaskNotifications onUnreadCountChange={onUnreadCountChange} />);

    fireEvent.click(screen.getByRole("button", { name: "Task notifications" }));
    expect(await screen.findByText("Task assigned: Prepare updated report")).toBeInTheDocument();
    expect(screen.queryByText(unreadNotification.message)).not.toBeInTheDocument();
    expect(screen.getAllByText("Unread")).toHaveLength(1);
    expect(onUnreadCountChange).toHaveBeenLastCalledWith(1);
  });

  it("marks an unread notification as read", async () => {
    mocks.getNotifications.mockResolvedValue([unreadNotification]);
    mocks.markAsRead.mockResolvedValue({
      ...unreadNotification,
      readAt: "2026-09-17T11:00:00Z",
    });

    render(<TaskNotifications />);

    fireEvent.click(screen.getByRole("button", { name: "Task notifications" }));
    fireEvent.click(await screen.findByRole("button", { name: "Mark as read" }));

    await waitFor(() => expect(mocks.markAsRead).toHaveBeenCalledWith(1));
    expect(await screen.findByText("Read")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Mark as read" })).not.toBeInTheDocument();
  });

  it("shows an error and retries loading notifications", async () => {
    mocks.getNotifications
      .mockRejectedValueOnce(new ApiError(500, { message: "Notifications are unavailable" }))
      .mockResolvedValueOnce([unreadNotification]);

    render(<TaskNotifications />);

    fireEvent.click(screen.getByRole("button", { name: "Task notifications" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The server encountered a problem. Please try again later."
    );
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    await waitFor(() => expect(mocks.getNotifications).toHaveBeenCalledTimes(2));
    expect(await screen.findByText(unreadNotification.message)).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("polls task notifications every 10 seconds without full loading", async () => {
    const newNotification = {
      ...unreadNotification,
      id: 3,
      message: "Task assigned: Review the new report",
    };
    vi.useFakeTimers();
    mocks.getNotifications
      .mockResolvedValueOnce([unreadNotification])
      .mockResolvedValueOnce([unreadNotification, newNotification]);

    render(<TaskNotifications embedded open />);

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText(unreadNotification.message)).toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(10000);
      await Promise.resolve();
    });

    expect(mocks.getNotifications).toHaveBeenCalledTimes(2);
    expect(screen.getByText(newNotification.message)).toBeInTheDocument();
    expect(screen.queryByText("Loading notifications...")).not.toBeInTheDocument();
  });

  it("keeps loaded notifications visible when a background refresh fails", async () => {
    vi.useFakeTimers();
    mocks.getNotifications
      .mockResolvedValueOnce([unreadNotification])
      .mockRejectedValueOnce(new ApiError(500, { message: "Notifications are unavailable" }));

    render(<TaskNotifications embedded open />);

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText(unreadNotification.message)).toBeInTheDocument();

    await act(async () => {
      vi.advanceTimersByTime(10000);
      await Promise.resolve();
    });

    expect(screen.getByText(unreadNotification.message)).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "The server encountered a problem. Please try again later."
    );
  });

  it("cleans up notification polling when unmounted", async () => {
    vi.useFakeTimers();
    mocks.getNotifications.mockResolvedValue([unreadNotification]);
    const clearIntervalSpy = vi.spyOn(window, "clearInterval");

    const { unmount } = render(<TaskNotifications embedded open />);

    await act(async () => {
      await Promise.resolve();
    });
    unmount();

    await act(async () => {
      vi.advanceTimersByTime(20000);
    });

    expect(clearIntervalSpy).toHaveBeenCalled();
    expect(mocks.getNotifications).toHaveBeenCalledTimes(1);
  });

  it("prevents overlapping notification polling requests", async () => {
    vi.useFakeTimers();
    let resolveRequest;
    mocks.getNotifications.mockImplementation(() => new Promise((resolve) => {
      resolveRequest = resolve;
    }));

    render(<TaskNotifications embedded open />);

    await act(async () => {
      await Promise.resolve();
      vi.advanceTimersByTime(30000);
    });

    expect(mocks.getNotifications).toHaveBeenCalledTimes(1);

    await act(async () => {
      resolveRequest([unreadNotification]);
      await Promise.resolve();
    });
  });
});
