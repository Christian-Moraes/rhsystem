import { type KeyboardEvent } from 'react'

/** Normaliza texto colado ou digitado para HH:mm (aceita H:mm). */
export function normalizePunchTime(raw: string): string {
  const t = raw.trim()
  if (!t) return ''
  const m = t.match(/^(\d{1,2}):(\d{1,2})$/)
  if (!m) return ''
  const h = Math.min(23, Math.max(0, parseInt(m[1], 10)))
  const min = Math.min(59, Math.max(0, parseInt(m[2], 10)))
  return `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`
}

function focusAdjacentGradeTimeInput(current: HTMLInputElement, direction: 1 | -1) {
  const root = current.closest<HTMLElement>('[data-timesheet-grade]')
  if (!root) return
  const list = Array.from(root.querySelectorAll<HTMLInputElement>('input[type="time"]'))
  const i = list.indexOf(current)
  if (i === -1) return
  const target = list[i + direction]
  if (target) target.focus()
}

function bumpMinutes(iso: string, deltaMin: number): string {
  if (!iso || !/^\d{1,2}:\d{2}$/.test(iso)) return iso
  const [h, min] = iso.split(':').map(Number)
  let total = h * 60 + min + deltaMin
  total = ((total % (24 * 60)) + 24 * 60) % (24 * 60)
  const nh = Math.floor(total / 60)
  const nm = total % 60
  return `${String(nh).padStart(2, '0')}:${String(nm).padStart(2, '0')}`
}

type PunchTimeInputProps = {
  value: string
  onChange: (v: string) => void
  id?: string
  'aria-label': string
  /** Grade densa: campo menor, menos padding. */
  compact?: boolean
}

/**
 * Campo de horário da grade: alvo maior, seleção ao focar, Enter/Shift+Enter para o próximo/anterior
 * `input[type=time]` dentro de `[data-timesheet-grade]`, setas ↑↓ em passos de 5 min,
 * colar "7:30" normalizado no blur.
 */
export function PunchTimeInput({
  value,
  onChange,
  id,
  'aria-label': ariaLabel,
  compact = false,
}: PunchTimeInputProps) {
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      focusAdjacentGradeTimeInput(e.currentTarget, e.shiftKey ? -1 : 1)
      return
    }
    if (e.key !== 'ArrowUp' && e.key !== 'ArrowDown') return
    e.preventDefault()
    const current = value && /^\d{1,2}:\d{2}$/.test(value) ? normalizePunchTime(value) : ''
    const base = current || '07:00'
    const step = e.shiftKey ? 15 : 5
    const d = e.key === 'ArrowUp' ? step : -step
    onChange(bumpMinutes(base, d))
  }

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    const text = e.clipboardData.getData('text').trim()
    if (text.includes(':') && /^\d{1,2}:\d{1,2}/.test(text)) {
      e.preventDefault()
      const norm = normalizePunchTime(text)
      if (norm) onChange(norm)
    }
  }

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const v = e.target.value
    if (!v) {
      onChange('')
      return
    }
    const norm = normalizePunchTime(v)
    if (norm && norm !== v) onChange(norm)
  }

  return (
    <input
      id={id}
      type="time"
      step={60}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      onFocus={(e) => {
        requestAnimationFrame(() => e.target.select())
      }}
      onKeyDown={handleKeyDown}
      onPaste={handlePaste}
      onBlur={handleBlur}
      aria-label={ariaLabel}
      title="Enter: próximo horário na grade · Shift+Enter: anterior · Setas ↑↓: ±5 min (Shift: ±15 min) · Colar 7:30."
      className={
        compact
          ? 'h-9 w-[6rem] min-h-[2.25rem] cursor-text rounded-md border border-slate-200 bg-white px-1.5 py-0 text-[13px] font-semibold tabular-nums leading-9 text-slate-800 transition-colors hover:border-slate-300 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500/30 [&::-webkit-calendar-picker-indicator]:opacity-45 [&::-webkit-calendar-picker-indicator]:scale-90'
          : 'min-h-[2.75rem] w-[7.25rem] cursor-text rounded-lg border border-gray-300 bg-white px-2.5 py-2 text-sm font-medium tabular-nums text-gray-900 shadow-sm transition-all hover:border-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/25'
      }
    />
  )
}
