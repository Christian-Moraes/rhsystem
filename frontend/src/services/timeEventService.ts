import api from './api'

export interface TimeEvent {
  id: string
  employeeId: string
  eventType: string
  eventTime: string
  source: string
  status: string
  createdAt: string
}

export interface TimeEventRequest {
  employeeId: string
  eventType: string
  eventTime: string
}

export const timeEventService = {
  getByEmployee: async (companyId: string, employeeId: string): Promise<TimeEvent[]> => {
    const response = await api.get(`/companies/${companyId}/time-events/employee/${employeeId}`)
    return response.data
  },

  getByEmployeeAndMonth: async (
    companyId: string,
    employeeId: string,
    month: number,
    year: number
  ): Promise<TimeEvent[]> => {
    const response = await api.get(
      `/companies/${companyId}/time-events/employee/${employeeId}/month/${month}/year/${year}`
    )
    return response.data
  },

  create: async (companyId: string, data: TimeEventRequest): Promise<TimeEvent> => {
    const response = await api.post(`/companies/${companyId}/time-events`, data)
    return response.data
  },

  update: async (companyId: string, id: string, data: TimeEventRequest): Promise<TimeEvent> => {
    const response = await api.put(`/companies/${companyId}/time-events/${id}`, data)
    return response.data
  },

  delete: async (companyId: string, id: string): Promise<void> => {
    await api.delete(`/companies/${companyId}/time-events/${id}`)
  },
}
