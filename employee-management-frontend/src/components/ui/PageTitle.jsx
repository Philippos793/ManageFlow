function PageTitle({ eyebrow, title, description, action }) {
  return (
    <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
      <div>
        {eyebrow && (
          <p className="text-sm font-semibold uppercase tracking-wider text-blue-600">
            {eyebrow}
          </p>
        )}
        <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
          {title}
        </h1>
        {description && <p className="mt-2 text-slate-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export default PageTitle;
