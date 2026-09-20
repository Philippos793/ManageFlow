import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import TaskCreateModal from "./TaskCreateModal.jsx";

const mocks = vi.hoisted(() => ({
  getAllEmployees: vi.fn(),
  createTask: vi.fn(),
}));

vi.mock("../services/employeeService.js", () => ({
  default: { getAllEmployees: mocks.getAllEmployees },
}));

vi.mock("../services/taskService.js", () => ({
  default: { createTask: mocks.createTask },
}));

const employee = {
  id: 7,
  firstName: "Nikos",
  lastName: "Papadopoulos",
  department: "IT",
};

describe("TaskCreateModal", () => {
  beforeEach(() => {
    mocks.getAllEmployees.mockReset();
    mocks.createTask.mockReset();
  });

  it("loads employees and creates a task with optional checklist items", async () => {
    const onClose = vi.fn();
    const onCreated = vi.fn();
    mocks.getAllEmployees.mockResolvedValue([employee]);
    mocks.createTask.mockResolvedValue({ id: 11 });

    render(<TaskCreateModal open onClose={onClose} onCreated={onCreated} />);

    await screen.findByRole("option", { name: "Nikos Papadopoulos — IT" });
    fireEvent.change(screen.getByLabelText("Employee"), { target: { value: "7" } });
    fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Prepare report" } });
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "Monthly summary" } });
    fireEvent.change(screen.getByLabelText("Hours"), { target: { value: "2" } });
    fireEvent.click(screen.getByRole("button", { name: "+ Add Checklist Item" }));
    fireEvent.change(screen.getByLabelText("Checklist item 1"), { target: { value: "Review data" } });

    fireEvent.click(screen.getByRole("button", { name: "Assign Task" }));

    await waitFor(() => expect(mocks.createTask).toHaveBeenCalledWith({
      employeeId: 7,
      title: "Prepare report",
      description: "Monthly summary",
      timeAllowedMinutes: 120,
      checklistItems: [{ description: "Review data" }],
    }));
    expect(onCreated).toHaveBeenCalledWith({ id: 11 });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("validates required fields and prevents blank checklist items", async () => {
    mocks.getAllEmployees.mockResolvedValue([employee]);

    render(<TaskCreateModal open onClose={vi.fn()} onCreated={vi.fn()} />);

    await screen.findByRole("option", { name: "Nikos Papadopoulos — IT" });
    fireEvent.click(screen.getByRole("button", { name: "+ Add Checklist Item" }));
    fireEvent.click(screen.getByRole("button", { name: "Assign Task" }));

    expect(screen.getByText("Please select an employee.")).toBeInTheDocument();
    expect(screen.getByText("Title is required.")).toBeInTheDocument();
    expect(screen.getByText("Time allowed must be greater than 0.")).toBeInTheDocument();
    expect(screen.getByText("Checklist items cannot be blank.")).toBeInTheDocument();
    expect(screen.getByLabelText("Title")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("Title")).toHaveAttribute("aria-describedby", "task-title-error");
    expect(screen.getByLabelText("Days")).toHaveAttribute("aria-describedby", "task-time-allowed-error");
    expect(screen.getByLabelText("Checklist item 1")).toHaveAttribute("aria-describedby", "task-checklist-items-error");
    expect(mocks.createTask).not.toHaveBeenCalled();
  });

  it("allows checklist items to be removed before submitting", async () => {
    mocks.getAllEmployees.mockResolvedValue([employee]);

    render(<TaskCreateModal open onClose={vi.fn()} onCreated={vi.fn()} />);

    await screen.findByRole("option", { name: "Nikos Papadopoulos — IT" });
    fireEvent.click(screen.getByRole("button", { name: "+ Add Checklist Item" }));
    expect(screen.getByLabelText("Checklist item 1")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Remove checklist item 1" }));
    expect(screen.queryByLabelText("Checklist item 1")).not.toBeInTheDocument();
  });

  it("shows backend validation feedback", async () => {
    mocks.getAllEmployees.mockResolvedValue([employee]);
    mocks.createTask.mockRejectedValue(new ApiError(400, {
      message: "Validation failed",
      validationErrors: { title: "Title must not exceed 200 characters" },
    }));

    render(<TaskCreateModal open onClose={vi.fn()} onCreated={vi.fn()} />);

    await screen.findByRole("option", { name: "Nikos Papadopoulos — IT" });
    fireEvent.change(screen.getByLabelText("Employee"), { target: { value: "7" } });
    fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Prepare report" } });
    fireEvent.change(screen.getByLabelText("Hours"), { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: "Assign Task" }));

    expect(await screen.findByText("Title must not exceed 200 characters")).toBeInTheDocument();
  });

  it("validates Days, Hours, and Minutes ranges", async () => {
    mocks.getAllEmployees.mockResolvedValue([employee]);

    render(<TaskCreateModal open onClose={vi.fn()} onCreated={vi.fn()} />);

    await screen.findByRole("option", { name: "Nikos Papadopoulos — IT" });
    fireEvent.change(screen.getByLabelText("Employee"), { target: { value: "7" } });
    fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Prepare report" } });
    fireEvent.change(screen.getByLabelText("Hours"), { target: { value: "24" } });
    fireEvent.click(screen.getByRole("button", { name: "Assign Task" }));

    expect(screen.getByText("Hours must be between 0 and 23.")).toBeInTheDocument();
    expect(mocks.createTask).not.toHaveBeenCalled();
  });
});
