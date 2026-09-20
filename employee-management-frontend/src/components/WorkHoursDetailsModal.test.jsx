import { useState } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import WorkHoursDetailsModal from "./WorkHoursDetailsModal.jsx";

const mocks = vi.hoisted(() => ({
  getEmployeeMonthlyDetails: vi.fn(),
  updateHourlyRate: vi.fn(),
}));

vi.mock("../services/workShiftService.js", () => ({
  default: {
    getEmployeeMonthlyDetails: mocks.getEmployeeMonthlyDetails,
  },
}));


vi.mock("../services/employeeService.js", () => ({
  default: {
    updateHourlyRate: mocks.updateHourlyRate,
  },
}));
function monthlyDetails() {
  return {
    employeeId: 7,
    fullName: "Maria Georgiou",
    year: 2026,
    month: 5,
    dailyWorkedTimes: Array.from({ length: 31 }, (_, index) => ({
      day: index + 1,
      workedMinutes: index === 0 ? 483 : 0,
      workedTime: index === 0 ? "8h 03m" : "0h 0m",
    })),
    totalWorkedMinutes: 483,
    totalWorkedHours: "8h 03m",
    hourlyRate: 15,
  };
}

function renderModal(onClose = vi.fn()) {
  render(
    <WorkHoursDetailsModal
      employee={{ employeeId: 7, fullName: "Maria Georgiou" }}
      year={2026}
      month={5}
      onClose={onClose}
    />
  );
  return onClose;
}

describe("WorkHoursDetailsModal", () => {
  beforeEach(() => {
    mocks.getEmployeeMonthlyDetails.mockReset();
    mocks.getEmployeeMonthlyDetails.mockResolvedValue(monthlyDetails());
    mocks.updateHourlyRate.mockReset();
    mocks.updateHourlyRate.mockResolvedValue({ employeeId: 7, hourlyRate: 15 });
  });

  it("renders employee, month, calendar, worked time, and zero-work days", async () => {
    renderModal();

    expect(await screen.findByText("Maria Georgiou")).toBeInTheDocument();
    expect(screen.getByText("May 2026")).toBeInTheDocument();
    expect(screen.getByText("Mon")).toBeInTheDocument();
    expect(screen.getByText("Sun")).toBeInTheDocument();
    expect(screen.getByText("8h 03m")).toBeInTheDocument();
    expect(screen.getAllByText("0h 0m")).toHaveLength(30);
  });

  it("closes from the X button", async () => {
    const onClose = renderModal();
    await screen.findByText("Maria Georgiou");

    fireEvent.click(screen.getAllByRole("button", { name: "Close" })[0]);

    expect(onClose).toHaveBeenCalledOnce();
  });

  it("closes from the Close button", async () => {
    const onClose = renderModal();
    await screen.findByText("Maria Georgiou");

    fireEvent.click(screen.getAllByRole("button", { name: "Close" })[1]);

    expect(onClose).toHaveBeenCalledOnce();
  });

  it("shows payroll details with total hours and the existing hourly rate", async () => {
    renderModal();
    await screen.findByText("Maria Georgiou");

    fireEvent.click(screen.getByRole("button", { name: "Prepare Payroll" }));

    expect(screen.getByText("Payroll Estimate")).toBeInTheDocument();
    expect(screen.getAllByText("8h 03m")).toHaveLength(2);
    expect(screen.getByLabelText("Hourly Rate")).toHaveValue(15);
  });

  it("calculates estimated pay from total minutes and hourly rate", async () => {
    renderModal();
    await screen.findByText("Maria Georgiou");
    fireEvent.click(screen.getByRole("button", { name: "Prepare Payroll" }));

    fireEvent.click(screen.getByRole("button", { name: "Calculate" }));

    expect(screen.getByText("120.75")).toBeInTheDocument();
  });

  it("shows validation for an invalid hourly rate", async () => {
    renderModal();
    await screen.findByText("Maria Georgiou");
    fireEvent.click(screen.getByRole("button", { name: "Prepare Payroll" }));
    fireEvent.change(screen.getByLabelText("Hourly Rate"), { target: { value: "-1" } });

    fireEvent.click(screen.getByRole("button", { name: "Calculate" }));

    expect(screen.getByText("Enter a valid non-negative hourly rate.")).toBeInTheDocument();
  });

  it("saves the hourly rate and shows a success message", async () => {
    mocks.updateHourlyRate.mockResolvedValue({ employeeId: 7, hourlyRate: 18.5 });
    renderModal();
    await screen.findByText("Maria Georgiou");
    fireEvent.click(screen.getByRole("button", { name: "Prepare Payroll" }));
    fireEvent.change(screen.getByLabelText("Hourly Rate"), { target: { value: "18.5" } });

    fireEvent.click(screen.getByRole("button", { name: "Save Rate" }));

    await waitFor(() => expect(mocks.updateHourlyRate).toHaveBeenCalledWith(7, 18.5));
    expect(await screen.findByText("Hourly rate saved successfully.")).toBeInTheDocument();
    expect(screen.getByLabelText("Hourly Rate")).toHaveValue(18.5);
  });

  it("retries after monthly details fail to load", async () => {
    mocks.getEmployeeMonthlyDetails
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(monthlyDetails());
    renderModal();

    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to connect to the server");
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("8h 03m")).toBeInTheDocument();
    expect(mocks.getEmployeeMonthlyDetails).toHaveBeenCalledTimes(2);
  });

  it("closes with Escape and restores focus to the opener", async () => {
    function Harness() {
      const [open, setOpen] = useState(false);
      return (
        <>
          <button type="button" onClick={() => setOpen(true)}>Open details</button>
          {open && (
            <WorkHoursDetailsModal
              employee={{ employeeId: 7, fullName: "Maria Georgiou" }}
              year={2026}
              month={5}
              onClose={() => setOpen(false)}
            />
          )}
        </>
      );
    }

    render(<Harness />);
    const opener = screen.getByRole("button", { name: "Open details" });
    opener.focus();
    fireEvent.click(opener);
    const dialog = screen.getByRole("dialog");
    await waitFor(() => expect(dialog).toHaveFocus());

    fireEvent.keyDown(document, { key: "Escape" });

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(opener).toHaveFocus();
  });
});
