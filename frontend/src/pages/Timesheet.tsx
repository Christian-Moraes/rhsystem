import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { employeeService } from '../services/employeeService'
import { timesheetService } from '../services/timesheetService'
import { timeEventService, TimeEventRequest } from '../services/timeEventService'
import { timeImportService, TimeImportResponse } from '../services/timeImportService'
import { useCompany } from '../contexts/CompanyContext'
import { useToast } from '../contexts/ToastContext'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Badge from '../components/ui/Badge'
import { ClockIcon, UploadIcon, CheckCircleIcon } from '../components/icons'

interface DayEntry {
  dateStr: string
  punches: Array<{ type: 'ENTRY' | 'EXIT'; time: string }>
}

export default function TimesheetPage() {
  const { selectedCompany, companies, setCompany } = useCompany()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>('')
  const [month, setMonth] = useState(new Date().getMonth() + 1)
  const [year, setYear] = useState(new Date().getFullYear())
  const [gradeData, setGradeData] = useState<Record<string, DayEntry>>({})
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  const [importFiles, setImportFiles] = useState<File[]>([])
  const [importDragActive, setImportDragActive] = useState(false)

  const { data: employees = [] } = useQuery({
    queryKey: ['employees', selectedCompany?.id],
    queryFn: () => employeeService.getAll(selectedCompany?.id || ''),
    enabled: !!selectedCompany?.id,
  })

  const { data: timesheet, isLoading: _timesheetLoading, error: _timesheetError } = useQuery({
    queryKey: ['timesheet', selectedCompany?.id, selectedEmployeeId, month, year],
    queryFn: () =>
      timesheetService.get(selectedCompany?.id || '', selectedEmployeeId, month, year),
    enabled: !!selectedCompany?.id && !!selectedEmployeeId,
    retry: false,
  })

  const { data: existingEvents = [], isLoading: isEventsLoading } = useQuery({
    queryKey: ['time-events-month', selectedCompany?.id, selectedEmployeeId, month, year],
    queryFn: () =>
      timeEventService.getByEmployeeAndMonth(
        selectedCompany!.id,
        selectedEmployeeId,
        month,
        year
      ),
    enabled: !!selectedCompany?.id && !!selectedEmployeeId,
  })

  // Inicializa grade a partir dos eventos (e preserva dados já preenchidos na grade)
  useEffect(() => {
    if (!selectedEmployeeId || !selectedCompany?.id) {
      setGradeData({})
      setHasUnsavedChanges(false)
      return
    }

    const n = new Date(year, month, 0).getDate()
    const newGrade: Record<string, DayEntry> = {}
    const byDay: Record<string, { type: string; time: string }[]> = {}

    for (const ev of existingEvents) {
      const d = new Date(ev.eventTime)
      if (d.getMonth() + 1 !== month || d.getFullYear() !== year) continue
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      if (!byDay[dateStr]) byDay[dateStr] = []
      byDay[dateStr].push({
        type: ev.eventType,
        time: d.toTimeString().slice(0, 5),
      })
    }

    setGradeData((prev) => {
      for (let day = 1; day <= n; day++) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
        const date = new Date(year, month - 1, day)
        if (date.getMonth() + 1 !== month || date.getFullYear() !== year) continue

        const dayEvents = (byDay[dateStr] || []).sort((a, b) => a.time.localeCompare(b.time))
        const punchesFromApi =
          dayEvents.length > 0
            ? dayEvents.map((e) => ({ type: e.type as 'ENTRY' | 'EXIT', time: e.time }))
            : [
                { type: 'ENTRY' as const, time: '' },
                { type: 'EXIT' as const, time: '' },
                { type: 'ENTRY' as const, time: '' },
                { type: 'EXIT' as const, time: '' },
              ]

        const existing = prev[dateStr]
        const hasExistingPunches = existing?.punches?.some((p) => p.time)
        newGrade[dateStr] = {
          dateStr,
          punches: hasExistingPunches ? existing!.punches : punchesFromApi,
        }
      }
      return { ...newGrade }
    })
    setHasUnsavedChanges(false)
  }, [existingEvents, selectedEmployeeId, month, year, selectedCompany?.id])

  // Calcula estatísticas em tempo real da grade
  const gradeStats = useMemo(() => {
    let totalDays = 0
    let filledDays = 0
    let totalPunches = 0

    for (const entry of Object.values(gradeData)) {
      totalDays++
      const hasData = entry.punches.some((p) => p.time)
      if (hasData) filledDays++
      totalPunches += entry.punches.filter((p) => p.time).length
    }

    return { totalDays, filledDays, totalPunches }
  }, [gradeData])

  const gradeRows = useMemo(() => {
    const n = new Date(year, month, 0).getDate()
    const wd = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']
    const rows: { key: string; label: string; punches: Array<{ type: 'ENTRY' | 'EXIT'; time: string }> }[] = []

    let maxPunches = 4
    for (const entry of Object.values(gradeData)) {
      if (entry.punches.length > maxPunches) maxPunches = entry.punches.length
    }

    for (let day = 1; day <= n; day++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
      const date = new Date(year, month - 1, day)
      if (date.getMonth() + 1 !== month || date.getFullYear() !== year) continue

      const entry = gradeData[dateStr] || { dateStr, punches: [] }
      const punches = entry.punches.length >= 4
        ? entry.punches
        : [
            ...entry.punches,
            ...Array(Math.max(4 - entry.punches.length, 0)).fill(null).map((_, i) => ({
              type: (entry.punches.length + i) % 2 === 0 ? 'ENTRY' as const : 'EXIT' as const,
              time: '',
            })),
          ]

      rows.push({
        key: dateStr,
        label: `${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')} ${wd[date.getDay()]}`,
        punches,
      })
    }
    return rows
  }, [gradeData, year, month])

  const updateGradeCell = (dateStr: string, index: number, value: string) => {
    setGradeData((prev) => {
      const entry = prev[dateStr] || { dateStr, punches: [] }
      const newPunches = [...entry.punches]
      if (newPunches[index]) {
        newPunches[index] = { ...newPunches[index], time: value }
      } else {
        const type = index % 2 === 0 ? 'ENTRY' : 'EXIT'
        newPunches[index] = { type, time: value }
      }
      return {
        ...prev,
        [dateStr]: { dateStr, punches: newPunches },
      }
    })
    setHasUnsavedChanges(true)
  }

  const addPunchPair = (dateStr: string) => {
    setGradeData((prev) => {
      const entry = prev[dateStr] || { dateStr, punches: [] }
      return {
        ...prev,
        [dateStr]: {
          dateStr,
          punches: [
            ...entry.punches,
            { type: 'ENTRY' as const, time: '' },
            { type: 'EXIT' as const, time: '' },
          ],
        },
      }
    })
    setHasUnsavedChanges(true)
  }

  const removePunchPair = (dateStr: string) => {
    setGradeData((prev) => {
      const entry = prev[dateStr] || { dateStr, punches: [] }
      if (entry.punches.length <= 4) return prev
      return {
        ...prev,
        [dateStr]: { dateStr, punches: entry.punches.slice(0, -2) },
      }
    })
    setHasUnsavedChanges(true)
  }

  const saveGradeMutation = useMutation({
    mutationFn: async () => {
      if (!selectedCompany?.id || !selectedEmployeeId) return

      const existingEvents = await timeEventService.getByEmployeeAndMonth(
        selectedCompany.id,
        selectedEmployeeId,
        month,
        year
      )
      for (const ev of existingEvents) {
        await timeEventService.delete(selectedCompany.id, ev.id)
      }

      const eventsToCreate: TimeEventRequest[] = []
      for (const [dateStr, entry] of Object.entries(gradeData)) {
        const [y, m, d] = dateStr.split('-').map(Number)
        const baseDate = new Date(y, m - 1, d)

        for (const punch of entry.punches) {
          if (!punch.time) continue
          const [h, min] = punch.time.split(':').map(Number)
          const eventTime = new Date(baseDate)
          eventTime.setHours(h, min, 0, 0)
          eventsToCreate.push({
            employeeId: selectedEmployeeId,
            eventType: punch.type,
            eventTime: eventTime.toISOString(),
          })
        }
      }

      for (const req of eventsToCreate) {
        await timeEventService.create(selectedCompany.id, req)
      }
    },
    onSuccess: () => {
      setHasUnsavedChanges(false)
      queryClient.invalidateQueries({ queryKey: ['time-events-month'] })
      showToast('Grade salva com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao salvar grade', 'error')
    },
  })

  const importMutation = useMutation({
    mutationFn: ({ file, fileType }: { file: File; fileType?: string }) =>
      timeImportService.importFile(
        selectedCompany?.id || '',
        selectedEmployeeId,
        file,
        fileType,
        month,
        year
      ),
    onSuccess: (data: TimeImportResponse) => {
      if (data.gradeByDate && Object.keys(data.gradeByDate).length > 0) {
        setGradeData((prev) => {
          const next = { ...prev }
          for (const [dateStr, times] of Object.entries(data.gradeByDate!)) {
            const punches = times.map((time, idx) => ({
              type: (idx % 2 === 0 ? 'ENTRY' : 'EXIT') as 'ENTRY' | 'EXIT',
              time,
            }))
            next[dateStr] = { dateStr, punches }
          }
          return next
        })
        setHasUnsavedChanges(true)
      }
      if (data.events?.length) {
        queryClient.setQueryData(
          ['time-events-month', selectedCompany?.id, selectedEmployeeId, month, year],
          data.events
        )
      }
      queryClient.invalidateQueries({ queryKey: ['time-events-month'] })
      queryClient.invalidateQueries({ queryKey: ['timesheet'] })
      const count = data.gradeByDate ? Object.keys(data.gradeByDate).length : 0
      showToast(
        data.message || (count > 0
          ? `Grade do mês atualizada: ${count} dia(s) com ponto. Importe outra foto (ex.: 2ª quinzena) para mesclar na mesma grade.`
          : 'Importação realizada. Revise a grade.'),
        'success'
      )
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao importar arquivo', 'error')
    },
  })

  const seedMutation = useMutation({
    mutationFn: () =>
      timeImportService.seedDemoEvents(
        selectedCompany?.id || '',
        selectedEmployeeId,
        month,
        year
      ),
    onSuccess: (data: TimeImportResponse) => {
      if (data.gradeByDate && Object.keys(data.gradeByDate).length > 0) {
        setGradeData((prev) => {
          const next = { ...prev }
          for (const [dateStr, times] of Object.entries(data.gradeByDate!)) {
            const punches = times.map((time, idx) => ({
              type: (idx % 2 === 0 ? 'ENTRY' : 'EXIT') as 'ENTRY' | 'EXIT',
              time,
            }))
            next[dateStr] = { dateStr, punches }
          }
          return next
        })
        setHasUnsavedChanges(false)
      }
      if (data.events?.length) {
        queryClient.setQueryData(
          ['time-events-month', selectedCompany?.id, selectedEmployeeId, month, year],
          data.events
        )
      }
      queryClient.invalidateQueries({ queryKey: ['time-events-month'] })
      queryClient.invalidateQueries({ queryKey: ['timesheet'] })
      showToast('Exemplo criado no servidor (GET). Atualize a página se a grade não preencher.', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao criar exemplo via GET', 'error')
    },
  })

  const closeMutation = useMutation({
    mutationFn: () =>
      timesheetService.close(selectedCompany?.id || '', selectedEmployeeId, month, year),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['timesheet'] })
      showToast('Mês fechado com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao fechar mês', 'error')
    },
  })

  const formatMinutes = (minutes: number) => {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return `${hours}h ${mins.toString().padStart(2, '0')}m`
  }

  const formatHours = (minutes: number) => {
    return (minutes / 60).toFixed(2).replace('.', ',')
  }

  const selectedEmployee = employees.find((e) => e.id === selectedEmployeeId)
  const monthName = new Date(2000, month - 1).toLocaleString('pt-BR', { month: 'long' })

  if (!selectedCompany) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Fechamento de Ponto</h1>
          <p className="mt-1 text-sm text-gray-500">
            Consolidação por funcionário e mês — manual, CSV ou IA. Não é batida em tempo real.
          </p>
        </div>
        <Card>
          <div className="text-center py-8">
            {companies.length === 0 ? (
              <>
                <p className="text-gray-600 mb-4">Nenhuma empresa cadastrada.</p>
                <Link to="/companies">
                  <Button>Cadastrar Empresa</Button>
                </Link>
              </>
            ) : (
              <>
                <p className="text-gray-600 mb-4">Selecione uma empresa para continuar.</p>
                <div className="mx-auto max-w-xs">
                  <label className="block text-sm font-medium text-gray-700 mb-2">Empresa</label>
                  <select
                    onChange={(e) => {
                      const company = companies.find((c) => c.id === e.target.value)
                      if (company) setCompany(company)
                    }}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Selecione...</option>
                    {companies.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="mt-4">
                  <Link to="/companies">
                    <Button variant="outline">Gerenciar Empresas</Button>
                  </Link>
                </div>
              </>
            )}
          </div>
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Fechamento de Ponto</h1>
          <p className="mt-1 text-sm text-gray-500">
            Consolidação por funcionário e mês — manual, CSV ou IA. Não é batida em tempo real.
          </p>
        </div>
        {selectedEmployeeId && (
          <div className="flex gap-2">
            {hasUnsavedChanges && (
              <Button
                type="button"
                onClick={() => saveGradeMutation.mutate()}
                disabled={saveGradeMutation.isPending}
                isLoading={saveGradeMutation.isPending}
                variant="outline"
              >
                Salvar Grade
              </Button>
            )}
            <Link to="/time-import">
              <Button type="button" variant="outline" icon={<UploadIcon className="w-4 h-4" />}>
                Importar
              </Button>
            </Link>
          </div>
        )}
      </div>

      {/* Seleção de Funcionário e Período */}
      <Card
        title="Seleção"
        description="Escolha o funcionário e o período para fechamento"
      >
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Funcionário</label>
            <select
              value={selectedEmployeeId}
              onChange={(e) => setSelectedEmployeeId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
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
            <label className="block text-sm font-medium text-gray-700 mb-2">Mês</label>
            <select
              value={month}
              onChange={(e) => setMonth(parseInt(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <option key={m} value={m}>
                  {new Date(2000, m - 1).toLocaleString('pt-BR', { month: 'long' })}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Ano</label>
            <input
              type="number"
              value={year}
              onChange={(e) => setYear(parseInt(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="flex items-end">
            <Button
              onClick={() => closeMutation.mutate()}
              disabled={!selectedEmployeeId || closeMutation.isPending || hasUnsavedChanges}
              isLoading={closeMutation.isPending}
              className="w-full"
              icon={<CheckCircleIcon className="w-4 h-4" />}
            >
              Fechar Mês
            </Button>
          </div>
        </div>
      </Card>

      {selectedEmployeeId && (
        <>
          {/* Importar na mesma tela */}
          <Card
            title="Importar imagem/CSV"
            description="Envie uma folha de ponto (imagem ou CSV) para preencher a grade pela IA"
          >
            <div className="space-y-4">
              <div
                onDragEnter={(e) => { e.preventDefault(); e.stopPropagation(); setImportDragActive(true) }}
                onDragLeave={(e) => { e.preventDefault(); setImportDragActive(false) }}
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault()
                  setImportDragActive(false)
                  if (e.dataTransfer.files?.length) {
                    setImportFiles((prev) => [...prev, ...Array.from(e.dataTransfer.files!)])
                  }
                }}
                className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
                  importDragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300'
                } ${importFiles.length ? 'border-green-500 bg-green-50' : ''}`}
              >
                {importFiles.length > 0 ? (
                  <div className="space-y-3">
                    <p className="text-sm font-medium text-gray-700">
                      {importFiles.length} arquivo(s) — 1ª e 2ª quinzena na mesma grade
                    </p>
                    <ul className="text-left space-y-1 max-h-32 overflow-y-auto">
                      {importFiles.map((file, idx) => (
                        <li key={`${file.name}-${idx}`} className="flex items-center justify-between gap-2 text-sm">
                          <span className="truncate text-gray-900">{file.name}</span>
                          <span className="text-gray-500 shrink-0">{(file.size / 1024).toFixed(1)} KB</span>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:text-red-700 shrink-0"
                            onClick={() => setImportFiles((prev) => prev.filter((_, i) => i !== idx))}
                          >
                            Remover
                          </Button>
                        </li>
                      ))}
                    </ul>
                    <div className="flex flex-wrap gap-2 justify-center pt-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => setImportFiles([])}
                      >
                        Limpar tudo
                      </Button>
                      <Button
                        type="button"
                        onClick={async () => {
                          const count = importFiles.length
                          for (let i = 0; i < importFiles.length; i++) {
                            const file = importFiles[i]
                            const ext = file.name.split('.').pop()?.toUpperCase()
                            const fileType = ['JPG', 'JPEG', 'PNG'].includes(ext || '') ? 'IMAGE' : ext === 'PDF' ? 'PDF' : ext === 'CSV' ? 'CSV' : undefined
                            await importMutation.mutateAsync({ file, fileType })
                          }
                          setImportFiles([])
                          if (count > 1) {
                            showToast(`${count} foto(s) importada(s) e mescladas na mesma grade. Revise e salve.`, 'success')
                          }
                        }}
                        disabled={importMutation.isPending}
                        isLoading={importMutation.isPending}
                        icon={<UploadIcon className="w-4 h-4" />}
                      >
                        Importar {importFiles.length > 1 ? `todos (${importFiles.length})` : ''}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <>
                    <UploadIcon className="w-10 h-10 mx-auto text-gray-400" />
                    <p className="mt-2 text-sm text-gray-600">
                      Arraste uma ou mais fotos (ex.: 1ª e 2ª quinzena) ou{' '}
                      <label className="cursor-pointer font-medium text-blue-600 hover:text-blue-700">
                        clique para selecionar
                        <input
                          type="file"
                          accept="image/*,.pdf,.csv"
                          multiple
                          className="hidden"
                          onChange={(e) => {
                            const list = e.target.files ? Array.from(e.target.files) : []
                            if (list.length) setImportFiles((prev) => [...prev, ...list])
                          }}
                        />
                      </label>
                    </p>
                    <p className="text-xs text-gray-500 mt-1">PNG, JPG, PDF ou CSV — cada imagem preenche os dias identificados na mesma grade</p>
                  </>
                )}
              </div>
            </div>
          </Card>

          {existingEvents.length === 0 && !isEventsLoading && (
            <Card className="border-amber-200 bg-amber-50">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="font-medium text-amber-900">Nenhum ponto registrado neste mês</p>
                  <p className="text-sm text-amber-800 mt-1">
                    Importe uma folha de ponto (imagem) acima ou use &quot;Criar via GET&quot; para criar eventos de exemplo no servidor.
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => seedMutation.mutate()}
                    disabled={seedMutation.isPending}
                    isLoading={seedMutation.isPending}
                  >
                    Criar via GET
                  </Button>
                </div>
              </div>
            </Card>
          )}

          {/* Estatísticas Rápidas */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">Dias no Mês</p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">{gradeStats.totalDays}</p>
                </div>
                <div className="rounded-full bg-blue-100 p-3">
                  <ClockIcon className="w-6 h-6 text-blue-600" />
                </div>
              </div>
            </Card>
            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">Dias Preenchidos</p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">{gradeStats.filledDays}</p>
                </div>
                <div className="rounded-full bg-green-100 p-3">
                  <CheckCircleIcon className="w-6 h-6 text-green-600" />
                </div>
              </div>
            </Card>
            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">Total de Batidas</p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">{gradeStats.totalPunches}</p>
                </div>
                <div className="rounded-full bg-purple-100 p-3">
                  <ClockIcon className="w-6 h-6 text-purple-600" />
                </div>
              </div>
            </Card>
            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">Status</p>
                  <div className="mt-1">
                    {hasUnsavedChanges ? (
                      <Badge variant="warning">Alterações não salvas</Badge>
                    ) : timesheet?.closedAt ? (
                      <Badge variant="success">Fechado</Badge>
                    ) : (
                      <Badge variant="info">Em edição</Badge>
                    )}
                  </div>
                </div>
                <div className="rounded-full bg-gray-100 p-3">
                  <CheckCircleIcon className="w-6 h-6 text-gray-600" />
                </div>
              </div>
            </Card>
          </div>

          {/* Grade de Ponto */}
          <Card
            title={`Grade de Ponto — ${selectedEmployee?.name || 'Funcionário'} | ${monthName}/${year}`}
            description="Edite diretamente na grade. Mínimo 4 campos (E1/S1/E2/S2), pode adicionar mais batidas."
          >
            <div className="max-h-[500px] overflow-y-auto rounded-lg border border-gray-200">
              <table className="w-full border-collapse text-sm">
                <thead className="sticky top-0 bg-gray-50 z-10">
                  <tr className="border-b">
                    <th className="px-4 py-3 text-left font-semibold text-gray-700 w-28">Dia</th>
                    <th className="px-4 py-3 text-left font-semibold text-gray-700">
                      Batidas (só o dia clicado expande)
                    </th>
                    <th className="px-3 py-3 text-center font-semibold text-gray-700 text-xs w-24">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {gradeRows.map((row) => {
                    const dateStr = row.key
                    const punches = [...row.punches]
                    const minLen = 4
                    while (punches.length < minLen) {
                      const idx = punches.length
                      punches.push({
                        type: idx % 2 === 0 ? 'ENTRY' : 'EXIT',
                        time: '',
                      })
                    }
                    return (
                      <tr key={dateStr} className="border-b border-gray-100 hover:bg-blue-50/30 transition-colors align-top">
                        <td className="whitespace-nowrap px-4 py-2.5 text-gray-800 font-medium">{row.label}</td>
                        <td className="px-2 py-2">
                          <div className="flex flex-wrap items-center gap-2">
                            {punches.map((punch, idx) => {
                              const pair = Math.floor(idx / 2) + 1
                              const label = idx % 2 === 0 ? `E${pair}` : `S${pair}`
                              return (
                                <div key={idx} className="flex items-center gap-1">
                                  <span className="text-[10px] text-gray-500 w-6 shrink-0">{label}</span>
                                  <input
                                    type="time"
                                    value={punch.time}
                                    onChange={(e) => updateGradeCell(dateStr, idx, e.target.value)}
                                    className="w-[6.5rem] rounded-md border border-gray-300 bg-white px-2 py-1.5 text-xs focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all"
                                  />
                                </div>
                              )
                            })}
                          </div>
                        </td>
                        <td className="px-2 py-2">
                          <div className="flex gap-1 justify-center">
                            <button
                              type="button"
                              onClick={(e) => { e.stopPropagation(); addPunchPair(dateStr) }}
                              className="rounded-md bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700 hover:bg-blue-100 transition-colors"
                              title="Adicionar mais batidas neste dia"
                            >
                              +
                            </button>
                            {row.punches.length > 4 && (
                              <button
                                type="button"
                                onClick={(e) => { e.stopPropagation(); removePunchPair(dateStr) }}
                                className="rounded-md bg-red-50 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-100 transition-colors"
                                title="Remover último par neste dia"
                              >
                                −
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </Card>

          {/* Preview do Fechamento */}
          {timesheet && (
            <Card
              title="Resumo do Fechamento"
              description={`Fechamento consolidado para ${selectedEmployee?.name || 'funcionário'} em ${monthName}/${year}`}
            >
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-gradient-to-br from-blue-50 to-blue-100 rounded-xl p-5 border border-blue-200">
                  <p className="text-sm font-medium text-blue-700 mb-1">Horas Trabalhadas</p>
                  <p className="text-3xl font-bold text-blue-900">{formatHours(timesheet.workedHours)}</p>
                  <p className="text-xs text-blue-600 mt-1">{formatMinutes(timesheet.workedHours)}</p>
                </div>
                <div className="bg-gradient-to-br from-green-50 to-green-100 rounded-xl p-5 border border-green-200">
                  <p className="text-sm font-medium text-green-700 mb-1">Horas Extras</p>
                  <p className="text-3xl font-bold text-green-900">{formatHours(timesheet.extraHours)}</p>
                  <p className="text-xs text-green-600 mt-1">{formatMinutes(timesheet.extraHours)}</p>
                </div>
                <div className="bg-gradient-to-br from-red-50 to-red-100 rounded-xl p-5 border border-red-200">
                  <p className="text-sm font-medium text-red-700 mb-1">Atrasos</p>
                  <p className="text-3xl font-bold text-red-900">{formatMinutes(timesheet.delayMinutes)}</p>
                </div>
                <div className="bg-gradient-to-br from-orange-50 to-orange-100 rounded-xl p-5 border border-orange-200">
                  <p className="text-sm font-medium text-orange-700 mb-1">Ausências</p>
                  <p className="text-3xl font-bold text-orange-900">{formatMinutes(timesheet.absenceMinutes)}</p>
                </div>
              </div>
              {timesheet.closedAt && (
                <div className="mt-4 pt-4 border-t border-gray-200">
                  <Badge variant="success" size="md">
                    Fechado em {new Date(timesheet.closedAt).toLocaleDateString('pt-BR', {
                      day: '2-digit',
                      month: 'long',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </Badge>
                </div>
              )}
            </Card>
          )}

          {!timesheet && selectedEmployeeId && (
            <Card>
              <div className="text-center py-8">
                <p className="text-gray-600 mb-2">Nenhum fechamento encontrado para este período.</p>
                <p className="text-sm text-gray-500 mb-4">
                  Preencha a grade e clique em "Fechar Mês" para calcular e gerar o fechamento.
                </p>
              </div>
            </Card>
          )}
        </>
      )}
    </div>
  )
}
