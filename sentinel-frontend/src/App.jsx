import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './AuthContext';
import Header from './components/Header';
import Login from './pages/Login';
import AlertQueue from './pages/AlertQueue';
import AlertDetail from './pages/AlertDetail';
import CaseList from './pages/CaseList';
import CaseDetail from './pages/CaseDetail';
import AuditLog from './pages/AuditLog';
import Ingestion from './pages/Ingestion';

function RequireAuth({ children }) {
  const { auth } = useAuth();
  const location = useLocation();
  if (!auth) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  return children;
}

function Layout({ children }) {
  return (
    <>
      <Header />
      <main className="content">{children}</main>
    </>
  );
}

function AppRoutes() {
  const { auth } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={auth ? <Navigate to="/alerts" replace /> : <Login />} />
      <Route
        path="/alerts"
        element={
          <RequireAuth>
            <Layout><AlertQueue /></Layout>
          </RequireAuth>
        }
      />
      <Route
        path="/alerts/:id"
        element={
          <RequireAuth>
            <Layout><AlertDetail /></Layout>
          </RequireAuth>
        }
      />
      <Route
        path="/cases"
        element={
          <RequireAuth>
            <Layout><CaseList /></Layout>
          </RequireAuth>
        }
      />
      <Route
        path="/cases/:id"
        element={
          <RequireAuth>
            <Layout><CaseDetail /></Layout>
          </RequireAuth>
        }
      />
      <Route
        path="/audit-log"
        element={
          <RequireAuth>
            <Layout><AuditLog /></Layout>
          </RequireAuth>
        }
      />
      <Route
        path="/ingest"
        element={
          <RequireAuth>
            <Layout><Ingestion /></Layout>
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/alerts" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
