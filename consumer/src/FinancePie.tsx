type Slice = { label: string; value: number; color: string }

export function FinancePie({ downPayment, financed }: { downPayment?: number; financed?: number }) {
  const down = Number(downPayment || 0)
  const rest = Number(financed || 0)
  const total = down + rest
  if (total <= 0) return null
  const slices: Slice[] = [
    { label: 'Down payment', value: down, color: '#0c3d3a' },
    { label: 'Financed', value: rest, color: '#7acbc7' },
  ]
  let angle = -90
  const paths = slices.map((slice) => {
    const sweep = (slice.value / total) * 360
    const start = angle
    angle += sweep
    return { ...slice, d: arc(80, 80, 64, start, angle) }
  })
  return (
    <div className="finance-pie" title="Down payment versus financed amount">
      <svg viewBox="0 0 160 160" width="72" height="72" aria-hidden>
        {paths.map((slice) => <path key={slice.label} d={slice.d} fill={slice.color} />)}
        <circle cx="80" cy="80" r="34" fill="#ffffff" />
      </svg>
      <ul>
        {slices.map((slice) => (
          <li key={slice.label}>
            <i style={{ background: slice.color }} />
            <span>{slice.label}</span>
            <strong>{Math.round((slice.value / total) * 100)}%</strong>
          </li>
        ))}
      </ul>
    </div>
  )
}

function arc(cx: number, cy: number, r: number, startDeg: number, endDeg: number) {
  const start = polar(cx, cy, r, endDeg)
  const end = polar(cx, cy, r, startDeg)
  const large = endDeg - startDeg > 180 ? 1 : 0
  return `M ${cx} ${cy} L ${start.x} ${start.y} A ${r} ${r} 0 ${large} 0 ${end.x} ${end.y} Z`
}

function polar(cx: number, cy: number, r: number, deg: number) {
  const rad = (deg * Math.PI) / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}
