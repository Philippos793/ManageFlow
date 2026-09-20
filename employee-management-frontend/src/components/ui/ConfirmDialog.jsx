import Button from "./Button.jsx";
import useModalAccessibility from "../../hooks/useModalAccessibility.js";

function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "Delete",
  cancelLabel = "Cancel",
  loadingLabel = "Deleting...",
  confirmVariant = "danger",
  loading = false,
  onConfirm,
  onCancel,
}) {
  const dialogRef = useModalAccessibility(open, () => {
    if (!loading) onCancel();
  });

  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm" role="presentation">
      <div ref={dialogRef} tabIndex="-1" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title" className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl outline-none sm:p-8">
        <span className="flex size-12 items-center justify-center rounded-full bg-red-100 text-xl font-bold text-red-600">!</span>
        <h2 id="confirm-dialog-title" className="mt-5 text-xl font-bold text-slate-950">
          {title}
        </h2>
        <p className="mt-2 leading-6 text-slate-600">{message}</p>

        <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button variant="secondary" disabled={loading} onClick={onCancel}>
            {cancelLabel}
          </Button>
          <Button variant={confirmVariant} disabled={loading} onClick={onConfirm}>
            {loading ? loadingLabel : confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmDialog;
