import React, { useState, useEffect } from 'react';
import { Shield, Clock, Search, Filter } from 'lucide-react';
import { adminService } from '../../services/api';

const AdminAuditLogs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('ALL');

  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    try {
      const res = await adminService.getAuditLogs();
      setLogs(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const filteredLogs = logs.filter((log) => {
    const matchesSearch =
      (log.userEmail && log.userEmail.toLowerCase().includes(search.toLowerCase())) ||
      (log.targetResource && log.targetResource.toLowerCase().includes(search.toLowerCase())) ||
      (log.details && log.details.toLowerCase().includes(search.toLowerCase()));

    const matchesAction = actionFilter === 'ALL' || log.action === actionFilter;
    return matchesSearch && matchesAction;
  });

  const getActionBadge = (action) => {
    if (action.includes('LOGIN')) return 'badge-confirmed';
    if (action.includes('CANCEL') || action.includes('REVOKE')) return 'badge-cancelled';
    if (action.includes('CREATE') || action.includes('GRANTED')) return 'badge-pending';
    return 'badge-role';
  };

  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Compliance & Security Audit Trail</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Immutable system audit logs recording authentication events, patient record views, prescription grants, and appointment updates.
        </p>
      </div>

      <div className="card">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '12px' }}>
          <div style={{ display: 'flex', gap: '10px', flex: 1, maxWidth: '500px' }}>
            <input
              type="text"
              className="form-control form-control-sm"
              placeholder="Search user, resource, or details..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="form-control form-control-sm"
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value)}
            >
              <option value="ALL">All Actions</option>
              <option value="LOGIN">LOGIN</option>
              <option value="LOGOUT">LOGOUT</option>
              <option value="APPOINTMENT_CREATED">APPOINTMENT_CREATED</option>
              <option value="APPOINTMENT_STATUS_UPDATED">APPOINTMENT_STATUS_UPDATED</option>
              <option value="MEDICAL_RECORD_VIEWED">MEDICAL_RECORD_VIEWED</option>
              <option value="MEDICAL_RECORD_CREATED">MEDICAL_RECORD_CREATED</option>
              <option value="PRESCRIPTION_CREATED">PRESCRIPTION_CREATED</option>
              <option value="CAREGIVER_ACCESS_GRANTED">CAREGIVER_ACCESS_GRANTED</option>
              <option value="CAREGIVER_ACCESS_REVOKED">CAREGIVER_ACCESS_REVOKED</option>
            </select>
          </div>

          <button type="button" className="btn btn-secondary btn-sm" onClick={fetchLogs}>
            Refresh Logs
          </button>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '36px', color: 'var(--slate-500)' }}>
            Loading audit records...
          </div>
        ) : filteredLogs.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--slate-500)' }}>
            No audit logs found matching criteria.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>User / Actor</th>
                  <th>Action</th>
                  <th>Target Resource</th>
                  <th>Audit Details</th>
                </tr>
              </thead>
              <tbody>
                {filteredLogs.map((log) => (
                  <tr key={log.id}>
                    <td style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={12} />
                        {new Date(log.timestamp).toLocaleString()}
                      </div>
                    </td>
                    <td style={{ fontWeight: 600, color: 'var(--slate-900)' }}>
                      {log.userEmail || 'System'}
                    </td>
                    <td>
                      <span className={`badge ${getActionBadge(log.action)}`}>
                        {log.action}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.8125rem', color: 'var(--primary-700)', fontWeight: 600 }}>
                      {log.targetResource || '—'}
                    </td>
                    <td style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', maxWidth: '340px' }}>
                      {log.details}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminAuditLogs;
