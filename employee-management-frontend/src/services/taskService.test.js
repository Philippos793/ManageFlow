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

import taskService from "./taskService.js";

describe("taskService", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    handleUnauthorizedMock.mockClear();
  });

  it("sends the bearer token and delegates 401 responses to central logout handling", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      text: vi.fn().mockResolvedValue(JSON.stringify({
        status: 401,
        error: "Unauthorized",
        message: "Authentication is required",
      })),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(taskService.getAllTasks()).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/tasks$/),
      expect.objectContaining({ headers: { Authorization: "Bearer expired-token" } })
    );
    expect(handleUnauthorizedMock).toHaveBeenCalledOnce();
  });

  it("sends task edits as an authenticated PATCH request", async () => {
    const response = { id: 11, title: "Updated report" };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: vi.fn().mockResolvedValue(JSON.stringify(response)),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(taskService.updateTask(11, { title: "Updated report" })).resolves.toEqual(response);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/tasks\/11$/),
      expect.objectContaining({
        method: "PATCH",
        headers: expect.objectContaining({
          "Content-Type": "application/json",
          Authorization: "Bearer expired-token",
        }),
      })
    );
  });

  it("sends a request-changes payload as an authenticated POST request", async () => {
    const response = { id: 11, status: "IN_PROGRESS" };
    const payload = { feedback: "Please revise.", additionalTimeMinutes: 60 };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: vi.fn().mockResolvedValue(JSON.stringify(response)),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(taskService.requestChanges(11, payload)).resolves.toEqual(response);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/tasks\/11\/request-changes$/),
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "Content-Type": "application/json",
          Authorization: "Bearer expired-token",
        }),
        body: JSON.stringify(payload),
      })
    );
  });

  it("loads a task activity history with the bearer token", async () => {
    const response = [{ type: "TASK_CREATED", message: "Task created." }];
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, text: vi.fn().mockResolvedValue(JSON.stringify(response)) });
    vi.stubGlobal("fetch", fetchMock);

    await expect(taskService.getTaskActivities(11)).resolves.toEqual(response);
    expect(fetchMock).toHaveBeenCalledWith(expect.stringMatching(/\/tasks\/11\/activities$/), expect.objectContaining({ headers: { Authorization: "Bearer expired-token" } }));
  });
});
