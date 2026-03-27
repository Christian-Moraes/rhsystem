import { useState } from 'react'
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

function formatWorkScheduleLabel(s: WorkSchedule): string {
  return `${s.entryTime} – ${s.exitTime} (${s.dailyHours}h/dia)`
}

function defaultNewScheduleForm(): WorkScheduleCreateRequest {
  return {
    dailyHours: 8,
    entryTime: '09:00',
    exitTime: '18:00',
    breakMinutes: 60,
    toleranceLate: 15,
    toleranceLeave: 15,
  }
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
  const [newScheduleForm, setNewScheduleForm] = useState<WorkScheduleCreateRequest>(() =>
    defaultNewScheduleForm()
  )

  const { data: employees = [], isLoading } = useQuery({
    queryKey: ['employees', selectedCompanyId],
    queryFn: () => employeeService.getAll(selectedCompanyId),
    enabled: !!selectedCompanyId,
  })

  const { data: workSchedules = [] } = useQuery({
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
    setFormData({
      name: employee.name,
      cpf: employee.cpf,
      role: employee.role || '',
      hireDate: employee.hireDate,
      workScheduleId: employee.workScheduleId || '',
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
    createScheduleMutation.mutate({ ...newScheduleForm })
  }

  const scheduleLabelForRow = (workScheduleId?: string) => {
    if (!workScheduleId) return 'Padrão da empresa'
    const s = workSchedules.find((w) => w.id === workScheduleId)
    return s ? formatWorkScheduleLabel(s) : workScheduleId.slice(0, 8) + '…'
  }

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
                headers={['Nome', 'CPF', 'Cargo', 'Escala', 'Status', 'Ações']}
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
        <form onSubmit={handleSubmit} className="space-y-4">
          {!editingEmployee && (
            <CompanySelect
              label="Empresa"
              value={selectedCompanyId}
              onChange={(id) => {
                setSelectedCompanyId(id)
              }}
              required
            />
          )}
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

          {selectedCompanyId && (
            <>
              <div className="w-full">
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  Escala de trabalho
                </label>
                <select
                  className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
                  value={typeof formData.workScheduleId === 'string' ? formData.workScheduleId : ''}
                  onChange={(e) =>
                    setFormData({ ...formData, workScheduleId: e.target.value || null })
                  }
                >
                  <option value="">Padrão da empresa (primeira escala cadastrada)</option>
                  {workSchedules.map((s) => (
                    <option key={s.id} value={s.id}>
                      {formatWorkScheduleLabel(s)} · atraso ±{s.toleranceLate} min
                    </option>
                  ))}
                </select>
                <p className="mt-1.5 text-xs text-gray-500">
                  Sem escala específica, o sistema usa a regra da empresa para entrada e tolerância.
                </p>
              </div>

              <div className="border border-gray-200 rounded-lg p-3 bg-gray-50/80">
                <button
                  type="button"
                  onClick={() => setShowNewSchedule((v) => !v)}
                  className="text-sm font-medium text-blue-600 hover:text-blue-800"
                >
                  {showNewSchedule ? 'Ocultar nova escala' : '+ Criar nova escala nesta empresa'}
                </button>
                {showNewSchedule && (
                  <div className="mt-3 space-y-3">
                    <div className="grid grid-cols-2 gap-3">
                      <Input
                        label="Horas/dia"
                        type="number"
                        min={1}
                        value={newScheduleForm.dailyHours}
                        onChange={(e) =>
                          setNewScheduleForm((f) => ({
                            ...f,
                            dailyHours: Number(e.target.value) || 0,
                          }))
                        }
                        required
                      />
                      <Input
                        label="Intervalo (min)"
                        type="number"
                        min={0}
                        value={newScheduleForm.breakMinutes}
                        onChange={(e) =>
                          setNewScheduleForm((f) => ({
                            ...f,
                            breakMinutes: Number(e.target.value) || 0,
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
                        value={newScheduleForm.toleranceLate}
                        onChange={(e) =>
                          setNewScheduleForm((f) => ({
                            ...f,
                            toleranceLate: Number(e.target.value) || 0,
                          }))
                        }
                        required
                      />
                      <Input
                        label="Tolerância saída (min)"
                        type="number"
                        min={0}
                        value={newScheduleForm.toleranceLeave}
                        onChange={(e) =>
                          setNewScheduleForm((f) => ({
                            ...f,
                            toleranceLeave: Number(e.target.value) || 0,
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
                      Salvar escala e selecionar
                    </Button>
                  </div>
                )}
              </div>
            </>
          )}
        </form>
      </Modal>
    </div>
  )
}
