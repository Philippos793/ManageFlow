import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import AcceptInvitation from "./AcceptInvitation.jsx";

const { validateMock, acceptMock } = vi.hoisted(() => ({
  validateMock: vi.fn(),
  acceptMock: vi.fn(),
}));

vi.mock("../services/employeeInvitationService.js", () => ({
  default: { validate: validateMock, accept: acceptMock },
}));

describe("AcceptInvitation", () => {
  beforeEach(() => {
    validateMock.mockReset();
    acceptMock.mockReset();
    window.location.hash = "#token=invitation-token";
    validateMock.mockResolvedValue({
      employeeId: 7,
      employeeName: "Jane Doe",
      expiresAt: "2026-09-17T10:00:00Z",
    });
  });

  it("validates the token and displays the account setup form", async () => {
    render(<MemoryRouter><AcceptInvitation /></MemoryRouter>);

    expect(await screen.findByText(/Welcome, Jane Doe/)).toBeInTheDocument();
    expect(validateMock).toHaveBeenCalledWith("invitation-token");
    expect(screen.getByLabelText("Username")).toBeInTheDocument();
  });

  it("toggles both account setup password fields", async () => {
    render(<MemoryRouter><AcceptInvitation /></MemoryRouter>);
    await screen.findByText(/Welcome, Jane Doe/);
    const password = screen.getByLabelText("Password");
    const confirmation = screen.getByLabelText("Confirm Password");

    fireEvent.click(screen.getByRole("button", { name: "Show Password" }));
    fireEvent.click(screen.getByRole("button", { name: "Show Confirm Password" }));
    expect(password).toHaveAttribute("type", "text");
    expect(confirmation).toHaveAttribute("type", "text");
    fireEvent.click(screen.getByRole("button", { name: "Hide Password" }));
    expect(password).toHaveAttribute("type", "password");
  });

  it("does not submit when password confirmation differs", async () => {
    render(<MemoryRouter><AcceptInvitation /></MemoryRouter>);
    await screen.findByText(/Welcome, Jane Doe/);

    fireEvent.change(screen.getByLabelText("Username"), {
      target: { value: "jane.doe" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "secure-password" },
    });
    fireEvent.change(screen.getByLabelText("Confirm Password"), {
      target: { value: "different-password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create Account" }));

    expect(screen.getByText("Passwords do not match.")).toBeInTheDocument();
    expect(acceptMock).not.toHaveBeenCalled();
  });

  it("creates the account and offers navigation to login", async () => {
    acceptMock.mockResolvedValue({ username: "jane.doe" });
    render(<MemoryRouter><AcceptInvitation /></MemoryRouter>);
    await screen.findByText(/Welcome, Jane Doe/);

    fireEvent.change(screen.getByLabelText("Username"), {
      target: { value: "jane.doe" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "secure-password" },
    });
    fireEvent.change(screen.getByLabelText("Confirm Password"), {
      target: { value: "secure-password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create Account" }));

    expect(await screen.findByText(
      "Your account has been created successfully."
    )).toBeInTheDocument();
    expect(acceptMock).toHaveBeenCalledWith(
      "invitation-token", "jane.doe", "secure-password"
    );
    expect(screen.getByRole("link", { name: "Continue to Login" }))
      .toHaveAttribute("href", "/login");
  });

  it("shows an expired invitation error returned by the API", async () => {
    validateMock.mockRejectedValue(new ApiError(409, {
      message: "Invitation has expired",
    }));
    render(<MemoryRouter><AcceptInvitation /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText("Invitation has expired")).toBeInTheDocument();
    });
  });
});
