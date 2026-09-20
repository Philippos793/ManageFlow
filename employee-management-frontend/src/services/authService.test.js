import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import authService from "./authService.js";

describe("authService unauthorized handling", () => {
  beforeEach(() => {
    window.history.replaceState({}, "", "/login");
    localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("stores and returns the authenticated user id after login", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ token: "token", userId: 17, username: "admin", role: "ADMIN" }),
    }));

    await authService.login("admin", "password");

    expect(localStorage.getItem("userId")).toBe("17");
    expect(authService.getUserId()).toBe(17);
  });

  it("clears all authentication data after a 401", () => {
    localStorage.setItem("token", "expired-token");
    localStorage.setItem("userId", "17");
    localStorage.setItem("username", "employee");
    localStorage.setItem("role", "EMPLOYEE");

    authService.handleUnauthorized();

    expect(localStorage.getItem("token")).toBeNull();
    expect(localStorage.getItem("userId")).toBeNull();
    expect(localStorage.getItem("username")).toBeNull();
    expect(localStorage.getItem("role")).toBeNull();
    expect(window.location.pathname).toBe("/login");
  });
});
