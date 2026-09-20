import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ShiftControls from "./ShiftControls.jsx";

const { getCurrentShiftMock, startShiftMock, endShiftMock } = vi.hoisted(() => ({
  getCurrentShiftMock: vi.fn(),
  startShiftMock: vi.fn(),
  endShiftMock: vi.fn(),
}));

vi.mock("../services/workShiftService.js", () => ({
  default: {
    getCurrentShift: getCurrentShiftMock,
    startShift: startShiftMock,
    endShift: endShiftMock,
  },
}));

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

describe("ShiftControls", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("disables both actions while checking the initial status", () => {
    getCurrentShiftMock.mockReturnValue(deferred().promise);

    render(<ShiftControls />);

    expect(screen.getByText("Checking shift status...")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start Shift" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "End Shift" })).toBeDisabled();
  });

  it("keeps End Shift disabled when there is no active shift", async () => {
    getCurrentShiftMock.mockResolvedValue({ active: false, startTime: null });

    render(<ShiftControls />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Start Shift" })).toBeEnabled();
    });
    expect(screen.getByRole("button", { name: "End Shift" })).toBeDisabled();
  });

  it("disables actions after a status error and retries on demand", async () => {
    getCurrentShiftMock
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce({ active: false, startTime: null });

    render(<ShiftControls />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to connect to the server"
    );
    expect(screen.getByRole("button", { name: "Start Shift" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "End Shift" })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Start Shift" })).toBeEnabled();
    });
    expect(getCurrentShiftMock).toHaveBeenCalledTimes(2);
  });

  it("shows active status and start time returned by the backend", async () => {
    getCurrentShiftMock.mockResolvedValue({
      active: true,
      startTime: "2026-09-15T06:30:00Z",
    });

    render(<ShiftControls />);

    expect(await screen.findByText("Shift active")).toBeInTheDocument();
    expect(screen.getByText(/Started at/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start Shift" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "End Shift" })).toBeEnabled();
  });

  it("refreshes current status after a successful Start Shift", async () => {
    getCurrentShiftMock
      .mockResolvedValueOnce({ active: false, startTime: null })
      .mockResolvedValueOnce({ active: true, startTime: "2026-09-15T06:30:00Z" });
    startShiftMock.mockResolvedValue({});

    render(<ShiftControls />);
    const startButton = await screen.findByRole("button", { name: "Start Shift" });
    await waitFor(() => expect(startButton).toBeEnabled());
    fireEvent.click(startButton);

    expect(await screen.findByText("Shift started successfully.")).toBeInTheDocument();
    expect(await screen.findByText("Shift active")).toBeInTheDocument();
    expect(startShiftMock).toHaveBeenCalledTimes(1);
    expect(getCurrentShiftMock).toHaveBeenCalledTimes(2);
  });

  it("returns to inactive state after a successful End Shift", async () => {
    getCurrentShiftMock
      .mockResolvedValueOnce({ active: true, startTime: "2026-09-15T06:30:00Z" })
      .mockResolvedValueOnce({ active: false, startTime: null });
    endShiftMock.mockResolvedValue({});

    render(<ShiftControls />);
    const endButton = await screen.findByRole("button", { name: "End Shift" });
    await waitFor(() => expect(endButton).toBeEnabled());
    fireEvent.click(endButton);

    expect(await screen.findByText("Shift ended successfully.")).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Start Shift" })).toBeEnabled();
      expect(screen.getByRole("button", { name: "End Shift" })).toBeDisabled();
    });
    expect(getCurrentShiftMock).toHaveBeenCalledTimes(2);
  });

  it("locks an action immediately to prevent double clicks", async () => {
    const startRequest = deferred();
    getCurrentShiftMock.mockResolvedValue({ active: false, startTime: null });
    startShiftMock.mockReturnValue(startRequest.promise);

    render(<ShiftControls />);
    const startButton = await screen.findByRole("button", { name: "Start Shift" });
    await waitFor(() => expect(startButton).toBeEnabled());

    fireEvent.click(startButton);
    fireEvent.click(startButton);

    expect(startShiftMock).toHaveBeenCalledTimes(1);
    startRequest.resolve({});
  });

  it("refreshes status when the window regains focus", async () => {
    getCurrentShiftMock
      .mockResolvedValueOnce({ active: false, startTime: null })
      .mockResolvedValueOnce({ active: true, startTime: "2026-09-15T06:30:00Z" });

    render(<ShiftControls />);
    await waitFor(() => expect(getCurrentShiftMock).toHaveBeenCalledTimes(1));
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Start Shift" })).toBeEnabled();
    });

    fireEvent.focus(window);

    expect(await screen.findByText("Shift active")).toBeInTheDocument();
    expect(getCurrentShiftMock).toHaveBeenCalledTimes(2);
  });
});
