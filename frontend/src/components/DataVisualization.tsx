type DataVisualizationProps = {
  title?: string
  subtitle?: string
  data?: Array<Record<string, unknown>>
  emptyMessage?: string
  className?: string
}

export function DataVisualization({
  title,
  subtitle,
  data = [],
  emptyMessage = 'No chart data available',
  className = '',
}: DataVisualizationProps) {
  return (
    <div className={`rounded-2xl border border-white/10 bg-white/5 p-4 ${className}`.trim()}>
      <div className="mb-4">
        {title && <h3 className="text-lg font-semibold text-white">{title}</h3>}
        {subtitle && <p className="text-sm text-white/60">{subtitle}</p>}
      </div>

      {data.length === 0 ? (
        <div className="rounded-xl border border-dashed border-white/15 bg-black/10 p-6 text-center text-sm text-white/50">
          {emptyMessage}
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {data.slice(0, 6).map((entry, index) => {
            const label = String(entry.label ?? entry.name ?? entry.title ?? `Series ${index + 1}`)
            const value = entry.value ?? entry.count ?? entry.amount ?? entry.total
            const secondary = entry.secondary ?? entry.subtitle ?? entry.note

            return (
              <div key={`${label}-${index}`} className="rounded-xl border border-white/10 bg-black/10 p-3">
                <p className="text-xs uppercase tracking-[0.2em] text-white/40">{label}</p>
                <p className="mt-2 text-2xl font-semibold text-white">{String(value ?? 'N/A')}</p>
                {secondary != null && <p className="mt-1 text-sm text-white/60">{String(secondary)}</p>}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default DataVisualization
