import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useCompany } from '../contexts/CompanyContext'
import { companyService } from '../services/companyService'
import { employeeService } from '../services/employeeService'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import { DashboardIcon, BuildingIcon, UsersIcon, UploadIcon, DocumentIcon, ClockIcon, PlusIcon } from '../components/icons'

export default function Dashboard() {
  const { selectedCompany } = useCompany()

  const { data: companies = [] } = useQuery({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
  })

  const { data: employees = [] } = useQuery({
    queryKey: ['employees', selectedCompany?.id],
    queryFn: () => employeeService.getAll(selectedCompany?.id || ''),
    enabled: !!selectedCompany?.id,
  })

  const stats = [
    {
      name: 'Empresas',
      value: companies.length,
      icon: BuildingIcon,
      color: 'from-blue-500 to-blue-600',
      link: '/companies',
    },
    {
      name: 'Funcionários',
      value: employees.length,
      icon: UsersIcon,
      color: 'from-green-500 to-green-600',
      link: '/employees',
    },
    {
      name: 'Eventos de Ponto',
      icon: ClockIcon,
      color: 'from-purple-500 to-purple-600',
      link: '/time-events',
    },
    {
      name: 'Importar Ponto',
      icon: UploadIcon,
      color: 'from-indigo-500 to-indigo-600',
      link: '/time-import',
    },
    {
      name: 'Fechamento',
      icon: DocumentIcon,
      color: 'from-orange-500 to-orange-600',
      link: '/timesheet',
    },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">
          Visão geral do sistema de gestão de ponto
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
        {stats.map((stat) => {
          const Icon = stat.icon
          return (
            <Link key={stat.name} to={stat.link}>
              <Card className="hover:shadow-lg transition-all duration-200 cursor-pointer group border-2 border-transparent hover:border-blue-200">
                <div className="flex items-center space-x-4">
                  <div className={`flex-shrink-0 w-12 h-12 rounded-xl bg-gradient-to-br ${stat.color} flex items-center justify-center shadow-sm group-hover:scale-110 transition-transform`}>
                    <Icon className="w-6 h-6 text-white" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-500 truncate">{stat.name}</p>
                    {stat.value !== undefined && (
                      <p className="text-2xl font-bold text-gray-900 mt-1">{stat.value}</p>
                    )}
                  </div>
                </div>
              </Card>
            </Link>
          )
        })}
      </div>

      {selectedCompany && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card title="Empresa Ativa" description="Informações da empresa selecionada">
            <div className="space-y-4">
              <div className="flex items-center justify-between py-2 border-b border-gray-100">
                <span className="text-sm text-gray-500">Nome</span>
                <span className="font-semibold text-gray-900">{selectedCompany.name}</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-gray-100">
                <span className="text-sm text-gray-500">CNPJ</span>
                <span className="font-medium text-gray-700">{selectedCompany.cnpj}</span>
              </div>
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-gray-500">Permissão</span>
                <Badge variant={selectedCompany.role === 'ADMIN' ? 'success' : 'default'}>
                  {selectedCompany.role}
                </Badge>
              </div>
            </div>
          </Card>

          <Card title="Ações Rápidas" description="Acesso rápido às principais funcionalidades">
            <div className="space-y-2">
              <Link to="/employees">
                <button className="w-full text-left px-4 py-3 rounded-lg hover:bg-blue-50 transition-colors border border-transparent hover:border-blue-200 group">
                  <div className="flex items-center space-x-3">
                    <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-blue-100 group-hover:bg-blue-200 flex items-center justify-center transition-colors">
                      <UsersIcon className="w-4 h-4 text-blue-600" />
                    </div>
                    <div className="flex-1">
                      <span className="font-medium text-gray-900 block">Cadastrar Funcionário</span>
                      <p className="text-sm text-gray-500">Adicionar novo funcionário</p>
                    </div>
                  </div>
                </button>
              </Link>
              <Link to="/time-import">
                <button className="w-full text-left px-4 py-3 rounded-lg hover:bg-purple-50 transition-colors border border-transparent hover:border-purple-200 group">
                  <div className="flex items-center space-x-3">
                    <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-purple-100 group-hover:bg-purple-200 flex items-center justify-center transition-colors">
                      <UploadIcon className="w-4 h-4 text-purple-600" />
                    </div>
                    <div className="flex-1">
                      <span className="font-medium text-gray-900 block">Importar Ponto</span>
                      <p className="text-sm text-gray-500">Importar registros de ponto</p>
                    </div>
                  </div>
                </button>
              </Link>
              <Link to="/timesheet">
                <button className="w-full text-left px-4 py-3 rounded-lg hover:bg-orange-50 transition-colors border border-transparent hover:border-orange-200 group">
                  <div className="flex items-center space-x-3">
                    <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-orange-100 group-hover:bg-orange-200 flex items-center justify-center transition-colors">
                      <DocumentIcon className="w-4 h-4 text-orange-600" />
                    </div>
                    <div className="flex-1">
                      <span className="font-medium text-gray-900 block">Fechar Mês</span>
                      <p className="text-sm text-gray-500">Fechar fechamento mensal</p>
                    </div>
                  </div>
                </button>
              </Link>
            </div>
          </Card>
        </div>
      )}

      {!selectedCompany && companies.length === 0 && (
        <Card>
          <div className="text-center py-12">
            <BuildingIcon className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Nenhuma empresa cadastrada</h3>
            <p className="text-gray-500 mb-6">Comece criando sua primeira empresa</p>
            <Link to="/companies">
              <Button icon={<PlusIcon />}>
                Criar Primeira Empresa
              </Button>
            </Link>
          </div>
        </Card>
      )}
    </div>
  )
}
