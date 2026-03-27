import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from './AuthContext'
import { companyService, Company } from '../services/companyService'

interface CompanyContextType {
  company: Company | null
  selectedCompany: Company | null // Alias para compatibilidade
  companies: Company[]
  setCompany: (company: Company | null) => void
}

const CompanyContext = createContext<CompanyContextType | undefined>(undefined)

export function CompanyProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const [company, setCompany] = useState<Company | null>(null)

  // Busca empresas via API (inclui as cadastradas depois do login)
  const { data: apiCompanies = [] } = useQuery({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
    enabled: isAuthenticated,
  })

  useEffect(() => {
    if (apiCompanies.length === 0) {
      setCompany(null)
      return
    }

    const storedCompanyId = localStorage.getItem('selectedCompanyId')
    if (storedCompanyId) {
      const found = apiCompanies.find((c) => c.id === storedCompanyId)
      if (found) {
        setCompany(found)
        return
      }
    }
    // Se não encontrou a empresa salva, seleciona a primeira
    if (apiCompanies.length > 0) {
      setCompany(apiCompanies[0])
    }
  }, [apiCompanies])

  const handleSetCompany = (selectedCompany: Company | null) => {
    setCompany(selectedCompany)
    if (selectedCompany) {
      localStorage.setItem('selectedCompanyId', selectedCompany.id)
    } else {
      localStorage.removeItem('selectedCompanyId')
    }
  }

  return (
    <CompanyContext.Provider
      value={{
        company,
        selectedCompany: company, // Alias para compatibilidade
        companies: apiCompanies,
        setCompany: handleSetCompany,
      }}
    >
      {children}
    </CompanyContext.Provider>
  )
}

export function useCompany() {
  const context = useContext(CompanyContext)
  if (context === undefined) {
    throw new Error('useCompany must be used within a CompanyProvider')
  }
  return context
}
