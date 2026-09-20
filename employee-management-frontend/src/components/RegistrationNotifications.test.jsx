import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import Navbar from "./Navbar.jsx";

const request = {
  id: 1, firstName: "Alex", lastName: "Smith", email: "alex@example.com",
  username: "alex", status: "PENDING", createdAt: "2026-01-10T12:00:00",
};

const mocks = vi.hoisted(() => ({
  role: "ADMIN",
  getPending: vi.fn(),
  approve: vi.fn(),
  reject: vi.fn(),
  getDepartments: vi.fn(),
  createDepartment: vi.fn(),
}));

vi.mock("../services/authService", () => ({
  default: {
    isAuthenticated: () => true,
    getRole: () => mocks.role,
    getUsername: () => "admin",
    logout: vi.fn(),
  },
}));
vi.mock("../services/registrationRequestService.js", () => ({
  default: { getPending: mocks.getPending, approve: mocks.approve, reject: mocks.reject },
}));
vi.mock("../services/departmentService.js", () => ({
  default: { getAllDepartments: mocks.getDepartments, createDepartment: mocks.createDepartment },
}));
vi.mock("./TaskNotifications.jsx", () => ({
  default: () => null,
}));

describe("RegistrationNotifications", () => {
  beforeEach(() => {
    mocks.role = "ADMIN";
    mocks.getPending.mockReset().mockResolvedValue([request]);
    mocks.approve.mockReset().mockResolvedValue({ status: "APPROVED" });
    mocks.reject.mockReset().mockResolvedValue({ status: "REJECTED" });
    mocks.getDepartments.mockReset().mockResolvedValue([{ id: 3, name: "IT" }]);
    mocks.createDepartment.mockReset().mockResolvedValue({ id: 7, name: "Operations", description: "Business operations" });
  });

  async function openReview() {
    fireEvent.click(screen.getByRole("button", { name: "Notifications" }));
    fireEvent.click(await screen.findByRole("button", { name: "Review" }));
    return screen.findByRole("dialog", { name: "Registration details" });
  }

  it("shows ADMIN request details and loads departments", async () => {
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    await waitFor(() => expect(mocks.getPending).toHaveBeenCalledTimes(1));

    await openReview();
    expect(screen.getByText("alex", { selector: "dd" })).toBeInTheDocument();
    fireEvent.focus(screen.getByRole("combobox", { name: "Department" }));
    expect(await screen.findByRole("option", { name: "IT" })).toBeInTheDocument();
    expect(mocks.getDepartments).toHaveBeenCalledTimes(1);
  });

  it("requires a department, accepts, closes, and refreshes pending requests", async () => {
    mocks.getPending.mockResolvedValueOnce([request]).mockResolvedValueOnce([]);
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    await waitFor(() => expect(mocks.getPending).toHaveBeenCalledTimes(1));
    await openReview();

    const accept = screen.getByRole("button", { name: "Accept" });
    expect(accept).toBeDisabled();
    fireEvent.focus(screen.getByRole("combobox", { name: "Department" }));
    fireEvent.click(await screen.findByRole("option", { name: "IT" }));
    expect(accept).toBeEnabled();
    fireEvent.click(accept);

    await waitFor(() => expect(mocks.approve).toHaveBeenCalledWith(1, 3));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(await screen.findByRole("status")).toHaveTextContent("accepted successfully");
    expect(mocks.getPending).toHaveBeenCalledTimes(2);
  });

  it("rejects, closes, and refreshes pending requests", async () => {
    mocks.getPending.mockResolvedValueOnce([request]).mockResolvedValueOnce([]);
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    await waitFor(() => expect(mocks.getPending).toHaveBeenCalledTimes(1));
    await openReview();
    fireEvent.click(screen.getByRole("button", { name: "Reject" }));

    await waitFor(() => expect(mocks.reject).toHaveBeenCalledWith(1));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(await screen.findByRole("status")).toHaveTextContent("rejected successfully");
    expect(mocks.getPending).toHaveBeenCalledTimes(2);
  });

  it("creates a department, refreshes the list, and selects it for approval", async () => {
    mocks.getDepartments
      .mockResolvedValueOnce([{ id: 3, name: "IT" }])
      .mockResolvedValueOnce([
        { id: 3, name: "IT" },
        { id: 7, name: "Operations", description: "Business operations" },
      ]);
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    await waitFor(() => expect(mocks.getPending).toHaveBeenCalledTimes(1));
    await openReview();

    const departmentInput = screen.getByRole("combobox", { name: "Department" });
    fireEvent.change(departmentInput, { target: { value: "Operations" } });
    fireEvent.click(screen.getByRole("button", { name: 'Create "Operations"' }));
    expect(screen.getByRole("dialog", { name: "Create new department" })).toBeInTheDocument();
    expect(screen.getByLabelText("Department Name")).toHaveValue("Operations");
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "Business operations" } });
    fireEvent.click(screen.getByRole("button", { name: "Create Department" }));

    await waitFor(() => expect(mocks.createDepartment).toHaveBeenCalledWith({
      name: "Operations",
      description: "Business operations",
    }));
    await waitFor(() => expect(mocks.getDepartments).toHaveBeenCalledTimes(2));
    expect(departmentInput).toHaveValue("Operations");
    fireEvent.focus(departmentInput);
    expect(await screen.findByRole("option", { name: "Operations" })).toBeInTheDocument();
    expect(screen.queryByRole("dialog", { name: "Create new department" })).not.toBeInTheDocument();
  });
  it("does not offer creation for an existing department regardless of case or whitespace", async () => {
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    await waitFor(() => expect(mocks.getPending).toHaveBeenCalledTimes(1));
    await openReview();

    const departmentInput = screen.getByRole("combobox", { name: "Department" });
    fireEvent.change(departmentInput, { target: { value: " it " } });

    expect(await screen.findByRole("option", { name: "IT" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Create/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("option", { name: "IT" }));
    expect(departmentInput).toHaveValue("IT");
    expect(mocks.createDepartment).not.toHaveBeenCalled();
  });
  it("shows a backend error when department creation fails", async () => {
    mocks.createDepartment.mockRejectedValue(
      new ApiError(409, { message: "Department name already exists" })
    );
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    await waitFor(() => expect(mocks.getPending).toHaveBeenCalledTimes(1));
    await openReview();

    const departmentInput = screen.getByRole("combobox", { name: "Department" });
    fireEvent.change(departmentInput, { target: { value: "Operations" } });
    fireEvent.click(screen.getByRole("button", { name: 'Create "Operations"' }));
    fireEvent.click(screen.getByRole("button", { name: "Create Department" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Department name already exists");
    expect(screen.getByRole("dialog", { name: "Create new department" })).toBeInTheDocument();
  });
  it("does not render the bell or request data for EMPLOYEE", async () => {
    mocks.role = "EMPLOYEE";
    render(<MemoryRouter><Navbar /></MemoryRouter>);
    expect(screen.queryByRole("tab", { name: "Requests" })).not.toBeInTheDocument();
    await Promise.resolve();
    expect(mocks.getPending).not.toHaveBeenCalled();
    expect(mocks.getDepartments).not.toHaveBeenCalled();
  });
});
