import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../utils/apiErrors.js";
import Login from "./Login.jsx";

const { loginMock } = vi.hoisted(() => ({
  loginMock: vi.fn(),
}));

vi.mock("../services/authService", () => ({
  default: {
    login: loginMock,
  },
}));

describe("Login", () => {
  beforeEach(() => {
    loginMock.mockReset();
  });

  it("renders the login page", () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(
      screen.getByRole("heading", { name: "Welcome to ManageFlow" })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Sign In" })
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Request an account" })).toHaveAttribute("href", "/register");
  });

  it("shows and hides the password", () => {
    render(<MemoryRouter><Login /></MemoryRouter>);
    const password = screen.getByLabelText("Password");

    expect(password).toHaveAttribute("type", "password");
    fireEvent.click(screen.getByRole("button", { name: "Show Password" }));
    expect(password).toHaveAttribute("type", "text");
    fireEvent.click(screen.getByRole("button", { name: "Hide Password" }));
    expect(password).toHaveAttribute("type", "password");
  });

  it("shows invalid credentials after a failed login", async () => {
    loginMock.mockRejectedValue(
      new ApiError(401, { message: "Invalid username or password" })
    );

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Username"), {
      target: { value: "unknown" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "wrong-password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(
      await screen.findByText("Invalid username or password.")
    ).toBeInTheDocument();
  });
});
