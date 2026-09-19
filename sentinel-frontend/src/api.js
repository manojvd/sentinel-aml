const BASE_URL = 'http://localhost:8090/api/v1';

export class ApiError extends Error {
  constructor(message, status, details) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

async function request(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (networkErr) {
    throw new ApiError(
      'Could not reach the Sentinel backend. Is it running on http://localhost:8090?',
      0,
      undefined,
    );
  }

  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!res.ok) {
    const message = data?.message || res.statusText || `Request failed (${res.status})`;
    throw new ApiError(message, res.status, data?.details);
  }

  return data;
}

function toQuery(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value);
    }
  });
  const qs = search.toString();
  return qs ? `?${qs}` : '';
}

export const api = {
  login(username, password) {
    return request('/auth/login', { method: 'POST', body: { username, password } });
  },

  getAlerts(token, { status, page = 0, size = 10, sort } = {}) {
    return request(`/alerts${toQuery({ status, page, size, sort })}`, { token });
  },

  getAlert(token, id) {
    return request(`/alerts/${id}`, { token });
  },

  dispositionAlert(token, id, action, reason) {
    return request(`/alerts/${id}/disposition`, {
      method: 'POST',
      token,
      body: { action, reason },
    });
  },

  getCases(token, { page = 0, size = 10 } = {}) {
    return request(`/cases${toQuery({ page, size })}`, { token });
  },

  getCase(token, id) {
    return request(`/cases/${id}`, { token });
  },

  createCase(token, body) {
    return request('/cases', { method: 'POST', token, body });
  },

  updateCase(token, id, body) {
    return request(`/cases/${id}`, { method: 'PUT', token, body });
  },

  addCaseNote(token, id, note) {
    return request(`/cases/${id}/notes`, { method: 'POST', token, body: { note } });
  },

  getAuditLog(token, { page = 0, size = 20 } = {}) {
    return request(`/audit-log${toQuery({ page, size })}`, { token });
  },

  async ingestCsv(token, kind, file) {
    const form = new FormData();
    form.append('file', file);

    let res;
    try {
      res = await fetch(`${BASE_URL}/ingest/${kind}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form,
      });
    } catch {
      throw new ApiError(
        'Could not reach the Sentinel backend. Is it running on http://localhost:8090?',
        0,
        undefined,
      );
    }

    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
      throw new ApiError(data?.message || res.statusText || `Request failed (${res.status})`, res.status, data?.details);
    }
    return data; // { accepted, rejected, errors, elapsedMillis }
  },
};

export function describeError(err) {
  if (!(err instanceof ApiError)) return err?.message || 'Something went wrong';
  const extra = err.details?.length ? `: ${err.details.join(', ')}` : '';
  return `${err.message}${extra}`;
}
