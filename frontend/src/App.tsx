import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { CompanyProvider } from './contexts/CompanyContext'
import { ToastProvider } from './contexts/ToastContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Companies from './pages/Companies'
import Employees from './pages/Employees'
import TimesheetPage from './pages/Timesheet'
import PrivateRoute from './components/PrivateRoute'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CompanyProvider>
          <ToastProvider>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route
                path="/dashboard"
                element={
                  <PrivateRoute>
                    <Layout>
                      <Dashboard />
                    </Layout>
                  </PrivateRoute>
                }
              />
              <Route
                path="/companies"
                element={
                  <PrivateRoute>
                    <Layout>
                      <Companies />
                    </Layout>
                  </PrivateRoute>
                }
              />
              <Route
                path="/employees"
                element={
                  <PrivateRoute>
                    <Layout>
                      <Employees />
                    </Layout>
                  </PrivateRoute>
                }
              />
              <Route
                path="/timesheet"
                element={
                  <PrivateRoute>
                    <Layout>
                      <TimesheetPage />
                    </Layout>
                  </PrivateRoute>
                }
              />
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </ToastProvider>
        </CompanyProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
