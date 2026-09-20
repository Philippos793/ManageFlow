import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AdminDashboardSummary from "./AdminDashboardSummary.jsx";

const { getAdminSummaryMock } = vi.hoisted(() => ({
  getAdminSummaryMock: vi.fn(),
}));

vi.mock("../services/dashboardService.js", () => ({
  default: {
    getAdminSummary: getAdminSummaryMock,
  },
}));

describe("AdminDashboardSummary", () => {
  beforeEach(() => {
    getAdminSummaryMock.mockReset();
    getAdminSummaryMock.mockResolvedValue({
      totalEmployees: 7,
      totalDepartments: 3,
      activeShifts: 1,
      hoursThisMonth: "12h 30m",
    });
  });

  it("labels the employee count as active employees", async () => {
    render(<AdminDashboardSummary />);

    expect(await screen.findByText("Active Employees")).toBeInTheDocument();
    expect(screen.queryByText("Total Employees")).not.toBeInTheDocument();
    expect(screen.getByText("7")).toBeInTheDocument();
  });
});
