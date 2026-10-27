import axios, { AxiosError } from 'axios'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL: apiBaseUrl,
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
    // Sem token ou token inválido: Spring costuma responder 403 em rotas protegidas (anônimo)
    const status = error.response?.status
    if (status === 401 || status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('companies')
      localStorage.removeItem('selectedCompanyId')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
