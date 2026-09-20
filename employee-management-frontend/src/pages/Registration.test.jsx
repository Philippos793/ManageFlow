import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Registration from "./Registration.jsx";

const { submitMock } = vi.hoisted(() => ({ submitMock: vi.fn() }));

vi.mock("../services/registrationRequestService.js", () => ({
  default: { submit: submitMock },
}));

function renderRegistration() {
  return render(<MemoryRouter><Registration /></MemoryRouter>);
}

function fillForm(password = "SecurePass123", confirmPassword = password) {
  fireEvent.change(screen.getByLabelText("First Name"), { target: { value: "Alex" } });
  fireEvent.change(screen.getByLabelText("Last Name"), { target: { value: "Morgan" } });
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: "alex.morgan@example.com" } });
  fireEvent.change(screen.getByLabelText("Username"), { target: { value: "alex.morgan" } });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.change(screen.getByLabelText("Confirm Password"), { target: { value: confirmPassword } });
}

describe("Registration", () => {
  beforeEach(() => submitMock.mockReset());

  it("renders every account request field", () => {
    renderRegistration();
    expect(screen.getByRole("heading", { name: "Request a ManageFlow account" })).toBeInTheDocument();
    expect(screen.getByLabelText("First Name")).toBeRequired();
    expect(screen.getByLabelText("Last Name")).toBeRequired();
    expect(screen.getByLabelText("Email")).toBeRequired();
    expect(screen.getByLabelText("Username")).toBeRequired();
    expect(screen.getByLabelText("Password")).toBeRequired();
    expect(screen.getByLabelText("Confirm Password")).toBeRequired();
  });

  it("toggles password and confirmation visibility independently", () => {
    renderRegistration();
    const password = screen.getByLabelText("Password");
    const confirmation = screen.getByLabelText("Confirm Password");

    expect(password).toHaveAttribute("type", "password");
    expect(confirmation).toHaveAttribute("type", "password");
    fireEvent.click(screen.getByRole("button", { name: "Show Password" }));
    fireEvent.click(screen.getByRole("button", { name: "Show Confirm Password" }));
    expect(password).toHaveAttribute("type", "text");
    expect(confirmation).toHaveAttribute("type", "text");
    fireEvent.click(screen.getByRole("button", { name: "Hide Confirm Password" }));
    expect(confirmation).toHaveAttribute("type", "password");
  });

  it("does not submit when password confirmation does not match", () => {
    renderRegistration();
    fillForm("SecurePass123", "DifferentPass123");
    fireEvent.click(screen.getByRole("button", { name: "Request Account" }));
    expect(screen.getByText("Passwords do not match.")).toBeInTheDocument();
    expect(submitMock).not.toHaveBeenCalled();
  });

  it("submits the request and shows the approval message", async () => {
    submitMock.mockResolvedValue({ id: 1, status: "PENDING" });
    renderRegistration();
    fillForm();
    fireEvent.click(screen.getByRole("button", { name: "Request Account" }));

    await waitFor(() => expect(submitMock).toHaveBeenCalledWith({
      firstName: "Alex",
      lastName: "Morgan",
      email: "alex.morgan@example.com",
      username: "alex.morgan",
      password: "SecurePass123",
    }));
    expect(await screen.findByText("Your account request has been submitted and is waiting for admin approval.")).toBeInTheDocument();
  });
});
