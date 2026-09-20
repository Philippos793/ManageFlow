function LoadingSpinner({ label = "Loading...", compact = false }) {
  return (
    <div className={`flex items-center justify-center ${compact ? "gap-2" : "min-h-72"}`}>
      <div className={compact ? "flex items-center gap-2" : "text-center"}>
        <span className={`${compact ? "size-4 border-2" : "mx-auto block size-8 border-4"} animate-spin rounded-full border-slate-200 border-t-blue-600`} />
        <p className={`${compact ? "" : "mt-4"} text-sm font-medium text-slate-500`}>
          {label}
        </p>
      </div>
    </div>
  );
}

export default LoadingSpinner;
