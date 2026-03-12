import api from './api'

export interface Employee {
  id: string
  companyId: string
  name: string
  cpf: string
  role?: string
  hireDate: string
  workScheduleId?: string
  status: string
}

export interface EmployeeRequest {
  name: string
  cpf: string
  role?: string
  hireDate: string
  workScheduleId?: string
}

export const employeeService = {
  getAll: async (companyId: string): Promise<Employee[]> => {
    const response = await api.get(`/companies/${companyId}/employees`)
    return response.data
  },

  getById: async (companyId: string, id: string): Promise<Employee> => {
    const response = await api.get(`/companies/${companyId}/employees/${id}`)
    return response.data
  },

  create: async (companyId: string, data: EmployeeRequest): Promise<Employee> => {
    const response = await api.post(`/companies/${companyId}/employees`, data)
    return response.data
  },

  update: async (companyId: string, id: string, data: EmployeeRequest): Promise<Employee> => {
    const response = await api.put(`/companies/${companyId}/employees/${id}`, data)
    return response.data
  },

  delete: async (companyId: string, id: string): Promise<void> => {
    await api.delete(`/companies/${companyId}/employees/${id}`)
  },
}
