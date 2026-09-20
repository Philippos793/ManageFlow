import { useCallback, useEffect, useRef, useState } from "react";
import taskService from "../services/taskService.js";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";
import Button from "./ui/Button.jsx";
import FeedbackMessage from "./ui/FeedbackMessage.jsx";
import LoadingSpinner from "./ui/LoadingSpinner.jsx";

function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) return "Unknown size";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatUploadedAt(value) {
  if (!value) return "Unknown upload time";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unknown upload time";
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function triggerDownload(blob, filename) {
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

function TaskAttachments({ taskId, taskStatus, isAdmin, isEmployee }) {
  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);
  const uploadLockedRef = useRef(false);
  const canUploadResource = isAdmin && taskStatus === "IN_PROGRESS";
  const canUploadSubmission = isEmployee && taskStatus === "IN_PROGRESS";
  const adminResources = attachments.filter(
    (attachment) => attachment.attachmentType === "ADMIN_RESOURCE"
  );
  const employeeSubmissions = attachments.filter(
    (attachment) => attachment.attachmentType === "EMPLOYEE_SUBMISSION"
  );

  const loadAttachments = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await taskService.getAttachments(taskId);
      setAttachments(Array.isArray(data) ? data : []);
    } catch (requestError) {
      setAttachments([]);
      setError(getFriendlyErrorMessage(requestError, "Failed to load attachments."));
    } finally {
      setLoading(false);
    }
  }, [taskId]);

  useEffect(() => {
    let mounted = true;

    taskService.getAttachments(taskId)
      .then((data) => {
        if (mounted) setAttachments(Array.isArray(data) ? data : []);
      })
      .catch((requestError) => {
        if (mounted) setError(getFriendlyErrorMessage(requestError, "Failed to load attachments."));
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => { mounted = false; };
  }, [taskId]);

  async function handleUpload() {
    if (!selectedFile || uploadLockedRef.current) return;

    uploadLockedRef.current = true;
    setUploading(true);
    setError("");
    try {
      await taskService.uploadAttachment(taskId, selectedFile);
      setSelectedFile(null);
      await loadAttachments();
    } catch (requestError) {
      setError(getFriendlyErrorMessage(requestError, "Failed to upload attachment."));
    } finally {
      uploadLockedRef.current = false;
      setUploading(false);
    }
  }

  async function handleDownload(attachment) {
    if (downloadingId !== null) return;

    setDownloadingId(attachment.id);
    setError("");
    try {
      const blob = await taskService.downloadAttachment(taskId, attachment.id);
      triggerDownload(blob, attachment.originalFilename);
    } catch (requestError) {
      setError(getFriendlyErrorMessage(requestError, "Failed to download attachment."));
    } finally {
      setDownloadingId(null);
    }
  }

  function renderAttachmentList(items, emptyMessage) {
    if (items.length === 0) {
      return <p className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">{emptyMessage}</p>;
    }

    return (
      <ul className="space-y-2">
        {items.map((attachment) => (
          <li key={attachment.id} className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-3 py-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-slate-800">{attachment.originalFilename}</p>
              <p className="mt-1 text-xs text-slate-500">
                Uploaded by {attachment.uploaderUsername || "Unknown uploader"}
              </p>
              <p className="mt-0.5 text-xs text-slate-500">
                {formatUploadedAt(attachment.uploadedAt)} · {formatFileSize(attachment.fileSize)}
              </p>
            </div>
            <button type="button" disabled={downloadingId !== null} onClick={() => handleDownload(attachment)} className="shrink-0 rounded-lg border border-blue-200 px-3 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-60">
              {downloadingId === attachment.id ? "Downloading..." : "Download"}
            </button>
          </li>
        ))}
      </ul>
    );
  }

  return (
    <section className="mt-5 border-t border-slate-100 pt-5" aria-labelledby={`task-${taskId}-attachments-title`}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 id={`task-${taskId}-attachments-title`} className="text-sm font-bold text-slate-800">Attachments</h3>
          <p className="mt-1 text-xs text-slate-500">Files shared between the task creator and assigned employee.</p>
        </div>
      </div>

      {(canUploadResource || canUploadSubmission) && (
        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50/70 p-3">
          <label htmlFor={`task-${taskId}-attachment-upload`} className="block text-sm font-semibold text-slate-700">
            {canUploadResource ? "Upload Admin Resource" : "Upload Employee Submission"}
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row sm:items-center">
            <input id={`task-${taskId}-attachment-upload`} type="file" disabled={uploading} onChange={(event) => setSelectedFile(event.target.files?.[0] || null)} className="min-w-0 flex-1 text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-blue-50 file:px-3 file:py-2 file:text-sm file:font-semibold file:text-blue-700 hover:file:bg-blue-100 disabled:cursor-not-allowed" />
            <Button disabled={!selectedFile || uploading} onClick={handleUpload} className="w-full shrink-0 sm:w-auto">
              {uploading ? "Uploading..." : "Upload"}
            </Button>
          </div>
          <p className="mt-2 text-xs text-slate-500">File size is validated securely by the server.</p>
        </div>
      )}

      <div className="mt-4">
        {loading ? <LoadingSpinner compact label="Loading attachments..." /> : error ? (
          <div className="space-y-3">
            <FeedbackMessage>{error}</FeedbackMessage>
            <Button variant="secondary" onClick={loadAttachments}>Retry</Button>
          </div>
        ) : (
          <div className="space-y-5">
            <div>
              <h4 className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500">Admin Resources</h4>
              {renderAttachmentList(adminResources, "No admin resources yet.")}
            </div>
            <div>
              <h4 className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500">Employee Submissions</h4>
              {renderAttachmentList(employeeSubmissions, "No employee submissions yet.")}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}

export default TaskAttachments;
