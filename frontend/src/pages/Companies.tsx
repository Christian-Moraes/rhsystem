import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { companyService, Company, CompanyRequest } from '../services/companyService'
import { useToast } from '../contexts/ToastContext'
import Card from '../components/ui/Card'
import Table from '../components/ui/Table'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import Input from '../components/ui/Input'
import Badge from '../components/ui/Badge'
import { PlusIcon, EditIcon, BuildingIcon } from '../components/icons'

export default function Companies() {
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [showModal, setShowModal] = useState(false)
  const [editingCompany, setEditingCompany] = useState<Company | null>(null)
  const [formData, setFormData] = useState<CompanyRequest>({
    name: '',
    cnpj: '',
    responsibleName: '',
    email: '',
    phone: '',
    enableOvertime: true,
    enableTimeBank: false,
    overtimePercentage: 50,
    weekendPercentage: 50,
  })

  const { data: companies = [], isLoading } = useQuery({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
  })

  const createMutation = useMutation({
    mutationFn: (data: CompanyRequest) => companyService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'] })
      setShowModal(false)
      resetForm()
      showToast('Empresa criada com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao criar empresa', 'error')
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CompanyRequest }) =>
      companyService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'] })
      setShowModal(false)
      setEditingCompany(null)
      resetForm()
      showToast('Empresa atualizada com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao atualizar empresa', 'error')
    },
  })

  const resetForm = () => {
    setFormData({
      name: '',
      cnpj: '',
      responsibleName: '',
      email: '',
      phone: '',
      enableOvertime: true,
      enableTimeBank: false,
      overtimePercentage: 50,
      weekendPercentage: 50,
    })
  }

  const handleEdit = (company: Company) => {
    setEditingCompany(company)
    setFormData({
      name: company.name,
      cnpj: company.cnpj,
      responsibleName: company.responsibleName,
      email: company.email,
      phone: company.phone,
      enableOvertime: company.enableOvertime,
      enableTimeBank: company.enableTimeBank,
      overtimePercentage: company.overtimePercentage,
      weekendPercentage: company.weekendPercentage,
    })
    setShowModal(true)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (editingCompany) {
      updateMutation.mutate({ id: editingCompany.id, data: formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  if (isLoading) {
    return (
      <Card>
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <p className="mt-4 text-sm text-gray-500">Carregando empresas...</p>
        </div>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Empresas</h1>
          <p className="text-sm text-gray-500 mt-1">
            Gerencie empresas e suas configurações
          </p>
        </div>
        <Button
          onClick={() => {
            setEditingCompany(null)
            resetForm()
            setShowModal(true)
          }}
          icon={<PlusIcon />}
        >
          Nova Empresa
        </Button>
      </div>

      <Card>
        {companies.length === 0 ? (
          <div className="text-center py-12">
            <BuildingIcon className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 mb-4">Nenhuma empresa cadastrada</p>
            <Button
              onClick={() => {
                setEditingCompany(null)
                resetForm()
                setShowModal(true)
              }}
              icon={<PlusIcon />}
            >
              Criar Primeira Empresa
            </Button>
          </div>
        ) : (
          <Table
            headers={['Nome', 'CNPJ', 'Email', 'Status', 'Ações']}
            emptyMessage="Nenhuma empresa encontrada"
          >
            {companies.map((company) => (
              <tr key={company.id} className="hover:bg-gray-50 transition-colors">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
                      <BuildingIcon className="w-5 h-5 text-white" />
                    </div>
                    <div className="ml-4">
                      <div className="text-sm font-medium text-gray-900">{company.name}</div>
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{company.cnpj}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{company.email}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <Badge variant={company.enableOvertime ? 'success' : 'default'}>
                    {company.enableOvertime ? 'Ativa' : 'Inativa'}
                  </Badge>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <button
                    onClick={() => handleEdit(company)}
                    className="text-blue-600 hover:text-blue-900 font-medium flex items-center gap-1"
                  >
                    <EditIcon />
                    Editar
                  </button>
                </td>
              </tr>
            ))}
          </Table>
        )}
      </Card>

      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false)
          setEditingCompany(null)
          resetForm()
        }}
        title={editingCompany ? 'Editar Empresa' : 'Nova Empresa'}
        description={editingCompany ? 'Atualize as informações da empresa' : 'Preencha os dados para cadastrar uma nova empresa'}
        size="lg"
        footer={
          <div className="flex justify-end space-x-3">
            <Button
              variant="outline"
              onClick={() => {
                setShowModal(false)
                setEditingCompany(null)
                resetForm()
              }}
            >
              Cancelar
            </Button>
            <Button
              onClick={handleSubmit}
              isLoading={createMutation.isPending || updateMutation.isPending}
            >
              {editingCompany ? 'Atualizar' : 'Criar'}
            </Button>
          </div>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Nome da Empresa"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />
            <Input
              label="CNPJ"
              value={formData.cnpj}
              onChange={(e) => setFormData({ ...formData, cnpj: e.target.value })}
              required
            />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Responsável"
              value={formData.responsibleName}
              onChange={(e) => setFormData({ ...formData, responsibleName: e.target.value })}
              required
            />
            <Input
              label="Email"
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              required
            />
          </div>
          <Input
            label="Telefone"
            value={formData.phone}
            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
            placeholder="(00) 00000-0000"
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-3">
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.enableOvertime}
                  onChange={(e) => setFormData({ ...formData, enableOvertime: e.target.checked })}
                  className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span className="text-sm font-medium text-gray-700">Habilitar Hora Extra</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.enableTimeBank}
                  onChange={(e) => setFormData({ ...formData, enableTimeBank: e.target.checked })}
                  className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span className="text-sm font-medium text-gray-700">Habilitar Banco de Horas</span>
              </label>
            </div>
            <div className="space-y-4">
              <Input
                label="% Hora Extra"
                type="number"
                value={formData.overtimePercentage}
                onChange={(e) => setFormData({ ...formData, overtimePercentage: parseInt(e.target.value) })}
                min="0"
                max="200"
                required
              />
              <Input
                label="% Fim de Semana"
                type="number"
                value={formData.weekendPercentage}
                onChange={(e) => setFormData({ ...formData, weekendPercentage: parseInt(e.target.value) })}
                min="0"
                max="200"
                required
              />
            </div>
          </div>
        </form>
      </Modal>
    </div>
  )
}
