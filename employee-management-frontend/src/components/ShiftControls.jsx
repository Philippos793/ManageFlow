import { useCallback, useEffect, useRef, useState } from "react";
import workShiftService from "../services/workShiftService.js";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";
import Button from "./ui/Button.jsx";
import FeedbackMessage from "./ui/FeedbackMessage.jsx";

const labels = {
  title: "Shift Tracking",
  start: "Start Shift",
  end: "End Shift",
  starting: "Starting shift...",
  ending: "Ending shift...",
  checking: "Checking shift status...",
  active: "Shift active",
  startedAt: "Started at",
  started: "Shift started successfully.",
  ended: "Shift ended successfully.",
  failed: "The shift action failed.",
  statusFailed: "Unable to check the current shift status.",
  retry: "Retry",
};

function formatStartTime(value) {
  if (!value) return "";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  return new Intl.DateTimeFormat("en", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function ShiftControls() {
  const [pendingAction, setPendingAction] = useState("");
  const [statusLoading, setStatusLoading] = useState(true);
  const [statusKnown, setStatusKnown] = useState(false);
  const [active, setActive] = useState(false);
  const [startTime, setStartTime] = useState(null);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const mountedRef = useRef(false);
  const actionLockedRef = useRef(false);
  const statusRequestRef = useRef(null);

  const refreshStatus = useCallback(() => {
    if (statusRequestRef.current) return statusRequestRef.current;

    if (mountedRef.current) {
      setStatusLoading(true);
      setError("");
    }

    const request = workShiftService
      .getCurrentShift()
      .then((status) => {
        if (!mountedRef.current) return status;
        setActive(Boolean(status.active));
        setStartTime(status.startTime ?? null);
        setStatusKnown(true);
        return status;
      })
      .catch((requestError) => {
        if (mountedRef.current) {
          setStatusKnown(false);
          setError(
            requestError?.data?.message ||
              getFriendlyErrorMessage(requestError, labels.statusFailed)
          );
        }
        throw requestError;
      })
      .finally(() => {
        statusRequestRef.current = null;
        if (mountedRef.current) setStatusLoading(false);
      });

    statusRequestRef.current = request;
    return request;
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    refreshStatus().catch(() => {});

    function refreshWhenVisible() {
      if (!actionLockedRef.current && document.visibilityState === "visible") {
        refreshStatus().catch(() => {});
      }
    }

    function refreshOnFocus() {
      if (!actionLockedRef.current) refreshStatus().catch(() => {});
    }

    window.addEventListener("focus", refreshOnFocus);
    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      mountedRef.current = false;
      window.removeEventListener("focus", refreshOnFocus);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [refreshStatus]);

  async function handleShiftAction(action) {
    if (actionLockedRef.current || !statusKnown || statusLoading) return;
    if ((action === "start" && active) || (action === "end" && !active)) return;

    actionLockedRef.current = true;
    setPendingAction(action);
    setSuccess("");
    setError("");

    try {
      if (action === "start") {
        await workShiftService.startShift();
        setSuccess(labels.started);
      } else {
        await workShiftService.endShift();
        setSuccess(labels.ended);
      }
      await refreshStatus();
    } catch (requestError) {
      if (mountedRef.current && statusKnown) {
        setError(
          requestError?.data?.message ||
            getFriendlyErrorMessage(requestError, labels.failed)
        );
      }
    } finally {
      actionLockedRef.current = false;
      if (mountedRef.current) setPendingAction("");
    }
  }

  const isPending = pendingAction !== "";
  const actionsUnavailable = !statusKnown || statusLoading || isPending;
  const formattedStartTime = formatStartTime(startTime);

  return (
    <div className="mt-8 rounded-2xl border border-slate-200 bg-slate-50 p-5">
      <p className="text-sm font-semibold uppercase tracking-wide text-slate-500">
        {labels.title}
      </p>

      {!statusKnown && statusLoading && (
        <p role="status" className="mt-3 text-sm font-medium text-slate-600">
          {labels.checking}
        </p>
      )}

      {statusKnown && active && (
        <div role="status" className="mt-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          <span className="font-semibold">{labels.active}</span>
          {formattedStartTime && (
            <span className="ml-2">� {labels.startedAt} {formattedStartTime}</span>
          )}
        </div>
      )}

      <div className="mt-4 flex flex-wrap gap-3">
        <Button
          disabled={actionsUnavailable || active}
          onClick={() => handleShiftAction("start")}
          className={active ? "bg-red-600 text-white ring-4 ring-red-100 hover:bg-red-600 disabled:bg-red-600" : ""}
        >
          {pendingAction === "start" ? labels.starting : labels.start}
        </Button>
        <Button
          variant="secondary"
          disabled={actionsUnavailable || !active}
          onClick={() => handleShiftAction("end")}
        >
          {pendingAction === "end" ? labels.ending : labels.end}
        </Button>
      </div>

      <div className="mt-4 space-y-3">
        <FeedbackMessage type="success">{success}</FeedbackMessage>
        <FeedbackMessage>{error}</FeedbackMessage>
        {!statusKnown && !statusLoading && error && (
          <Button variant="secondary" onClick={() => refreshStatus().catch(() => {})}>
            {labels.retry}
          </Button>
        )}
      </div>
    </div>
  );
}

export default ShiftControls;
