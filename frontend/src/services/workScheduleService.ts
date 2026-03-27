import api from './api'

export interface WorkSchedule {
  id: string
  dailyHours: number
  entryTime: string
  exitTime: string
  breakMinutes: number
  toleranceLate: number
  toleranceLeave: number
}

export interface WorkScheduleCreateRequest {
  dailyHours: number
  entryTime: string
  exitTime: string
  breakMinutes: number
  toleranceLate: number
  toleranceLeave: number
}

export const workScheduleService = {
  getAll: async (companyId: string): Promise<WorkSchedule[]> => {
    const response = await api.get(`/companies/${companyId}/work-schedules`)
    return response.data
  },

  create: async (
    companyId: string,
    data: WorkScheduleCreateRequest
  ): Promise<WorkSchedule> => {
    const response = await api.post(`/companies/${companyId}/work-schedules`, data)
    return response.data
  },
}
