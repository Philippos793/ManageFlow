import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Navbar from "./Navbar.jsx";

const mocks = vi.hoisted(() => ({
  role: "EMPLOYEE",
  getNotifications: vi.fn(),
  markAsRead: vi.fn(),
}));

vi.mock("../services/authService", () => ({
  default: {
    isAuthenticated: () => true,
    getRole: () => mocks.role,
    getUsername: () => "jane.doe",
    logout: vi.fn(),
  },
}));
vi.mock("../services/notificationService.js", () => ({
  default: {
    getNotifications: mocks.getNotifications,
    markAsRead: mocks.markAsRead,
  },
}));
vi.mock("./RegistrationNotifications.jsx", () => ({
  default: () => <span>Registration notification bell</span>,
}));

describe("Navbar task notifications", () => {
  beforeEach(() => {
    mocks.role = "EMPLOYEE";
    mocks.getNotifications.mockReset().mockResolvedValue([
      {
        id: 1,
        type: "TASK_ASSIGNED",
        message: "Task assigned: Prepare report",
        createdAt: "2026-09-17T09:00:00Z",
        readAt: null,
      },
    ]);
    mocks.markAsRead.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("shows task notifications for an EMPLOYEE without registration notifications", async () => {
    render(<MemoryRouter><Navbar /></MemoryRouter>);

    expect(screen.getByRole("button", { name: "Notifications" })).toBeInTheDocument();
    expect(screen.queryByText("Registration notification bell")).not.toBeInTheDocument();
    await waitFor(() => expect(mocks.getNotifications).toHaveBeenCalledTimes(1));
  });

  it("keeps registration notifications and shows task notifications for an ADMIN", async () => {
    mocks.role = "ADMIN";

    render(<MemoryRouter><Navbar /></MemoryRouter>);

    expect(screen.getByRole("button", { name: "Notifications" })).toBeInTheDocument();
    expect(screen.getByText("Registration notification bell")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Notifications" }));
    fireEvent.click(screen.getByRole("tab", { name: "Tasks" }));
    expect(await screen.findByText("Task assigned: Prepare report")).toBeInTheDocument();
  });

  it("updates the employee unread badge when polling receives a new task assignment", async () => {
    vi.useFakeTimers();
    mocks.getNotifications
      .mockResolvedValueOnce([
        {
          id: 1,
          type: "TASK_ASSIGNED",
          message: "Task assigned: Prepare report",
          createdAt: "2026-09-17T09:00:00Z",
          readAt: null,
        },
      ])
      .mockResolvedValueOnce([
        {
          id: 1,
          type: "TASK_ASSIGNED",
          message: "Task assigned: Prepare report",
          createdAt: "2026-09-17T09:00:00Z",
          readAt: null,
        },
        {
          id: 2,
          type: "TASK_ASSIGNED",
          message: "Task assigned: Review report",
          createdAt: "2026-09-17T10:00:00Z",
          readAt: null,
        },
      ]);

    render(<MemoryRouter><Navbar /></MemoryRouter>);

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByRole("button", { name: "Notifications" })).toHaveTextContent("1");

    await act(async () => {
      vi.advanceTimersByTime(10000);
      await Promise.resolve();
    });

    expect(screen.getByRole("button", { name: "Notifications" })).toHaveTextContent("2");
  });

  it("updates the unread badge immediately after marking a task notification as read", async () => {
    mocks.markAsRead.mockResolvedValue({
      id: 1,
      type: "TASK_ASSIGNED",
      message: "Task assigned: Prepare report",
      createdAt: "2026-09-17T09:00:00Z",
      readAt: "2026-09-17T10:00:00Z",
    });

    render(<MemoryRouter><Navbar /></MemoryRouter>);

    await waitFor(() => expect(screen.getByRole("button", { name: "Notifications" }))
      .toHaveTextContent("1"));
    fireEvent.click(screen.getByRole("button", { name: "Notifications" }));
    fireEvent.click(await screen.findByRole("button", { name: "Mark as read" }));

    await waitFor(() => expect(mocks.markAsRead).toHaveBeenCalledWith(1));
    await waitFor(() => expect(screen.getByRole("button", { name: "Notifications" }))
      .not.toHaveTextContent("1"));
  });
});
