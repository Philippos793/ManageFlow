import { afterEach, describe, expect, it, vi } from "vitest";

const { getTokenMock, handleUnauthorizedMock } = vi.hoisted(() => ({
  getTokenMock: vi.fn(() => "expired-token"),
  handleUnauthorizedMock: vi.fn(),
}));

vi.mock("./authService", () => ({
  default: {
    getToken: getTokenMock,
    handleUnauthorized: handleUnauthorizedMock,
  },
}));

import employeeService from "./employeeService.js";

describe("employeeService unauthorized handling", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    handleUnauthorizedMock.mockClear();
  });

  it("delegates a protected API 401 to the central unauthorized handler", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        text: vi.fn().mockResolvedValue(
          JSON.stringify({
            status: 401,
            error: "Unauthorized",
            message: "Authentication is required",
          })
        ),
      })
    );

    await expect(employeeService.getAllEmployees()).rejects.toMatchObject({
      status: 401,
    });
    expect(handleUnauthorizedMock).toHaveBeenCalledOnce();
  });
});
