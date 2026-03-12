import api from './api'

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
}
