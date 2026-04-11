import api from './api'
import type { AxiosError } from 'axios'

/** Resposta de GET .../closure-breakdown (exportação / espelho do motor). */
export interface ClosureBreakdownDailyLine {
  date: string
  punchTimeline?: string
  workedRawMinutes: number
  workedCountedMinutes: number
  extraMinutes: number
  delayMinutes: number
  absenceMinutes: number
  firstEntryTime: string | null
  calculationNote: string
}

export interface ClosureBreakdown {
  companyName?: string
  companyEnableOvertime?: boolean
  companyEnableTimeBank?: boolean
  companyOvertimePercentage?: number | null
  companyWeekendPercentage?: number | null
  employeeName: string
  month: number
  year: number
  scheduleDailyNormMinutes: number
  scheduleEntryTime: string | null
  scheduleExitTime: string | null
  scheduleBreakMinutes: number | null
  scheduleToleranceLate: number | null
  scheduleToleranceLeave: number | null
  dailyLines: ClosureBreakdownDailyLine[]
  calculatedWorkedMinutes: number
  calculatedExtraMinutes: number
  calculatedDelayMinutes: number
  calculatedAbsenceMinutes: number
  closedAt: string | null
  storedWorkedMinutes: number | null
  storedExtraMinutes: number | null
  storedDelayMinutes: number | null
  storedAbsenceMinutes: number | null
  formulaDocumentation: string[]
}

export interface Timesheet {
  id: string
  employeeId: string
  employeeName: string
  month: number
  year: number
  workedHours: number
  extraHours: number
  delayMinutes: number
  absenceMinutes: number
  closedAt: string
}

export const timesheetService = {
  close: async (
    companyId: string,
    employeeId: string,
    month: number,
    year: number
  ): Promise<Timesheet> => {
    const response = await api.post(
      `/companies/${companyId}/timesheets/employee/${employeeId}/month/${month}/year/${year}`
    )
    return response.data
  },

  get: async (
    companyId: string,
    employeeId: string,
    month: number,
    year: number
  ): Promise<Timesheet> => {
    const response = await api.get(
      `/companies/${companyId}/timesheets/employee/${employeeId}/month/${month}/year/${year}`
    )
    return response.data
  },

  getByCompany: async (
    companyId: string,
    month: number,
    year: number
  ): Promise<Timesheet[]> => {
    const response = await api.get(
      `/companies/${companyId}/timesheets/month/${month}/year/${year}`
    )
    return response.data
  },

  getClosureBreakdown: async (
    companyId: string,
    employeeId: string,
    month: number,
    year: number
  ): Promise<ClosureBreakdown> => {
    const response = await api.get(
      `/companies/${companyId}/timesheets/employee/${employeeId}/month/${month}/year/${year}/closure-breakdown`
    )
    return response.data
  },

  /** CSV ou Excel (.xls) gerados no backend (batidas, totais, regras). */
  downloadClosureExport: async (
    companyId: string,
    employeeId: string,
    month: number,
    year: number,
    format: 'xls' | 'csv'
  ): Promise<void> => {
    try {
      const response = await api.get(
        `/companies/${companyId}/timesheets/employee/${employeeId}/month/${month}/year/${year}/export`,
        { params: { format }, responseType: 'blob' }
      )
      const disposition = response.headers['content-disposition'] as string | undefined
      let filename = `fechamento_${year}-${String(month).padStart(2, '0')}.${format}`
      if (disposition) {
        const star = disposition.match(/filename\*=UTF-8''([^;\s]+)/i)
        const plain = disposition.match(/filename="([^"]+)"/i)
        const raw = star?.[1] ?? plain?.[1]
        if (raw) {
          try {
            filename = decodeURIComponent(raw)
          } catch {
            filename = raw
          }
        }
      }
      const url = URL.createObjectURL(response.data as Blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
    } catch (err: unknown) {
      const ax = err as AxiosError<Blob>
      if (ax.response?.data instanceof Blob) {
        const text = await ax.response.data.text()
        try {
          const j = JSON.parse(text) as { message?: string }
          throw new Error(j.message ?? 'Falha na exportação.')
        } catch (e) {
          if (e instanceof SyntaxError) {
            throw new Error(text.trim().slice(0, 200) || 'Falha na exportação.')
          }
          throw e
        }
      }
      throw err
    }
  },
}
