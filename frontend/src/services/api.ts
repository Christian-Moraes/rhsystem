import axios, { AxiosError } from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Interceptor para adicionar token nas requisições
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  const companyId = localStorage.getItem('selectedCompanyId')
  
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  
  if (companyId) {
    config.headers['X-Company-Id'] = companyId
  }
  
  return config
})

// Interceptor para tratamento de erros
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('selectedCompanyId')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
