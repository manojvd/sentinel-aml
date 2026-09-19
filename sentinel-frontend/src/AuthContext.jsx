import { createContext, useCallback, useContext, useState } from 'react';
import { api } from './api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Deliberately kept in memory only (not localStorage) per security preference.
  // A page refresh will log the user out — acceptable for this MVP.
  const [auth, setAuth] = useState(null); // { token, username, role }

  const login = useCallback(async (username, password) => {
    const data = await api.login(username, password);
    setAuth({ token: data.token, username: data.username, role: data.role });
    return data;
  }, []);

  const logout = useCallback(() => setAuth(null), []);

  return (
    <AuthContext.Provider value={{ auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

export function canDisposition(role) {
  return role === 'ADMIN' || role === 'COMPLIANCE_ANALYST';
}

export function canSeeAuditLog(role) {
  return role === 'ADMIN' || role === 'COMPLIANCE_ANALYST';
}

// The backend rejects VIEWER on every /cases endpoint (including plain GETs)
// with 403, not just the write actions — so the whole Cases area is
// analyst/admin-only, not just its edit actions.
export function canAccessCases(role) {
  return role === 'ADMIN' || role === 'COMPLIANCE_ANALYST';
}

// Matches the backend's @PreAuthorize on IngestionController.
export function canIngestData(role) {
  return role === 'ADMIN' || role === 'COMPLIANCE_ANALYST';
}
