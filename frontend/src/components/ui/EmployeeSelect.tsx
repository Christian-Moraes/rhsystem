import { useState, useMemo, useEffect, useRef } from 'react'
import { SearchIcon, ChevronDownIcon } from '../icons'

export interface EmployeeOption {
  id: string
  name: string
  cpf?: string
  role?: string
}

interface EmployeeSelectProps {
  employees: EmployeeOption[]
  value: string
  onChange: (employeeId: string) => void
  disabled?: boolean
  label?: string
  placeholder?: string
  isLoading?: boolean
}

export default function EmployeeSelect({
  employees,
  value,
  onChange,
  disabled = false,
  label = 'Funcionário',
  placeholder = 'Selecione o funcionário',
  isLoading = false,
}: EmployeeSelectProps) {
  const [searchTerm, setSearchTerm] = useState('')
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const filteredEmployees = useMemo(() => {
    if (!searchTerm) return employees
    const term = searchTerm.toLowerCase()
    return employees.filter(
      (e) =>
        e.name.toLowerCase().includes(term) ||
        (e.cpf && e.cpf.toLowerCase().includes(term)) ||
        (e.role && e.role.toLowerCase().includes(term))
    )
  }, [employees, searchTerm])

  const selectedEmployee = employees.find((e) => e.id === value)

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
        setSearchTerm('')
      }
    }

    if (isOpen && !disabled) {
      document.addEventListener('mousedown', handleClickOutside)
      setTimeout(() => inputRef.current?.focus(), 100)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen, disabled])

  return (
    <div className="w-full" ref={dropdownRef}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          {label}
        </label>
      )}
      <div className="relative">
        <button
          type="button"
          onClick={() => !disabled && setIsOpen(!isOpen)}
          disabled={disabled}
          className={`
            w-full px-3 py-2.5 text-left border rounded-lg bg-white
            focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
            transition-all duration-200
            disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-500
            ${!selectedEmployee ? 'text-gray-500' : 'text-gray-900'}
            ${isOpen ? 'ring-2 ring-blue-500 border-blue-500' : 'border-gray-300'}
          `}
        >
          {selectedEmployee ? (
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3 min-w-0 flex-1">
                <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 text-blue-700 font-semibold text-sm">
                  {selectedEmployee.name.charAt(0).toUpperCase()}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-medium truncate">{selectedEmployee.name}</p>
                  <p className="text-xs text-gray-600 mt-0.5">
                    <span className="text-gray-500 font-medium">CPF:</span>{' '}
                    <span className="text-gray-700 font-mono tracking-wide break-all">{selectedEmployee.cpf || '—'}</span>
                    {selectedEmployee.role && (
                      <>
                        <span className="text-gray-400 mx-1">·</span>
                        <span className="text-gray-600">{selectedEmployee.role}</span>
                      </>
                    )}
                  </p>
                </div>
              </div>
              <ChevronDownIcon className={`w-5 h-5 text-gray-400 transition-transform flex-shrink-0 ml-2 ${isOpen ? 'rotate-180' : ''}`} />
            </div>
          ) : (
            <div className="flex items-center justify-between">
              <span className="text-gray-500">{placeholder}</span>
              <ChevronDownIcon className={`w-5 h-5 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </div>
          )}
        </button>

        {isOpen && !disabled && (
          <div className="absolute z-[100] w-full mt-1 rounded-lg shadow-xl max-h-80 overflow-hidden animate-fade-in bg-white border border-gray-200 ring-2 ring-gray-200/50">
            <div className="p-3 border-b border-gray-200 bg-gray-100">
              <div className="relative">
                <SearchIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  ref={inputRef}
                  type="text"
                  placeholder="Buscar por nome, CPF ou cargo..."
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
              ) : filteredEmployees.length === 0 ? (
                <div className="p-6 text-center">
                  <p className="text-sm text-gray-500">
                    {searchTerm ? 'Nenhum funcionário encontrado' : 'Nenhum funcionário cadastrado'}
                  </p>
                </div>
              ) : (
                filteredEmployees.map((employee) => (
                  <button
                    key={employee.id}
                    type="button"
                    onClick={() => {
                      onChange(employee.id)
                      setIsOpen(false)
                      setSearchTerm('')
                    }}
                    className={`
                      w-full px-4 py-3 text-left hover:bg-blue-50 transition-colors border-b border-gray-100 last:border-0
                      ${value === employee.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''}
                    `}
                  >
                    <div className="flex items-start space-x-3">
                      <div className={`flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full font-semibold text-sm ${value === employee.id ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600'}`}>
                        {employee.name.charAt(0).toUpperCase()}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-gray-900 truncate">{employee.name}</p>
                        <p className="text-sm text-gray-500 mt-0.5">
                          {[employee.role, employee.cpf].filter(Boolean).join(' · ') || '—'}
                        </p>
                      </div>
                      {value === employee.id && (
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
    </div>
  )
}
