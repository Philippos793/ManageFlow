import { describe, expect, it } from "vitest";
import {
  formatDateTime,
  formatRemainingTime,
  formatTimeAllowed,
  isOverdueTask,
  matchesTaskFilter,
  taskStatusLabel,
} from "./taskFormatting.js";

const NOW = Date.parse("2026-03-10T12:00:00Z");

describe("taskFormatting", () => {
  it("formats allowed time with and without days", () => {
    expect(formatTimeAllowed(90)).toBe("1h 30m");
    expect(formatTimeAllowed(24 * 60 + 61)).toBe("1d 1h 1m");
    expect(formatTimeAllowed(undefined)).toBe("Not available");
  });

  it("handles missing and invalid date values", () => {
    expect(formatDateTime(null)).toBe("Not set");
    expect(formatDateTime("not-a-date")).toBe("Not available");
  });

  it("formats remaining time and overdue deadlines", () => {
    expect(formatRemainingTime("2026-03-10T12:30:00Z", NOW)).toBe("30m remaining");
    expect(formatRemainingTime("2026-03-10T14:05:00Z", NOW)).toBe("2h 5m remaining");
    expect(formatRemainingTime("2026-03-12T13:00:00Z", NOW)).toBe("2d 1h 0m remaining");
    expect(formatRemainingTime("2026-03-10T11:59:00Z", NOW)).toBe("Overdue");
  });

  it("detects overdue tasks only while in progress", () => {
    const overdue = { status: "IN_PROGRESS", deadline: "2026-03-10T11:00:00Z" };
    expect(isOverdueTask(overdue, NOW)).toBe(true);
    expect(isOverdueTask({ ...overdue, status: "COMPLETED" }, NOW)).toBe(false);
    expect(isOverdueTask({ status: "IN_PROGRESS", deadline: null }, NOW)).toBe(false);
  });

  it("separates in-progress and overdue filters", () => {
    const overdue = { status: "IN_PROGRESS", deadline: "2026-03-10T11:00:00Z" };
    const onTrack = { status: "IN_PROGRESS", deadline: "2026-03-11T11:00:00Z" };

    expect(matchesTaskFilter(overdue, "OVERDUE", NOW)).toBe(true);
    expect(matchesTaskFilter(overdue, "IN_PROGRESS", NOW)).toBe(false);
    expect(matchesTaskFilter(onTrack, "IN_PROGRESS", NOW)).toBe(true);
    expect(matchesTaskFilter({ status: "PENDING" }, "ALL", NOW)).toBe(true);
    expect(matchesTaskFilter({ status: "PENDING" }, "COMPLETED", NOW)).toBe(false);
  });

  it("formats status labels", () => {
    expect(taskStatusLabel("IN_PROGRESS")).toBe("In Progress");
    expect(taskStatusLabel(undefined)).toBe("Unknown");
  });
});
