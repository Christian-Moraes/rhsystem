import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { timeEventService, TimeEvent, TimeEventRequest } from '../services/timeEventService'
import { employeeService } from '../services/employeeService'
import { useCompany } from '../contexts/CompanyContext'
import { useToast } from '../contexts/ToastContext'
import Card from '../components/ui/Card'
import Table from '../components/ui/Table'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import Input from '../components/ui/Input'
import Badge from '../components/ui/Badge'

export default function TimeEvents() {
  const { employeeId } = useParams<{ employeeId: string }>()
  const { selectedCompany } = useCompany()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [showModal, setShowModal] = useState(false)
  const [editingEvent, setEditingEvent] = useState<TimeEvent | null>(null)
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>(employeeId || '')
  const [month, setMonth] = useState(new Date().getMonth() + 1)
  const [year, setYear] = useState(new Date().getFullYear())
  const [formData, setFormData] = useState<TimeEventRequest>({
    employeeId: selectedEmployeeId,
    eventType: 'ENTRY',
    eventTime: new Date().toISOString().slice(0, 16),
  })

  const { data: employees = [] } = useQuery({
    queryKey: ['employees', selectedCompany?.id],
    queryFn: () => employeeService.getAll(selectedCompany?.id || ''),
    enabled: !!selectedCompany?.id,
  })

  const { data: events = [], isLoading } = useQuery({
    queryKey: ['timeEvents', selectedCompany?.id, selectedEmployeeId, month, year],
    queryFn: () =>
      timeEventService.getByEmployeeAndMonth(
        selectedCompany?.id || '',
        selectedEmployeeId,
        month,
        year
      ),
    enabled: !!selectedCompany?.id && !!selectedEmployeeId,
  })

  const createMutation = useMutation({
    mutationFn: (data: TimeEventRequest) =>
      timeEventService.create(selectedCompany?.id || '', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['timeEvents'] })
      setShowModal(false)
      resetForm()
      showToast('Evento criado com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao criar evento', 'error')
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: TimeEventRequest }) =>
      timeEventService.update(selectedCompany?.id || '', id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['timeEvents'] })
      setShowModal(false)
      setEditingEvent(null)
      resetForm()
      showToast('Evento atualizado com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao atualizar evento', 'error')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => timeEventService.delete(selectedCompany?.id || '', id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['timeEvents'] })
      showToast('Evento excluído com sucesso!', 'success')
    },
    onError: (error: any) => {
      showToast(error.response?.data?.message || 'Erro ao excluir evento', 'error')
    },
  })

  const resetForm = () => {
    setFormData({
      employeeId: selectedEmployeeId,
      eventType: 'ENTRY',
      eventTime: new Date().toISOString().slice(0, 16),
    })
  }

  const handleEdit = (event: TimeEvent) => {
    setEditingEvent(event)
    setFormData({
      employeeId: event.employeeId,
      eventType: event.eventType,
      eventTime: new Date(event.eventTime).toISOString().slice(0, 16),
    })
    setShowModal(true)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (editingEvent) {
      updateMutation.mutate({ id: editingEvent.id, data: formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  const getStatusBadge = (status: string) => {
    const variants: Record<string, 'success' | 'warning' | 'danger'> = {
      CONFIRMED: 'success',
      REVIEW: 'warning',
      UNCERTAIN: 'danger',
    }
    return <Badge variant={variants[status] || 'default'}>{status}</Badge>
  }

  const getEventTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      ENTRY: 'Entrada',
      EXIT: 'Saída',
      BREAK_START: 'Início Intervalo',
      BREAK_END: 'Fim Intervalo',
    }
    return labels[type] || type
  }

  if (!selectedCompany) {
    return (
      <Card>
        <p className="text-gray-600">Selecione uma empresa primeiro.</p>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Eventos de Ponto</h1>
        <Button
          onClick={() => {
            setEditingEvent(null)
            resetForm()
            setShowModal(true)
          }}
          disabled={!selectedEmployeeId}
        >
          Novo Evento
        </Button>
      </div>

      <Card>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Funcionário
            </label>
            <select
              value={selectedEmployeeId}
              onChange={(e) => {
                setSelectedEmployeeId(e.target.value)
                setFormData({ ...formData, employeeId: e.target.value })
              }}
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
            <label className="block text-sm font-medium text-gray-700 mb-1">Mês</label>
            <input
              type="number"
              value={month}
              onChange={(e) => setMonth(parseInt(e.target.value))}
              min="1"
              max="12"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Ano</label>
            <input
              type="number"
              value={year}
              onChange={(e) => setYear(parseInt(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {selectedEmployeeId && (
          <>
            {isLoading ? (
              <div className="text-center py-8">
                <p className="text-gray-500">Carregando...</p>
              </div>
            ) : (
              <Table
                headers={['Data/Hora', 'Tipo', 'Status', 'Origem', 'Ações']}
                emptyMessage="Nenhum evento encontrado para este período"
              >
                {events.map((event) => (
                  <tr key={event.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      {new Date(event.eventTime).toLocaleString('pt-BR')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getEventTypeLabel(event.eventType)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getStatusBadge(event.status)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {event.source}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                      <button
                        onClick={() => handleEdit(event)}
                        className="text-blue-600 hover:text-blue-900"
                      >
                        Editar
                      </button>
                      <button
                        onClick={() => {
                          if (window.confirm('Deseja realmente excluir este evento?')) {
                            deleteMutation.mutate(event.id)
                          }
                        }}
                        className="text-red-600 hover:text-red-900"
                      >
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
              </Table>
            )}
          </>
        )}
      </Card>

      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false)
          setEditingEvent(null)
          resetForm()
        }}
        title={editingEvent ? 'Editar Evento' : 'Novo Evento'}
        size="md"
        footer={
          <div className="flex justify-end space-x-2">
            <Button
              variant="outline"
              onClick={() => {
                setShowModal(false)
                setEditingEvent(null)
                resetForm()
              }}
            >
              Cancelar
            </Button>
            <Button
              onClick={handleSubmit}
              isLoading={createMutation.isPending || updateMutation.isPending}
            >
              {editingEvent ? 'Atualizar' : 'Criar'}
            </Button>
          </div>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Funcionário
            </label>
            <select
              value={formData.employeeId}
              onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
              disabled={!!editingEvent}
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
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tipo de Evento
            </label>
            <select
              value={formData.eventType}
              onChange={(e) => setFormData({ ...formData, eventType: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            >
              <option value="ENTRY">Entrada</option>
              <option value="EXIT">Saída</option>
              <option value="BREAK_START">Início Intervalo</option>
              <option value="BREAK_END">Fim Intervalo</option>
            </select>
          </div>
          <Input
            label="Data e Hora"
            type="datetime-local"
            value={formData.eventTime}
            onChange={(e) => setFormData({ ...formData, eventTime: e.target.value })}
            required
          />
        </form>
      </Modal>
    </div>
  )
}
