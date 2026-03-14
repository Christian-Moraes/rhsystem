import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { employeeService, Employee, EmployeeRequest } from '../services/employeeService'
import { companyService } from '../services/companyService'
import { useToast } from '../contexts/ToastContext'
import Card from '../components/ui/Card'
import Table from '../components/ui/Table'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import Input from '../components/ui/Input'
import Badge from '../components/ui/Badge'
import CompanySelect from '../components/ui/CompanySelect'
import { PlusIcon, EditIcon, TrashIcon, DocumentIcon, UsersIcon, BuildingIcon } from '../components/icons'

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

  const { data: companies = [] } = useQuery({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
  })

  const { data: employees = [], isLoading } = useQuery({
    queryKey: ['employees', selectedCompanyId],
    queryFn: () => employeeService.getAll(selectedCompanyId),
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

  const resetForm = () => {
    setFormData({ name: '', cpf: '', role: '', hireDate: '', workScheduleId: '' })
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
    if (editingEmployee) {
      updateMutation.mutate({ id: editingEmployee.id, data: formData })
    } else {
      createMutation.mutate(formData)
    }
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
                headers={['Nome', 'CPF', 'Cargo', 'Status', 'Ações']}
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
        size="md"
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
        </form>
      </Modal>
    </div>
  )
}
