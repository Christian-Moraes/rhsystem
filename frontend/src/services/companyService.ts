import api from './api'

export interface Company {
  id: string
  name: string
  cnpj: string
  responsibleName: string
  email: string
  phone: string
  enableOvertime: boolean
  enableTimeBank: boolean
  overtimePercentage: number
  weekendPercentage: number
  createdAt: string
  role: string
}

export interface CompanyRequest {
  name: string
  cnpj: string
  responsibleName: string
  email: string
  phone?: string
  enableOvertime: boolean
  enableTimeBank: boolean
  overtimePercentage: number
  weekendPercentage: number
}

export const companyService = {
  getAll: async (): Promise<Company[]> => {
    const response = await api.get('/companies')
    return response.data
  },

  getById: async (id: string): Promise<Company> => {
    const response = await api.get(`/companies/${id}`)
    return response.data
  },

  create: async (data: CompanyRequest): Promise<Company> => {
    const response = await api.post('/companies', data)
    return response.data
  },

  update: async (id: string, data: CompanyRequest): Promise<Company> => {
    const response = await api.put(`/companies/${id}`, data)
    return response.data
  },
}
