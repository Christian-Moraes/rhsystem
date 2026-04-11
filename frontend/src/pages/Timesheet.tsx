import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { employeeService } from '../services/employeeService'
import { timesheetService } from '../services/timesheetService'
import { timeEventService, TimeEventRequest } from '../services/timeEventService'
import { timeImportService, TimeImportResponse } from '../services/timeImportService'
import { useCompany } from '../contexts/CompanyContext'
import { useToast } from '../contexts/ToastContext'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import Badge from '../components/ui/Badge'
import CompanySelect from '../components/ui/CompanySelect'
import EmployeeSelect from '../components/ui/EmployeeSelect'
import { ClockIcon, UploadIcon, CheckCircleIcon, DocumentIcon, TrashIcon } from '../components/icons'
import { PunchTimeInput } from '../components/timesheet/PunchTimeInput'

interface DayEntry {
  dateStr: string
  punches: Array<{ type: 'ENTRY' | 'EXIT'; time: string }>
}

/** Ordenação cronológica; `localeCompare` falha com "7:00" vs "12:00". */
function comparePunchTime(a: string, b: string): number {
  if (!a && !b) return 0
  if (!a) return 1
  if (!b) return -1
  const toMin = (t: string) => {
    const m = t.trim().match(/^(\d{1,2}):(\d{2})$/)
    if (!m) return Number.MAX_SAFE_INTEGER
    return Number(m[1]) * 60 + Number(m[2])
  }
  return toMin(a) - toMin(b)
}

export default function TimesheetPage() {
  const [searchParams] = useSearchParams()
  const employeeIdFromUrl = searchParams.get('employeeId') ?? ''
  const { selectedCompany, companies, setCompany } = useCompany()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>(employeeIdFromUrl)
  const [month, setMonth] = useState(new Date().getMonth() + 1)
  const [year, setYear] = useState(new Date().getFullYear())
  const [gradeData, setGradeData] = useState<Record<string, DayEntry>>({})
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  const [importFiles, setImportFiles] = useState<File[]>([])
  const [importDragActive, setImportDragActive] = useState(false)
  const [exporting, setExporting] = useState<'xls' | 'csv' | null>(null)
  const [clearDayTarget, setClearDayTarget] = useState<null | { dateStr: string; dayLabel: string }>(null)

  const { data: employees = [], isLoading: isEmployeesLoading } = useQuery({
    queryKey: ['employees', selectedCompany?.id],
    queryFn: () => employeeService.getAll(selectedCompany?.id || ''),
    enabled: !!selectedCompany?.id,
  })

  useEffect(() => {
    if (employeeIdFromUrl) setSelectedEmployeeId(employeeIdFromUrl)
  }, [employeeIdFromUrl])

  useEffect(() => {
    if (selectedCompany?.id && selectedEmployeeId && employees.length > 0) {
      const belongs = employees.some((e) => e.id === selectedEmployeeId)
      if (!belongs) setSelectedEmployeeId('')
    }
  }, [selectedCompany?.id, selectedEmployeeId, employees])

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

        const dayEvents = (byDay[dateStr] || []).sort((a, b) => comparePunchTime(a.time, b.time))
        // IMPORTANT: o backend pode ter `event_type` invertido por histórico/importação.
        // Para a grade continuar consistente com a UI (E1/S1/E2/S2), determinamos ENTRY/EXIT
        // pela posição (ordem na tela), e não pelo event_type recebido do banco.
        const punchesFromApi =
          dayEvents.length > 0
            ? dayEvents.map((e, idx) => ({
                type: idx % 2 === 0 ? ('ENTRY' as const) : ('EXIT' as const),
                time: e.time,
              }))
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

  const emptyDayTemplate = (): DayEntry['punches'] => [
    { type: 'ENTRY', time: '' },
    { type: 'EXIT', time: '' },
    { type: 'ENTRY', time: '' },
    { type: 'EXIT', time: '' },
  ]

  /** Abre o modal de confirmação antes de limpar o dia. */
  const requestClearDay = (dateStr: string, dayLabel: string) => {
    setClearDayTarget({ dateStr, dayLabel })
  }

  /** Zera o dia na grade (4 batidas vazias). Persiste só após Salvar. */
  const confirmClearDay = () => {
    if (!clearDayTarget) return
    const { dateStr } = clearDayTarget
    setGradeData((prev) => ({
      ...prev,
      [dateStr]: { dateStr, punches: emptyDayTemplate() },
    }))
    setHasUnsavedChanges(true)
    setClearDayTarget(null)
    showToast('Dia limpo na grade. Salve para gravar no servidor.', 'success')
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
        const yStr = String(y).padStart(4, '0')
        const mStr = String(m).padStart(2, '0')
        const dStr = String(d).padStart(2, '0')

        for (let idx = 0; idx < entry.punches.length; idx++) {
          const punch = entry.punches[idx]
          if (!punch.time) continue
          // Aceita H:mm ou HH:mm (normaliza para o backend).
          if (!/^\d{1,2}:\d{2}$/.test(punch.time.trim())) continue
          const [h, min] = punch.time.trim().split(':').map(Number)
          const hStr = String(h).padStart(2, '0')
          const minStr = String(min).padStart(2, '0')
          // Envia sem timezone ("Z") para o backend tratar como LocalDateTime no mesmo relógio local.
          const eventTimeLocal = `${yStr}-${mStr}-${dStr}T${hStr}:${minStr}:00`
          const eventType = idx % 2 === 0 ? 'ENTRY' : 'EXIT'
          eventsToCreate.push({
            employeeId: selectedEmployeeId,
            eventType,
            eventTime: eventTimeLocal,
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

  const monthName = new Date(2000, month - 1).toLocaleString('pt-BR', { month: 'long' })
  const selectedEmployee = employees.find((e) => e.id === selectedEmployeeId)
  const hasContext = !!selectedCompany && !!selectedEmployeeId

  const runClosureExport = async (format: 'xls' | 'csv') => {
    if (!selectedCompany?.id || !selectedEmployeeId) return
    setExporting(format)
    try {
      await timesheetService.downloadClosureExport(
        selectedCompany.id,
        selectedEmployeeId,
        month,
        year,
        format
      )
      showToast('Arquivo gerado no servidor (batidas, totais e regras).', 'success')
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            'Não foi possível gerar a exportação.'
      showToast(msg, 'error')
    } finally {
      setExporting(null)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Fechamento de Ponto</h1>
        <p className="mt-1 text-sm text-gray-500">
          Selecione a empresa e o funcionário para fechar o ponto do mês.
        </p>
      </div>

      {/* Barra de contexto: mesma pesquisa da tela de Funcionários (overflow-visible para o dropdown não ser cortado) */}
      <Card className="border border-gray-200 bg-gray-50/50 overflow-visible">
        <div className="flex flex-col sm:flex-row gap-4 sm:gap-6">
          <div className="flex-1 min-w-0">
            <CompanySelect
              label="Empresa"
              value={selectedCompany?.id ?? ''}
              onChange={(id) => {
                const c = companies.find((x) => x.id === id)
                if (c) setCompany(c)
              }}
            />
          </div>
          <div className="flex-1 min-w-0">
            {selectedCompany && employees.length === 0 && !isEmployeesLoading ? (
              <>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Funcionário</label>
                <div className="flex items-center gap-2 py-2.5 text-gray-500 text-sm">
                  Nenhum funcionário nesta empresa.
                  <Link to="/employees" className="text-blue-600 hover:text-blue-800 font-medium shrink-0">
                    Cadastrar
                  </Link>
                </div>
              </>
            ) : (
              <EmployeeSelect
                label="Funcionário"
                employees={employees}
                value={selectedEmployeeId}
                onChange={setSelectedEmployeeId}
                disabled={!selectedCompany}
                placeholder="Selecione o funcionário"
                isLoading={isEmployeesLoading}
              />
            )}
          </div>
        </div>
      </Card>

      {/* Conteúdo: só quando empresa e funcionário escolhidos */}
      {!hasContext ? (
        <Card className="border border-dashed border-gray-300 bg-gray-50/30">
          <div className="py-12 text-center">
            <ClockIcon className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-600 font-medium">Selecione uma empresa e um funcionário acima para fechar o ponto.</p>
            <p className="text-sm text-gray-500 mt-1">O fechamento (grade, importação e resumo) aparecerá aqui.</p>
          </div>
        </Card>
      ) : (
        <>
          {/* Título do fechamento */}
          <div className="rounded-xl border border-blue-200 bg-blue-50/50 p-4 md:p-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="min-w-0 flex-1">
                <h2 className="text-lg font-bold text-gray-900">Fechando ponto de {selectedEmployee?.name || 'Funcionário'}</h2>
                <p className="text-sm text-gray-600 mt-0.5">
                  {selectedCompany?.name} · {monthName}/{year}
                </p>
              </div>
              <div className="flex flex-col items-stretch sm:items-end gap-2 w-full sm:w-auto shrink-0">
                <p className="text-xs text-gray-600 text-left sm:text-right max-w-[18rem] leading-snug">
                  Excel (.xls) ou CSV gerados no servidor: batidas E/S, totais e regras do cálculo (escala, extras, cadastro da empresa).
                </p>
                <div className="flex flex-wrap gap-2 justify-start sm:justify-end">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={!!exporting}
                    isLoading={exporting === 'xls'}
                    icon={<DocumentIcon className="w-4 h-4" />}
                    onClick={() => runClosureExport('xls')}
                  >
                    Excel
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={!!exporting}
                    isLoading={exporting === 'csv'}
                    onClick={() => runClosureExport('csv')}
                  >
                    CSV
                  </Button>
                </div>
              </div>
            </div>
          </div>

          {/* Período e Fechar Mês */}
          <Card>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
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
              <div className="flex flex-col justify-end gap-1">
                <Button
                  onClick={() => closeMutation.mutate()}
                  disabled={closeMutation.isPending}
                  isLoading={closeMutation.isPending}
                  className="w-full"
                  icon={<CheckCircleIcon className="w-4 h-4" />}
                >
                  Fechar Mês
                </Button>
                {hasUnsavedChanges && (
                  <p className="text-[11px] leading-snug text-amber-800">
                    Grade com edições locais: o fechamento usa batidas <strong>já salvas</strong> no servidor. Salve a grade antes se quiser incluir essas mudanças.
                  </p>
                )}
              </div>
            </div>
          </Card>

          {/* Importar */}
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
            description="Enter: próximo campo · Shift+Enter: anterior · ↑↓: ±5 min (Shift: ±15 min)."
            actions={
              hasUnsavedChanges ? (
                <Button
                  type="button"
                  onClick={() => saveGradeMutation.mutate()}
                  disabled={saveGradeMutation.isPending}
                  isLoading={saveGradeMutation.isPending}
                  variant="outline"
                  size="sm"
                >
                  Salvar Grade
                </Button>
              ) : null
            }
            className="border-slate-200/90 shadow-sm"
          >
            <div
              className="-mx-6 -mb-6 -mt-1 border-t border-slate-100 bg-gradient-to-b from-slate-50/80 to-white"
              data-timesheet-grade
            >
              <div className="max-h-[min(70vh,560px)] overflow-y-auto overscroll-contain">
                <table className="w-full border-collapse text-[13px]">
                  <thead className="sticky top-0 z-10 border-b border-slate-200 bg-slate-100/95 backdrop-blur-sm">
                    <tr>
                      <th className="w-[4.5rem] px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                        Dia
                      </th>
                      <th className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                        Turnos (E = entrada · S = saída)
                      </th>
                      <th className="w-24 px-2 py-2.5 text-right text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                        Ações
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {gradeRows.map((row, rowIndex) => {
                      const dateStr = row.key
                      const labelBits = row.label.split(' ')
                      const datePart = labelBits[0] ?? row.label
                      const weekPart = labelBits.slice(1).join(' ')
                      const punches = [...row.punches]
                      const minLen = 4
                      while (punches.length < minLen) {
                        const idx = punches.length
                        punches.push({
                          type: idx % 2 === 0 ? 'ENTRY' : 'EXIT',
                          time: '',
                        })
                      }
                      const hasAnyTime = row.punches.some((p) => p.time)
                      const zebra = rowIndex % 2 === 0 ? 'bg-white' : 'bg-slate-50/50'
                      return (
                        <tr
                          key={dateStr}
                          className={`${zebra} transition-colors hover:bg-indigo-50/40 align-middle`}
                        >
                          <td className="whitespace-nowrap px-3 py-2 align-middle">
                            <div className="flex flex-col leading-tight">
                              <span className="font-semibold tabular-nums text-slate-900">{datePart}</span>
                              {weekPart ? (
                                <span className="text-[11px] font-medium text-slate-500">{weekPart}</span>
                              ) : null}
                            </div>
                          </td>
                          <td className="px-2 py-2 align-middle">
                            <div className="flex flex-wrap items-center gap-x-2 gap-y-1.5">
                              {Array.from({ length: Math.ceil(punches.length / 2) }, (_, pairIdx) => {
                                const idx = pairIdx * 2
                                const ePunch = punches[idx]
                                const sPunch = punches[idx + 1]
                                const pairNum = pairIdx + 1
                                return (
                                  <div
                                    key={pairIdx}
                                    className="inline-flex items-center gap-1 rounded-lg border border-slate-200/90 bg-white px-1.5 py-1 shadow-[0_1px_2px_rgba(15,23,42,0.04)]"
                                  >
                                    <span className="w-4 shrink-0 text-center text-[10px] font-bold text-indigo-600">
                                      E{pairNum}
                                    </span>
                                    <PunchTimeInput
                                      compact
                                      id={`punch-${dateStr}-e${pairNum}`}
                                      aria-label={`${row.label} entrada ${pairNum}`}
                                      value={ePunch?.time ?? ''}
                                      onChange={(v) => updateGradeCell(dateStr, idx, v)}
                                    />
                                    <span
                                      className="select-none text-slate-300"
                                      aria-hidden
                                    >
                                      ·
                                    </span>
                                    <span className="w-4 shrink-0 text-center text-[10px] font-bold text-amber-600">
                                      S{pairNum}
                                    </span>
                                    <PunchTimeInput
                                      compact
                                      id={`punch-${dateStr}-s${pairNum}`}
                                      aria-label={`${row.label} saída ${pairNum}`}
                                      value={sPunch?.time ?? ''}
                                      onChange={(v) => updateGradeCell(dateStr, idx + 1, v)}
                                    />
                                  </div>
                                )
                              })}
                            </div>
                          </td>
                          <td className="px-2 py-2 align-middle">
                            <div className="flex items-center justify-end gap-0.5">
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation()
                                  addPunchPair(dateStr)
                                }}
                                className="flex h-8 w-8 items-center justify-center rounded-md text-base font-medium text-indigo-600 transition-colors hover:bg-indigo-100 hover:text-indigo-800"
                                title="Adicionar turno neste dia"
                                aria-label="Adicionar turno"
                              >
                                +
                              </button>
                              {row.punches.length > 4 && (
                                <button
                                  type="button"
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    removePunchPair(dateStr)
                                  }}
                                  className="flex h-8 w-8 items-center justify-center rounded-md text-base font-medium text-slate-500 transition-colors hover:bg-slate-200 hover:text-slate-800"
                                  title="Remover último turno"
                                  aria-label="Remover último turno"
                                >
                                  −
                                </button>
                              )}
                              <button
                                type="button"
                                disabled={!hasAnyTime}
                                onClick={(e) => {
                                  e.stopPropagation()
                                  requestClearDay(dateStr, row.label)
                                }}
                                className="flex h-8 w-8 items-center justify-center rounded-md text-slate-400 transition-colors hover:bg-red-50 hover:text-red-600 disabled:pointer-events-none disabled:opacity-30"
                                title="Limpar todos os horários deste dia"
                                aria-label="Limpar dia"
                              >
                                <TrashIcon className="w-4 h-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </Card>

          {/* Preview do Fechamento */}
          {timesheet && (
            <Card
              title="Resumo do Fechamento"
              description={`Fechamento consolidado para ${selectedEmployee?.name || 'funcionário'} em ${monthName}/${year}`}
              actions={
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={!!exporting}
                    isLoading={exporting === 'xls'}
                    icon={<DocumentIcon className="w-4 h-4" />}
                    onClick={() => runClosureExport('xls')}
                    title="Arquivo .xls gerado no backend (batidas, totais, regras)"
                  >
                    Exportar Excel
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    disabled={!!exporting}
                    isLoading={exporting === 'csv'}
                    onClick={() => runClosureExport('csv')}
                    title="CSV com separador ; (UTF-8)"
                  >
                    CSV
                  </Button>
                </div>
              }
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
                  {timesheet.delayMinutes > 24 * 60 && (
                    <p className="text-xs text-red-600 mt-1">Valor alto? Confira a escala e use &quot;Recalcular&quot;.</p>
                  )}
                </div>
                <div className="bg-gradient-to-br from-orange-50 to-orange-100 rounded-xl p-5 border border-orange-200">
                  <p className="text-sm font-medium text-orange-700 mb-1">Ausências</p>
                  <p className="text-3xl font-bold text-orange-900">{formatMinutes(timesheet.absenceMinutes)}</p>
                </div>
              </div>
              {timesheet.closedAt && (
                <div className="mt-4 pt-4 border-t border-gray-200 flex flex-wrap items-center gap-3">
                  <Badge variant="success" size="md">
                    Fechado em {new Date(timesheet.closedAt).toLocaleDateString('pt-BR', {
                      day: '2-digit',
                      month: 'long',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </Badge>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => closeMutation.mutate()}
                    disabled={closeMutation.isPending}
                    isLoading={closeMutation.isPending}
                  >
                    Recalcular resumo
                  </Button>
                  <span className="text-xs text-gray-500">
                    Atualiza horas, atrasos e ausências com a escala atual
                    {hasUnsavedChanges ? ' (dados já salvos no servidor; salve a grade se editou algo).' : '.'}
                  </span>
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

      <Modal
        isOpen={!!clearDayTarget}
        onClose={() => setClearDayTarget(null)}
        title="Limpar horários deste dia?"
        description={
          clearDayTarget
            ? `As batidas de ${clearDayTarget.dayLabel} serão apagadas só na grade local.`
            : undefined
        }
        size="sm"
        footer={
          <>
            <Button type="button" variant="outline" size="sm" onClick={() => setClearDayTarget(null)}>
              Cancelar
            </Button>
            <Button
              type="button"
              variant="danger"
              size="sm"
              icon={<TrashIcon className="h-4 w-4" />}
              onClick={confirmClearDay}
            >
              Limpar dia
            </Button>
          </>
        }
      >
        <div className="flex gap-4 rounded-xl border border-amber-200/80 bg-gradient-to-br from-amber-50 to-orange-50/60 p-4">
          <div
            className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-red-100 text-red-600 shadow-inner"
            aria-hidden
          >
            <TrashIcon className="h-6 w-6" />
          </div>
          <div className="min-w-0 space-y-2 text-sm text-slate-700">
            <p className="font-medium text-slate-900">Esta ação remove todos os horários preenchidos nesta linha.</p>
            <ul className="list-disc space-y-1 pl-4 text-slate-600">
              <li>Os campos voltam ao estado vazio (quatro batidas em branco).</li>
              <li>
                Nada é enviado ao servidor até você clicar em <span className="font-medium text-slate-800">Salvar Grade</span>.
              </li>
            </ul>
          </div>
        </div>
      </Modal>
    </div>
  )
}
