import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { employeeService, Employee, EmployeeRequest } from '../services/employeeService'
import {
  workScheduleService,
  WorkSchedule,
  WorkScheduleCreateRequest,
} from '../services/workScheduleService'
import { useToast } from '../contexts/ToastContext'
import Card from '../components/ui/Card'
import Table from '../components/ui/Table'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import Input from '../components/ui/Input'
import Badge from '../components/ui/Badge'
import CompanySelect from '../components/ui/CompanySelect'
import { PlusIcon, EditIcon, TrashIcon, DocumentIcon, UsersIcon, BuildingIcon } from '../components/icons'

/** Texto curto só com horários (lista / tabela). */
function formatWorkScheduleShort(s: WorkSchedule): string {
  return `${s.entryTime}–${s.exitTime} · ${s.dailyHours}h`
}

/**
 * Rótulo legível para o select (sem ID). Índice 1-based para o RH achar “Jornada 1, 2…”.
 */
function formatScheduleOptionLabel(s: WorkSchedule, indexOneBased: number): string {
  return `Jornada ${indexOneBased}: das ${s.entryTime} às ${s.exitTime} · ${s.dailyHours} h/dia · intervalo ${s.breakMinutes} min · tolerância atraso ${s.toleranceLate} min · saída ${s.toleranceLeave} min`
}

function ScheduleSummaryHero({
  schedule,
  badge,
}: {
  schedule: WorkSchedule
  badge?: string
}) {
  return (
    <div className="rounded-xl border border-blue-100 bg-gradient-to-br from-blue-50/90 to-white px-4 py-4 sm:px-5">
      {badge && (
        <p className="text-[11px] font-semibold uppercase tracking-wide text-blue-700/90 mb-2">{badge}</p>
      )}
      <div className="flex flex-wrap items-end gap-6 sm:gap-10">
        <div>
          <p className="text-xs font-medium text-gray-500 mb-0.5">Entrada</p>
          <p className="text-2xl sm:text-3xl font-semibold tabular-nums text-gray-900 tracking-tight">
            {schedule.entryTime}
          </p>
        </div>
        <div className="hidden sm:block text-gray-300 pb-1 text-2xl font-light">→</div>
        <div>
          <p className="text-xs font-medium text-gray-500 mb-0.5">Saída</p>
          <p className="text-2xl sm:text-3xl font-semibold tabular-nums text-gray-900 tracking-tight">
            {schedule.exitTime}
          </p>
        </div>
      </div>
      <p className="mt-3 text-sm text-gray-600">
        <span className="font-medium text-gray-800">{schedule.dailyHours} horas por dia</span>
        <span className="text-gray-400 mx-2">·</span>
        intervalo {schedule.breakMinutes} min
        <span className="text-gray-400 mx-2">·</span>
        tolerância atraso {schedule.toleranceLate} min
        <span className="text-gray-400 mx-2">·</span>
        tolerância saída {schedule.toleranceLeave} min
      </p>
    </div>
  )
}

function scheduleIdsEqual(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase()
}

/** Campos numéricos podem ficar vazios enquanto o usuário apaga e redigita (evita travar em 0). */
type NewScheduleFormState = {
  dailyHours: number | ''
  breakMinutes: number | ''
  entryTime: string
  exitTime: string
  toleranceLate: number | ''
  toleranceLeave: number | ''
}

function defaultNewScheduleForm(): NewScheduleFormState {
  return {
    dailyHours: 8,
    entryTime: '09:00',
    exitTime: '18:00',
    breakMinutes: 60,
    toleranceLate: 15,
    toleranceLeave: 15,
  }
}

/** Aceita campo vazio durante a edição; string numérica vira inteiro (0 permitido para tolerâncias). */
function parseOptionalIntField(raw: string): number | '' {
  if (raw === '') return ''
  const n = parseInt(raw, 10)
  return Number.isNaN(n) ? '' : n
}

export default function Employees() {
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [selectedCompanyId, setSelectedCompanyId] = useState<string>('')
  const [showModal, setShowModal] = useState(false)
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null)
  const [formData, setFormData] = useState<EmployeeRequest>({
    name: '',
    cpf: '',
    role: '',
    hireDate: '',
    workScheduleId: '',
  })
  const [showNewSchedule, setShowNewSchedule] = useState(false)
  const [newScheduleForm, setNewScheduleForm] = useState<NewScheduleFormState>(() =>
    defaultNewScheduleForm()
  )

  const { data: employees = [], isLoading } = useQuery({
    queryKey: ['employees', selectedCompanyId],
    queryFn: () => employeeService.getAll(selectedCompanyId),
    enabled: !!selectedCompanyId,
  })

  const { data: workSchedules = [], isLoading: schedulesLoading } = useQuery({
    queryKey: ['work-schedules', selectedCompanyId],
    queryFn: () => workScheduleService.getAll(selectedCompanyId),
    enabled: !!selectedCompanyId,
  })

  const createMutation = useMutation({
    mutationFn: (data: EmployeeRequest) =>
      employeeService.create(selectedCompanyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employees'] })
      setShowModal(false)
      resetForm()
      showToast('Funcionário criado com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao criar funcionário', 'error')
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: EmployeeRequest }) =>
      employeeService.update(selectedCompanyId, id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employees'] })
      setShowModal(false)
      setEditingEmployee(null)
      resetForm()
      showToast('Funcionário atualizado com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao atualizar funcionário', 'error')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => employeeService.delete(selectedCompanyId, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employees'] })
      showToast('Funcionário excluído com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao excluir funcionário', 'error')
    },
  })

  const createScheduleMutation = useMutation({
    mutationFn: (data: WorkScheduleCreateRequest) =>
      workScheduleService.create(selectedCompanyId, data),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['work-schedules', selectedCompanyId] })
      setFormData((prev) => ({ ...prev, workScheduleId: created.id }))
      setShowNewSchedule(false)
      setNewScheduleForm(defaultNewScheduleForm())
      showToast('Escala criada e aplicada ao funcionário', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao criar escala', 'error')
    },
  })

  const resetForm = () => {
    setFormData({ name: '', cpf: '', role: '', hireDate: '', workScheduleId: '' })
    setShowNewSchedule(false)
    setNewScheduleForm(defaultNewScheduleForm())
  }

  const handleEdit = (employee: Employee) => {
    setEditingEmployee(employee)
    const wid = employee.workScheduleId
    setFormData({
      name: employee.name,
      cpf: employee.cpf,
      role: employee.role || '',
      hireDate: employee.hireDate,
      workScheduleId: wid != null && wid !== '' ? String(wid) : '',
    })
    setShowModal(true)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedCompanyId) {
      showToast('Selecione uma empresa', 'error')
      return
    }
    const wid = formData.workScheduleId?.trim()
    const payload: EmployeeRequest = {
      name: formData.name,
      cpf: formData.cpf,
      role: formData.role,
      hireDate: formData.hireDate,
      workScheduleId: wid ? wid : null,
    }
    if (editingEmployee) {
      updateMutation.mutate({ id: editingEmployee.id, data: payload })
    } else {
      createMutation.mutate(payload)
    }
  }

  const handleCreateSchedule = () => {
    if (!selectedCompanyId) {
      showToast('Selecione uma empresa', 'error')
      return
    }
    const f = newScheduleForm
    if (
      f.dailyHours === '' ||
      f.breakMinutes === '' ||
      f.toleranceLate === '' ||
      f.toleranceLeave === ''
    ) {
      showToast('Preencha todos os campos numéricos da jornada (incluindo tolerâncias).', 'error')
      return
    }
    const payload: WorkScheduleCreateRequest = {
      dailyHours: Number(f.dailyHours),
      breakMinutes: Number(f.breakMinutes),
      entryTime: f.entryTime,
      exitTime: f.exitTime,
      toleranceLate: Number(f.toleranceLate),
      toleranceLeave: Number(f.toleranceLeave),
    }
    if (payload.dailyHours < 1) {
      showToast('Horas por dia deve ser no mínimo 1.', 'error')
      return
    }
    createScheduleMutation.mutate(payload)
  }

  const scheduleLabelForRow = (workScheduleId?: string) => {
    if (!workScheduleId) return 'Jornada da empresa (sem vínculo)'
    const s = workSchedules.find((w) => scheduleIdsEqual(w.id, workScheduleId))
    return s ? formatWorkScheduleShort(s) : 'Jornada não encontrada na lista'
  }

  const selectedScheduleId = useMemo(() => {
    const id = formData.workScheduleId
    if (id == null || id === '') return ''
    return String(id).trim()
  }, [formData.workScheduleId])

  const linkedSchedule = useMemo(() => {
    if (!selectedScheduleId) return null
    return workSchedules.find((w) => scheduleIdsEqual(w.id, selectedScheduleId)) ?? null
  }, [workSchedules, selectedScheduleId])

  const hasOrphanSchedule =
    !!selectedCompanyId &&
    !schedulesLoading &&
    !!selectedScheduleId &&
    linkedSchedule === null

  const defaultCompanySchedule = workSchedules[0] ?? null

  /** Horários que o RH enxerga: escala vinculada ao funcionário, ou referência do «padrão» (1ª da empresa). */
  const displaySchedule = useMemo((): WorkSchedule | null => {
    if (linkedSchedule) return linkedSchedule
    if (!selectedScheduleId && defaultCompanySchedule) return defaultCompanySchedule
    return null
  }, [linkedSchedule, selectedScheduleId, defaultCompanySchedule])

  const scheduleSelectValue =
    formData.workScheduleId == null || formData.workScheduleId === ''
      ? ''
      : String(formData.workScheduleId)

  const hasAnyCompanySchedule = workSchedules.length > 0

  const defaultOptionLabel = defaultCompanySchedule
    ? `Mesma jornada da empresa para todos (não fixa no funcionário) — hoje equivale a: das ${defaultCompanySchedule.entryTime} às ${defaultCompanySchedule.exitTime}, ${defaultCompanySchedule.dailyHours} h/dia`
    : ''

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Funcionários</h1>
          <p className="text-sm text-gray-500 mt-1">
            Cadastre e gerencie funcionários
          </p>
        </div>
        <Button
          onClick={() => {
            setEditingEmployee(null)
            resetForm()
            setShowModal(true)
          }}
          icon={<PlusIcon />}
        >
          Novo Funcionário
        </Button>
      </div>

      <Card>
        <div className="mb-6">
          <CompanySelect
            label="Selecione a Empresa"
            value={selectedCompanyId}
            onChange={setSelectedCompanyId}
            required
          />
        </div>

        {selectedCompanyId && (
          <>
            {isLoading ? (
              <div className="text-center py-12">
                <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                <p className="mt-4 text-sm text-gray-500">Carregando funcionários...</p>
              </div>
            ) : employees.length === 0 ? (
              <div className="text-center py-12">
                <UsersIcon className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500 mb-4">Nenhum funcionário cadastrado</p>
                <Button
                  onClick={() => {
                    setEditingEmployee(null)
                    resetForm()
                    setShowModal(true)
                  }}
                  icon={<PlusIcon />}
                >
                  Cadastrar Primeiro Funcionário
                </Button>
              </div>
            ) : (
              <Table
                headers={['Nome', 'CPF', 'Cargo', 'Jornada', 'Status', 'Ações']}
                emptyMessage="Nenhum funcionário encontrado"
              >
                {employees.map((employee) => (
                  <tr key={employee.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="flex-shrink-0 h-10 w-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
                          <span className="text-sm font-semibold text-white">
                            {employee.name.charAt(0).toUpperCase()}
                          </span>
                        </div>
                        <div className="ml-4">
                          <div className="text-sm font-medium text-gray-900">{employee.name}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{employee.cpf}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{employee.role || '-'}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 max-w-[200px] truncate" title={scheduleLabelForRow(employee.workScheduleId)}>
                      {scheduleLabelForRow(employee.workScheduleId)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge variant={employee.status === 'ACTIVE' ? 'success' : 'default'}>
                        {employee.status === 'ACTIVE' ? 'Ativo' : 'Inativo'}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <div className="flex items-center space-x-3">
                        <Link
                          to={`/timesheet?employeeId=${employee.id}`}
                          className="text-blue-600 hover:text-blue-900 font-medium flex items-center gap-1"
                        >
                          <DocumentIcon className="w-4 h-4" />
                          Fechamento
                        </Link>
                        <button
                          onClick={() => handleEdit(employee)}
                          className="text-gray-600 hover:text-gray-900 font-medium flex items-center gap-1"
                        >
                          <EditIcon />
                          Editar
                        </button>
                        <button
                          onClick={() => {
                            if (window.confirm('Deseja realmente excluir este funcionário?')) {
                              deleteMutation.mutate(employee.id)
                            }
                          }}
                          className="text-red-600 hover:text-red-900 font-medium flex items-center gap-1"
                        >
                          <TrashIcon />
                          Excluir
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </Table>
            )}
          </>
        )}

        {!selectedCompanyId && (
          <div className="text-center py-12">
            <BuildingIcon className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 mb-4">Selecione uma empresa para visualizar os funcionários</p>
            <Link to="/companies">
              <Button variant="outline">Ir para Empresas</Button>
            </Link>
          </div>
        )}
      </Card>

      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false)
          setEditingEmployee(null)
          resetForm()
        }}
        title={editingEmployee ? 'Editar Funcionário' : 'Novo Funcionário'}
        description={editingEmployee ? 'Atualize as informações do funcionário' : 'Preencha os dados para cadastrar um novo funcionário'}
        size="lg"
        footer={
          <div className="flex justify-end space-x-3">
            <Button
              variant="outline"
              onClick={() => {
                setShowModal(false)
                setEditingEmployee(null)
                resetForm()
              }}
            >
              Cancelar
            </Button>
            <Button
              onClick={handleSubmit}
              isLoading={createMutation.isPending || updateMutation.isPending}
            >
              {editingEmployee ? 'Atualizar' : 'Criar'}
            </Button>
          </div>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          {!editingEmployee && (
            <CompanySelect
              label="Empresa"
              value={selectedCompanyId}
              onChange={(id) => {
                setSelectedCompanyId(id)
                setFormData((prev) => ({ ...prev, workScheduleId: null }))
              }}
              required
            />
          )}

          <div className="space-y-4">
            <div>
              <h3 className="text-sm font-semibold text-gray-900">Jornada de trabalho</h3>
              <p className="text-xs text-gray-500 mt-0.5 leading-relaxed">
                Aqui você define <span className="font-medium text-gray-700">que horários</span> o sistema
                usa para atraso e fechamento. Tudo é descrito em horário e minutos — não é preciso decorar
                código nem ID.
              </p>
            </div>

            {!selectedCompanyId ? (
              <div className="rounded-lg border border-dashed border-gray-300 bg-gray-50 px-4 py-3 text-sm text-gray-600">
                {editingEmployee
                  ? 'Selecione a empresa no filtro da página para carregar as jornadas cadastradas.'
                  : 'Primeiro escolha a empresa acima; em seguida você cadastra ou escolhe a jornada.'}
              </div>
            ) : schedulesLoading ? (
              <p className="text-sm text-gray-500 py-2">Carregando jornadas desta empresa…</p>
            ) : !hasAnyCompanySchedule ? (
              <div className="rounded-xl border border-amber-200 bg-amber-50/60 px-4 py-4 space-y-3">
                <p className="text-sm font-medium text-amber-950">
                  Esta empresa ainda não tem nenhuma jornada cadastrada
                </p>
                <p className="text-sm text-amber-900/90 leading-relaxed">
                  Sem isso, não dá para mostrar entrada e saída. Preencha os horários abaixo e salve: a
                  jornada fica guardada na empresa e já pode ser usada neste funcionário.
                </p>
                <div className="rounded-lg border border-amber-100/80 bg-white p-3 space-y-3">
                  <p className="text-xs font-semibold text-gray-700 uppercase tracking-wide">
                    Cadastrar primeira jornada
                  </p>
                  <div className="grid grid-cols-2 gap-3">
                    <Input
                      label="Horas/dia"
                      type="number"
                      min={1}
                      value={newScheduleForm.dailyHours === '' ? '' : newScheduleForm.dailyHours}
                      onChange={(e) =>
                        setNewScheduleForm((f) => ({
                          ...f,
                          dailyHours: parseOptionalIntField(e.target.value),
                        }))
                      }
                      required
                    />
                    <Input
                      label="Intervalo (min)"
                      type="number"
                      min={0}
                      value={newScheduleForm.breakMinutes === '' ? '' : newScheduleForm.breakMinutes}
                      onChange={(e) =>
                        setNewScheduleForm((f) => ({
                          ...f,
                          breakMinutes: parseOptionalIntField(e.target.value),
                        }))
                      }
                      required
                    />
                    <Input
                      label="Entrada"
                      type="time"
                      value={newScheduleForm.entryTime}
                      onChange={(e) =>
                        setNewScheduleForm((f) => ({ ...f, entryTime: e.target.value }))
                      }
                      required
                    />
                    <Input
                      label="Saída"
                      type="time"
                      value={newScheduleForm.exitTime}
                      onChange={(e) =>
                        setNewScheduleForm((f) => ({ ...f, exitTime: e.target.value }))
                      }
                      required
                    />
                    <Input
                      label="Tolerância atraso (min)"
                      type="number"
                      min={0}
                      value={newScheduleForm.toleranceLate === '' ? '' : newScheduleForm.toleranceLate}
                      onChange={(e) =>
                        setNewScheduleForm((f) => ({
                          ...f,
                          toleranceLate: parseOptionalIntField(e.target.value),
                        }))
                      }
                      required
                    />
                    <Input
                      label="Tolerância saída (min)"
                      type="number"
                      min={0}
                      value={newScheduleForm.toleranceLeave === '' ? '' : newScheduleForm.toleranceLeave}
                      onChange={(e) =>
                        setNewScheduleForm((f) => ({
                          ...f,
                          toleranceLeave: parseOptionalIntField(e.target.value),
                        }))
                      }
                      required
                    />
                  </div>
                  <Button
                    type="button"
                    isLoading={createScheduleMutation.isPending}
                    onClick={handleCreateSchedule}
                  >
                    Salvar jornada e usar neste funcionário
                  </Button>
                </div>
              </div>
            ) : (
              <>
                <div className="w-full space-y-2">
                  <label className="block text-sm font-medium text-gray-800">
                    Qual jornada este funcionário segue?
                  </label>
                  <p className="text-xs text-gray-500">
                    Cada linha descreve horário de entrada, saída e regras. Escolha uma ou deixe a opção da
                    empresa se ele não tiver jornada fixa no cadastro.
                  </p>
                  <select
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    value={scheduleSelectValue}
                    onChange={(e) => {
                      const v = e.target.value
                      setFormData((prev) => ({ ...prev, workScheduleId: v ? v : null }))
                    }}
                  >
                    <option value="">{defaultOptionLabel}</option>
                    {workSchedules.map((s, idx) => (
                      <option key={s.id} value={s.id}>
                        {formatScheduleOptionLabel(s, idx + 1)}
                      </option>
                    ))}
                    {hasOrphanSchedule && (
                      <option value={selectedScheduleId}>
                        Manter jornada já salva no funcionário (não aparece na lista da empresa — escolha
                        outra linha se quiser trocar)
                      </option>
                    )}
                  </select>
                </div>

                {hasOrphanSchedule && (
                  <p className="text-xs text-amber-900 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 leading-relaxed">
                    O cadastro deste funcionário aponta para uma jornada que não está entre as da empresa.
                    Selecione uma das jornadas listadas acima para alinhar, ou mantenha esta opção e salve
                    só se souber que o registro ainda é válido.
                  </p>
                )}

                {displaySchedule && (
                  <>
                    <ScheduleSummaryHero
                      schedule={displaySchedule}
                      badge={
                        linkedSchedule
                          ? 'Jornada fixada neste funcionário'
                          : 'Prévia — jornada da empresa (sem fixar no funcionário)'
                      }
                    />
                    <div className="rounded-lg border border-gray-200 bg-gray-50/80 p-3 space-y-2">
                      <p className="text-xs font-medium text-gray-600">
                        Mesmos horários em formato de formulário (só leitura)
                      </p>
                      <div className="grid grid-cols-2 gap-3">
                        <Input
                          label="Horas/dia"
                          readOnly
                          tabIndex={-1}
                          value={String(displaySchedule.dailyHours)}
                          className="bg-white cursor-default"
                        />
                        <Input
                          label="Intervalo (min)"
                          readOnly
                          tabIndex={-1}
                          value={String(displaySchedule.breakMinutes)}
                          className="bg-white cursor-default"
                        />
                        <Input
                          label="Entrada"
                          type="time"
                          readOnly
                          tabIndex={-1}
                          value={displaySchedule.entryTime}
                          className="bg-white cursor-default"
                        />
                        <Input
                          label="Saída"
                          type="time"
                          readOnly
                          tabIndex={-1}
                          value={displaySchedule.exitTime}
                          className="bg-white cursor-default"
                        />
                        <Input
                          label="Tolerância atraso (min)"
                          readOnly
                          tabIndex={-1}
                          value={String(displaySchedule.toleranceLate)}
                          className="bg-white cursor-default"
                        />
                        <Input
                          label="Tolerância saída (min)"
                          readOnly
                          tabIndex={-1}
                          value={String(displaySchedule.toleranceLeave)}
                          className="bg-white cursor-default"
                        />
                      </div>
                    </div>
                  </>
                )}

                <div className="border border-gray-200 rounded-lg p-3 bg-white">
                  <button
                    type="button"
                    onClick={() => setShowNewSchedule((v) => !v)}
                    className="text-sm font-medium text-blue-600 hover:text-blue-800"
                  >
                    {showNewSchedule ? 'Fechar cadastro de nova jornada' : '+ Cadastrar outra jornada nesta empresa'}
                  </button>
                  {showNewSchedule && (
                    <div className="mt-3 space-y-3 pt-3 border-t border-gray-100">
                      <p className="text-xs text-gray-500">
                        Use quando existir turno diferente (ex.: comercial x plantão). Depois de salvar, ela
                        aparece na lista acima.
                      </p>
                      <div className="grid grid-cols-2 gap-3">
                        <Input
                          label="Horas/dia"
                          type="number"
                          min={1}
                          value={newScheduleForm.dailyHours === '' ? '' : newScheduleForm.dailyHours}
                          onChange={(e) =>
                            setNewScheduleForm((f) => ({
                              ...f,
                              dailyHours: parseOptionalIntField(e.target.value),
                            }))
                          }
                          required
                        />
                        <Input
                          label="Intervalo (min)"
                          type="number"
                          min={0}
                          value={newScheduleForm.breakMinutes === '' ? '' : newScheduleForm.breakMinutes}
                          onChange={(e) =>
                            setNewScheduleForm((f) => ({
                              ...f,
                              breakMinutes: parseOptionalIntField(e.target.value),
                            }))
                          }
                          required
                        />
                        <Input
                          label="Entrada"
                          type="time"
                          value={newScheduleForm.entryTime}
                          onChange={(e) =>
                            setNewScheduleForm((f) => ({ ...f, entryTime: e.target.value }))
                          }
                          required
                        />
                        <Input
                          label="Saída"
                          type="time"
                          value={newScheduleForm.exitTime}
                          onChange={(e) =>
                            setNewScheduleForm((f) => ({ ...f, exitTime: e.target.value }))
                          }
                          required
                        />
                        <Input
                          label="Tolerância atraso (min)"
                          type="number"
                          min={0}
                          value={newScheduleForm.toleranceLate === '' ? '' : newScheduleForm.toleranceLate}
                          onChange={(e) =>
                            setNewScheduleForm((f) => ({
                              ...f,
                              toleranceLate: parseOptionalIntField(e.target.value),
                            }))
                          }
                          required
                        />
                        <Input
                          label="Tolerância saída (min)"
                          type="number"
                          min={0}
                          value={newScheduleForm.toleranceLeave === '' ? '' : newScheduleForm.toleranceLeave}
                          onChange={(e) =>
                            setNewScheduleForm((f) => ({
                              ...f,
                              toleranceLeave: parseOptionalIntField(e.target.value),
                            }))
                          }
                          required
                        />
                      </div>
                      <Button
                        type="button"
                        variant="outline"
                        isLoading={createScheduleMutation.isPending}
                        onClick={handleCreateSchedule}
                      >
                        Salvar nova jornada e selecionar para este funcionário
                      </Button>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>

          <div className="space-y-4 pt-1 border-t border-gray-100">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">Dados do funcionário</p>
            <Input
              label="Nome Completo"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              placeholder="João Silva"
            />
            <Input
              label="CPF"
              value={formData.cpf}
              onChange={(e) => setFormData({ ...formData, cpf: e.target.value })}
              required
              placeholder="000.000.000-00"
            />
            <Input
              label="Cargo"
              value={formData.role}
              onChange={(e) => setFormData({ ...formData, role: e.target.value })}
              placeholder="Desenvolvedor"
            />
            <Input
              label="Data de Admissão"
              type="date"
              value={formData.hireDate}
              onChange={(e) => setFormData({ ...formData, hireDate: e.target.value })}
              required
            />
          </div>
        </form>
      </Modal>
    </div>
  )
}
