import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Employees from "./Employees.jsx";

const mocks = vi.hoisted(() => ({
  getAllEmployees: vi.fn(),
  deleteEmployee: vi.fn(),
}));

vi.mock("../services/authService", () => ({
  default: {
    getRole: () => "ADMIN",
    isAuthenticated: () => true,
    getUsername: () => "admin",
  },
}));

vi.mock("../services/employeeService", () => ({
  default: {
    getAllEmployees: mocks.getAllEmployees,
    deleteEmployee: mocks.deleteEmployee,
  },
}));

const employee = {
  id: 1,
  firstName: "Maria",
  lastName: "Georgiou",
  email: "maria.georgiou@example.com",
  department: "Finance",
};

describe("Employees deactivate flow", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getAllEmployees.mockResolvedValue([employee]);
    mocks.deleteEmployee.mockResolvedValue(undefined);
  });

  it("presents DELETE as employee deactivation and confirms success", async () => {
    render(
      <MemoryRouter>
        <Employees />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole("button", { name: "Deactivate" }));

    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByText("Deactivate Employee?")).toBeInTheDocument();
    expect(within(dialog).getByText(/Work shift history will be preserved/)).toBeInTheDocument();

    fireEvent.click(within(dialog).getByRole("button", { name: "Deactivate" }));

    await waitFor(() => {
      expect(mocks.deleteEmployee).toHaveBeenCalledWith(1);
      expect(screen.getByRole("status")).toHaveTextContent(
        "Employee deactivated successfully"
      );
    });
  });

  it("shows a retryable load error without an empty state", async () => {
    mocks.getAllEmployees
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce([employee]);

    render(
      <MemoryRouter>
        <Employees />
      </MemoryRouter>
    );

    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to connect to the server");
    expect(screen.queryByText("No employees yet")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("Maria Georgiou")).toBeInTheDocument();
    expect(mocks.getAllEmployees).toHaveBeenCalledTimes(2);
  });
});
