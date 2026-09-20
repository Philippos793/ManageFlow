import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import AddEmployee from "./AddEmployee.jsx";

const { createEmployeeMock, reactivateEmployeeMock, getAllDepartmentsMock } = vi.hoisted(() => ({
  createEmployeeMock: vi.fn(),
  reactivateEmployeeMock: vi.fn(),
  getAllDepartmentsMock: vi.fn(),
}));

vi.mock("../services/employeeService", () => ({
  default: {
    createEmployee: createEmployeeMock,
    reactivateEmployee: reactivateEmployeeMock,
  },
}));

vi.mock("../services/departmentService", () => ({
  default: {
    getAllDepartments: getAllDepartmentsMock,
  },
}));

describe("AddEmployee validation", () => {
  beforeEach(() => {
    createEmployeeMock.mockReset();
    reactivateEmployeeMock.mockReset();
    getAllDepartmentsMock.mockReset();
    getAllDepartmentsMock.mockResolvedValue([
      { id: 1, name: "IT", description: "Technology" },
    ]);
  });

  async function fillEmployeeForm({
    firstName = "Maria",
    lastName = "Doe",
    email = "maria@example.com",
    department = "IT",
  } = {}) {
    fireEvent.change(screen.getByLabelText("First Name"), { target: { value: firstName } });
    fireEvent.change(screen.getByLabelText("Last Name"), { target: { value: lastName } });
    fireEvent.change(screen.getByLabelText("Email Address"), { target: { value: email } });
    await screen.findByRole("option", { name: department });
    fireEvent.change(screen.getByLabelText("Department"), { target: { value: department } });
  }

  it("loads only existing departments into the selection", async () => {
    render(<MemoryRouter><AddEmployee /></MemoryRouter>);

    expect(await screen.findByRole("option", { name: "IT" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Department" })).toHaveValue("");
  });

  it("renders backend validationErrors on the employee form", async () => {
    createEmployeeMock.mockRejectedValue(
      new ApiError(400, {
        status: 400,
        error: "Bad Request",
        message: "Validation failed",
        validationErrors: {
          firstName: "First name is required",
        },
      })
    );

    render(
      <MemoryRouter>
        <AddEmployee />
      </MemoryRouter>
    );

    await fillEmployeeForm({ firstName: "Invalid", email: "invalid@example.com" });
    fireEvent.click(
      screen.getByRole("button", { name: "Save Employee" })
    );

    expect(
      await screen.findByText("First name is required")
    ).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Maria" },
    });
    expect(screen.queryByText("First name is required")).not.toBeInTheDocument();
  });

  it("shows the backend message for a duplicate email conflict", async () => {
    createEmployeeMock.mockRejectedValue(
      new ApiError(409, { message: "An employee with this email already exists" })
    );

    render(
      <MemoryRouter>
        <AddEmployee />
      </MemoryRouter>
    );

    await fillEmployeeForm();
    fireEvent.click(screen.getByRole("button", { name: "Save Employee" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "An employee with this email already exists"
    );
  });

  it("does not offer reactivation when the inactive employee name does not match", async () => {
    const message =
      "An inactive employee with this email already exists, but the first or last name does not match. Reactivation from Add Employee is not allowed.";
    createEmployeeMock.mockRejectedValue(new ApiError(409, { message }));
    render(<MemoryRouter><AddEmployee /></MemoryRouter>);

    await fillEmployeeForm({ firstName: "Different", lastName: "Person" });
    fireEvent.click(screen.getByRole("button", { name: "Save Employee" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(reactivateEmployeeMock).not.toHaveBeenCalled();
  });

  it("offers reactivation for an inactive employee and supports cancellation", async () => {
    createEmployeeMock.mockRejectedValue(new ApiError(409, {
      message: "An inactive employee with this email already exists",
      code: "INACTIVE_EMPLOYEE",
      employeeId: 42,
    }));
    render(<MemoryRouter><AddEmployee /></MemoryRouter>);

    await fillEmployeeForm();
    fireEvent.click(screen.getByRole("button", { name: "Save Employee" }));

    expect(await screen.findByText(
      "An inactive employee with this email already exists. Would you like to reactivate the existing employee?"
    )).toBeInTheDocument();
    const dialog = screen.getByRole("dialog", { name: "Reactivate Employee?" });
    fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(reactivateEmployeeMock).not.toHaveBeenCalled();
  });

  it("reactivates the existing employee after confirmation", async () => {
    createEmployeeMock.mockRejectedValue(new ApiError(409, {
      message: "An inactive employee with this email already exists",
      code: "INACTIVE_EMPLOYEE",
      employeeId: 42,
    }));
    reactivateEmployeeMock.mockResolvedValue({ id: 42 });
    render(<MemoryRouter><AddEmployee /></MemoryRouter>);

    await fillEmployeeForm();
    fireEvent.click(screen.getByRole("button", { name: "Save Employee" }));
    fireEvent.click(await screen.findByRole("button", { name: "Reactivate Employee" }));

    expect(reactivateEmployeeMock).toHaveBeenCalledWith(42, {
      firstName: "Maria",
      lastName: "Doe",
    });
  });
});
