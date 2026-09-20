import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EmployeeDetails from "./EmployeeDetails.jsx";

const { getEmployeeByIdMock } = vi.hoisted(() => ({
  getEmployeeByIdMock: vi.fn(),
}));

vi.mock("../services/authService", () => ({
  default: {
    getRole: () => "EMPLOYEE",
  },
}));

vi.mock("../services/employeeService", () => ({
  default: {
    getEmployeeById: getEmployeeByIdMock,
  },
}));

describe("EmployeeDetails role-based actions", () => {
  beforeEach(() => {
    getEmployeeByIdMock.mockResolvedValue({
      id: 1,
      firstName: "John",
      lastName: "Doe",
      email: "john.doe@example.com",
      department: "IT",
    });
  });

  it("does not show the ADMIN-only Edit action to EMPLOYEE", async () => {
    render(
      <MemoryRouter initialEntries={["/employees/1"]}>
        <Routes>
          <Route path="/employees/:id" element={<EmployeeDetails />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText("John Doe")).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Edit Employee" })
    ).not.toBeInTheDocument();
  });
});
