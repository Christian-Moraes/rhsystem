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

  const clearStaleStorage = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('companies')
    localStorage.removeItem('selectedCompanyId')
    setToken(null)
    setUser(null)
    setCompanies([])
  }

  const hydrateFromStorage = () => {
    const storedToken = localStorage.getItem('token')?.trim() || ''
    const storedUser = localStorage.getItem('user')

    // Evita "logado" na UI sem token (axios só manda Bearer se existir em localStorage)
    if (storedUser && !storedToken) {
      localStorage.removeItem('user')
      localStorage.removeItem('companies')
      localStorage.removeItem('selectedCompanyId')
      setToken(null)
      setUser(null)
      setCompanies([])
      return
    }
    if (storedToken && !storedUser) {
      localStorage.removeItem('token')
      setToken(null)
      setUser(null)
      setCompanies([])
      return
    }

    if (storedToken && storedUser) {
      try {
        setToken(storedToken)
        setUser(JSON.parse(storedUser))
        const storedCompanies = localStorage.getItem('companies')
        if (storedCompanies) {
          setCompanies(JSON.parse(storedCompanies))
        }
      } catch (error) {
        console.error('Error parsing stored auth data:', error)
        clearStaleStorage()
      }
    }
  }

  useEffect(() => {
    hydrateFromStorage()
    setIsLoading(false)
  }, [])

  // Se o token sumir do localStorage (outra aba, extensão, devtools), alinhar o estado
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === 'token' || e.key === 'user') {
        hydrateFromStorage()
      }
    }
    const onFocus = () => {
      const t = localStorage.getItem('token')?.trim()
      if (token && !t) {
        clearStaleStorage()
      }
    }
    window.addEventListener('storage', onStorage)
    window.addEventListener('focus', onFocus)
    return () => {
      window.removeEventListener('storage', onStorage)
      window.removeEventListener('focus', onFocus)
    }
  }, [token])

  const login = async (email: string, password: string) => {
    const response = await authService.login(email, password)
    const access = response.accessToken?.trim()
    if (!access) {
      throw new Error('Login não retornou token. Tente de novo.')
    }
    setToken(access)
    setUser(response.user)
    setCompanies(response.companies)

    localStorage.setItem('token', access)
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
    localStorage.removeItem('selectedCompanyId')
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        companies,
        login,
        logout,
        isAuthenticated: !!(token?.trim() && user),
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
