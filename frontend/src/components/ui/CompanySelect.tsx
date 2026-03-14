import { useState, useMemo, useEffect, useRef } from 'react'
import { companyService } from '../../services/companyService'
import { useQuery } from '@tanstack/react-query'
import { SearchIcon, ChevronDownIcon, BuildingIcon } from '../icons'

interface CompanySelectProps {
  value?: string
  onChange: (companyId: string) => void
  required?: boolean
  label?: string
  error?: string
}

export default function CompanySelect({
  value,
  onChange,
  required = false,
  label = 'Empresa',
  error
}: CompanySelectProps) {
  const [searchTerm, setSearchTerm] = useState('')
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const { data: companies = [], isLoading } = useQuery({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
  })

  const filteredCompanies = useMemo(() => {
    if (!searchTerm) return companies
    const term = searchTerm.toLowerCase()
    return companies.filter(
      (c) =>
        c.name.toLowerCase().includes(term) ||
        c.cnpj.toLowerCase().includes(term) ||
        c.email.toLowerCase().includes(term)
    )
  }, [companies, searchTerm])

  const selectedCompany = companies.find((c) => c.id === value)

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
        setSearchTerm('')
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
      setTimeout(() => inputRef.current?.focus(), 100)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen])

  return (
    <div className="w-full" ref={dropdownRef}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
        </label>
      )}
      <div className="relative">
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className={`
            w-full px-3 py-2.5 text-left border rounded-lg bg-white
            focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
            transition-all duration-200
            ${error ? 'border-red-300 focus:ring-red-500' : 'border-gray-300'}
            ${!selectedCompany ? 'text-gray-500' : 'text-gray-900'}
            ${isOpen ? 'ring-2 ring-blue-500 border-blue-500' : ''}
          `}
        >
          {selectedCompany ? (
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3 min-w-0 flex-1">
                <BuildingIcon className="w-5 h-5 text-gray-400 flex-shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="font-medium truncate">{selectedCompany.name}</p>
                  <p className="text-xs text-gray-600 mt-0.5">
                    <span className="text-gray-500 font-medium">CNPJ:</span>{' '}
                    <span className="text-gray-700 font-mono tracking-wide break-all">{selectedCompany.cnpj}</span>
                  </p>
                </div>
              </div>
              <ChevronDownIcon className={`w-5 h-5 text-gray-400 transition-transform flex-shrink-0 ml-2 ${isOpen ? 'rotate-180' : ''}`} />
            </div>
          ) : (
            <div className="flex items-center justify-between">
              <span className="text-gray-500">Selecione uma empresa</span>
              <ChevronDownIcon className={`w-5 h-5 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </div>
          )}
        </button>

        {isOpen && (
          <div className="absolute z-[100] w-full mt-1 rounded-lg shadow-xl max-h-80 overflow-hidden animate-fade-in bg-white border border-gray-200 ring-2 ring-gray-200/50">
            <div className="p-3 border-b border-gray-200 bg-gray-100">
              <div className="relative">
                <SearchIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  ref={inputRef}
                  type="text"
                  placeholder="Buscar por nome, CNPJ ou email..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                />
              </div>
            </div>
            <div className="overflow-y-auto max-h-64 bg-white">
              {isLoading ? (
                <div className="p-6 text-center text-gray-500">
                  <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
                  <p className="mt-2 text-sm">Carregando...</p>
                </div>
              ) : filteredCompanies.length === 0 ? (
                <div className="p-6 text-center">
                  <BuildingIcon className="w-12 h-12 text-gray-300 mx-auto mb-2" />
                  <p className="text-sm text-gray-500">
                    {searchTerm ? 'Nenhuma empresa encontrada' : 'Nenhuma empresa cadastrada'}
                  </p>
                </div>
              ) : (
                filteredCompanies.map((company) => (
                  <button
                    key={company.id}
                    type="button"
                    onClick={() => {
                      onChange(company.id)
                      setIsOpen(false)
                      setSearchTerm('')
                    }}
                    className={`
                      w-full px-4 py-3 text-left hover:bg-blue-50 transition-colors border-b border-gray-100 last:border-0
                      ${value === company.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''}
                    `}
                  >
                    <div className="flex items-start space-x-3">
                      <BuildingIcon className={`w-5 h-5 mt-0.5 flex-shrink-0 ${value === company.id ? 'text-blue-600' : 'text-gray-400'}`} />
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-gray-900 truncate">{company.name}</p>
                        <p className="text-sm text-gray-500 mt-0.5">CNPJ: {company.cnpj}</p>
                        <p className="text-xs text-gray-400 mt-0.5 truncate">{company.email}</p>
                      </div>
                      {value === company.id && (
                        <svg className="w-5 h-5 text-blue-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                      )}
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>
        )}
      </div>
      {error && (
        <p className="mt-1.5 text-sm text-red-600 flex items-center">
          <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
          {error}
        </p>
      )}
    </div>
  )
}
