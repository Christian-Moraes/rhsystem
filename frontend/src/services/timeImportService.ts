import api from './api'
import { TimeEvent } from './timeEventService'

export interface TimeImportResponse {
  events: TimeEvent[]
  totalEvents: number
  confirmedEvents: number
  reviewEvents: number
  uncertainEvents: number
  gradeByDate?: Record<string, string[]>
  message?: string
}

export const timeImportService = {
  importFile: async (
    companyId: string,
    employeeId: string,
    file: File,
    fileType?: string,
    month?: number,
    year?: number,
    quinzena?: number
  ): Promise<TimeImportResponse> => {
    const formData = new FormData()
    formData.append('file', file)
    if (fileType) {
      formData.append('fileType', fileType)
    }
    if (month != null) {
      formData.append('month', String(month))
    }
    if (year != null) {
      formData.append('year', String(year))
    }
    if (quinzena != null && (quinzena === 1 || quinzena === 2)) {
      formData.append('quinzena', String(quinzena))
    }

    const response = await api.post(
      `/companies/${companyId}/time-import/employee/${employeeId}`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    )
    return response.data
  },

  createDemoEvents: async (
    companyId: string,
    employeeId: string,
    month: number,
    year: number
  ): Promise<TimeImportResponse> => {
    const response = await api.post(
      `/companies/${companyId}/time-import/employee/${employeeId}/demo?month=${month}&year=${year}`
    )
    return response.data
  },

  /** Seed via GET (mesmo efeito do demo) — útil se o POST não estiver chegando no backend */
  seedDemoEvents: async (
    companyId: string,
    employeeId: string,
    month: number,
    year: number
  ): Promise<TimeImportResponse> => {
    const response = await api.get(
      `/companies/${companyId}/time-import/employee/${employeeId}/seed?month=${month}&year=${year}`
    )
    return response.data
  },
}
