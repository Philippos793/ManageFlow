import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Departments from "./Departments.jsx";

const mocks = vi.hoisted(() => ({
  role: "EMPLOYEE",
  getAllDepartments: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn(),
}));

vi.mock("../services/authService", () => ({
  default: {
    getRole: () => mocks.role,
  },
}));

vi.mock("../services/departmentService", () => ({
  default: {
    getAllDepartments: mocks.getAllDepartments,
    createDepartment: mocks.createDepartment,
    updateDepartment: mocks.updateDepartment,
    deleteDepartment: mocks.deleteDepartment,
  },
}));

const department = {
  id: 1,
  name: "IT",
  description: "Technology",
};

function renderPage() {
  render(
    <MemoryRouter>
      <Departments />
    </MemoryRouter>
  );
}

describe("Departments role-based UI", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.role = "EMPLOYEE";
    mocks.getAllDepartments.mockResolvedValue([department]);
  });

  it("shows an EMPLOYEE a read-only department list", async () => {
    renderPage();

    expect(await screen.findByText("Technology")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Add Department" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Delete" })).not.toBeInTheDocument();
  });

  it("shows department actions to ADMIN", async () => {
    mocks.role = "ADMIN";
    renderPage();

    expect(await screen.findByText("Technology")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add Department" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Edit" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Delete" })).toBeInTheDocument();
  });

  it("shows the backend message when delete returns 409", async () => {
    mocks.role = "ADMIN";
    mocks.deleteDepartment.mockRejectedValue({
      status: 409,
      data: { message: "Department cannot be deleted because it has employees" },
    });
    renderPage();

    fireEvent.click(await screen.findByRole("button", { name: "Delete" }));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Delete" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Department cannot be deleted because it has employees"
      );
    });
  });

  it("shows a retryable load error without an empty state", async () => {
    mocks.getAllDepartments
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce([department]);
    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to connect to the server");
    expect(screen.queryByText("No departments yet.")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("Technology")).toBeInTheDocument();
    expect(mocks.getAllDepartments).toHaveBeenCalledTimes(2);
  });
});
