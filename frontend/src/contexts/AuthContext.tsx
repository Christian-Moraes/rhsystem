import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { authService } from '../services/authService'

interface User {
  id: string
  name: string
  email: string
}

interface Company {
  id: string
  name: string
  role: string
}

interface AuthContextType {
  user: User | null
  token: string | null
  companies: Company[]
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  isAuthenticated: boolean
  isLoading: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [companies, setCompanies] = useState<Company[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    const storedCompanies = localStorage.getItem('companies')

    if (storedToken && storedUser) {
      try {
        setToken(storedToken)
        setUser(JSON.parse(storedUser))
        if (storedCompanies) {
          setCompanies(JSON.parse(storedCompanies))
        }
      } catch (error) {
        console.error('Error parsing stored auth data:', error)
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        localStorage.removeItem('companies')
      }
    }
    setIsLoading(false)
  }, [])

  const login = async (email: string, password: string) => {
    const response = await authService.login(email, password)
    setToken(response.accessToken)
    setUser(response.user)
    setCompanies(response.companies)
    
    localStorage.setItem('token', response.accessToken)
    localStorage.setItem('user', JSON.stringify(response.user))
    localStorage.setItem('companies', JSON.stringify(response.companies))
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    setCompanies([])
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('companies')
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        companies,
        login,
        logout,
        isAuthenticated: !!token,
        isLoading,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
