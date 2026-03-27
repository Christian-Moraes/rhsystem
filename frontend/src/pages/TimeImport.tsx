import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { employeeService } from '../services/employeeService'
import { timeImportService, TimeImportResponse } from '../services/timeImportService'
import { useCompany } from '../contexts/CompanyContext'
import { useToast } from '../contexts/ToastContext'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'

export default function TimeImport() {
  const { selectedCompany } = useCompany()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>('')
  const [file, setFile] = useState<File | null>(null)
  const [importResult, setImportResult] = useState<TimeImportResponse | null>(null)
  const [dragActive, setDragActive] = useState(false)

  const { data: employees = [] } = useQuery({
    queryKey: ['employees', selectedCompany?.id],
    queryFn: () => employeeService.getAll(selectedCompany?.id || ''),
    enabled: !!selectedCompany?.id,
  })

  const importMutation = useMutation({
    mutationFn: ({ file, fileType }: { file: File; fileType?: string }) =>
      timeImportService.importFile(selectedCompany?.id || '', selectedEmployeeId, file, fileType),
    onSuccess: (data) => {
      setImportResult(data)
      queryClient.invalidateQueries({ queryKey: ['timeEvents'] })
      queryClient.invalidateQueries({ queryKey: ['time-events-month'] })
      queryClient.invalidateQueries({ queryKey: ['timesheet'] })
      showToast('Importação realizada com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao importar arquivo', 'error')
    },
  })

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0])
    }
  }

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setFile(e.dataTransfer.files[0])
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (file && selectedEmployeeId) {
      const fileType = file.name.split('.').pop()?.toUpperCase()
      importMutation.mutate({ file, fileType })
    }
  }

  const getFileTypeLabel = (filename: string) => {
    const ext = filename.split('.').pop()?.toLowerCase()
    if (['jpg', 'jpeg', 'png', 'gif'].includes(ext || '')) return 'Imagem'
    if (ext === 'pdf') return 'PDF'
    if (ext === 'csv') return 'CSV'
    return 'Arquivo'
  }

  if (!selectedCompany) {
    return (
      <Card>
        <div className="text-center py-8">
          <p className="text-gray-600 mb-4">Selecione uma empresa primeiro.</p>
          <Link to="/companies">
            <Button>Ir para Empresas</Button>
          </Link>
        </div>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Importar Ponto</h1>
        <p className="text-sm text-gray-500 mt-1">
          Importe registros de ponto através de imagens, PDF ou CSV
        </p>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Funcionário
            </label>
            <select
              value={selectedEmployeeId}
              onChange={(e) => setSelectedEmployeeId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            >
              <option value="">Selecione um funcionário</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Arquivo (Imagem, PDF ou CSV)
            </label>
            <div
              onDragEnter={handleDrag}
              onDragLeave={handleDrag}
              onDragOver={handleDrag}
              onDrop={handleDrop}
              className={`
                border-2 border-dashed rounded-lg p-8 text-center transition-colors
                ${dragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300'}
                ${file ? 'border-green-500 bg-green-50' : ''}
              `}
            >
              {file ? (
                <div className="space-y-2">
                  <p className="text-sm font-medium text-gray-900">{file.name}</p>
                  <p className="text-xs text-gray-500">
                    {getFileTypeLabel(file.name)} • {(file.size / 1024).toFixed(2)} KB
                  </p>
                  <button
                    type="button"
                    onClick={() => setFile(null)}
                    className="text-sm text-red-600 hover:text-red-700"
                  >
                    Remover arquivo
                  </button>
                </div>
              ) : (
                <div>
                  <svg
                    className="mx-auto h-12 w-12 text-gray-400"
                    stroke="currentColor"
                    fill="none"
                    viewBox="0 0 48 48"
                  >
                    <path
                      d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                      strokeWidth={2}
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                  <p className="mt-2 text-sm text-gray-600">
                    <span className="font-medium">Clique para fazer upload</span> ou arraste e solte
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    PNG, JPG, PDF ou CSV até 10MB
                  </p>
                </div>
              )}
              <input
                type="file"
                accept="image/*,.pdf,.csv"
                onChange={handleFileChange}
                className="hidden"
                id="file-upload"
              />
              {!file && (
                <label
                  htmlFor="file-upload"
                  className="mt-4 inline-block cursor-pointer"
                >
                  <Button variant="outline" type="button">
                    Selecionar Arquivo
                  </Button>
                </label>
              )}
            </div>
          </div>

          <Button
            type="submit"
            disabled={!file || !selectedEmployeeId || importMutation.isPending}
            isLoading={importMutation.isPending}
            className="w-full"
          >
            {importMutation.isPending ? 'Processando...' : 'Importar'}
          </Button>
        </form>
      </Card>

      {importResult && (
        <Card title="Resultado da Importação">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="text-center">
              <p className="text-2xl font-bold text-gray-900">{importResult.totalEvents}</p>
              <p className="text-sm text-gray-500">Total</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-green-600">{importResult.confirmedEvents}</p>
              <p className="text-sm text-gray-500">Confirmados</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-yellow-600">{importResult.reviewEvents}</p>
              <p className="text-sm text-gray-500">Revisão</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-red-600">{importResult.uncertainEvents}</p>
              <p className="text-sm text-gray-500">Incerteza</p>
            </div>
          </div>
          {importResult.reviewEvents > 0 && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
              <p className="text-sm text-yellow-800">
                <strong>Atenção:</strong> Alguns eventos precisam de revisão manual. 
                <Link to="/time-events" className="ml-1 text-yellow-900 underline">
                  Ver eventos
                </Link>
              </p>
            </div>
          )}
        </Card>
      )}
    </div>
  )
}
