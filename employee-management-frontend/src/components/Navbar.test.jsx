import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Navbar from "./Navbar.jsx";

const { logoutMock } = vi.hoisted(() => ({ logoutMock: vi.fn() }));

vi.mock("../services/authService", () => ({
  default: {
    isAuthenticated: () => true,
    getRole: () => "EMPLOYEE",
    getUsername: () => "jane.doe",
    logout: logoutMock,
  },
}));

vi.mock("./NotificationsBell.jsx", () => ({
  default: () => null,
}));

function renderNavbar() {
  return render(
    <MemoryRouter initialEntries={["/"]}>
      <Navbar />
      <Routes>
        <Route path="/login" element={<p>Login destination</p>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("Navbar logout confirmation", () => {
  beforeEach(() => logoutMock.mockReset());

  it("keeps the user signed in when logout is cancelled", () => {
    renderNavbar();
    fireEvent.click(screen.getByRole("button", { name: "Logout" }));

    expect(screen.getByRole("dialog", { name: "Log out?" })).toBeInTheDocument();
    expect(screen.getByText("Are you sure you want to log out?")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("dialog", { name: "Log out?" })).not.toBeInTheDocument();
    expect(logoutMock).not.toHaveBeenCalled();
  });

  it("logs out and navigates to login after confirmation", async () => {
    renderNavbar();
    fireEvent.click(screen.getByRole("button", { name: "Logout" }));
    fireEvent.click(screen.getByRole("button", { name: "Log Out" }));

    expect(logoutMock).toHaveBeenCalledTimes(1);
    expect(await screen.findByText("Login destination")).toBeInTheDocument();
  });

  it("closes with Escape and restores focus to the Logout button", async () => {
    renderNavbar();
    const logoutButton = screen.getByRole("button", { name: "Logout" });
    logoutButton.focus();
    fireEvent.click(logoutButton);
    fireEvent.keyDown(document, { key: "Escape" });

    await waitFor(() => {
      expect(screen.queryByRole("dialog", { name: "Log out?" })).not.toBeInTheDocument();
      expect(logoutButton).toHaveFocus();
    });
    expect(logoutMock).not.toHaveBeenCalled();
  });
});
