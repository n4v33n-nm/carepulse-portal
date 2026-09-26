import React, { useState, useEffect } from 'react';
import {
  Users,
  Stethoscope,
  Calendar,
  Shield,
  Activity,
  UserCheck,
  UserX,
  Search,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';
import { adminService } from '../../services/api';

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [userSearch, setUserSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [actionLoading, setActionLoading] = useState(null);

  useEffect(() => {
    fetchAdminData();
  }, []);

  const fetchAdminData = async () => {
    setLoading(true);
    try {
      const [statsRes, usersRes] = await Promise.all([
        adminService.getDashboardStats(),
        adminService.getAllUsers(),
      ]);
      setStats(statsRes.data);
      setUsers(usersRes.data || []);
    } catch (err) {
      console.error('Failed to load admin stats:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (userId) => {
    setActionLoading(userId);
    try {
      await adminService.toggleUserStatus(userId);
      setUsers((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, active: !u.active } : u))
      );
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update user status');
    } finally {
      setActionLoading(null);
    }
  };

  const filteredUsers = users.filter((u) => {
    const matchesSearch =
      u.email.toLowerCase().includes(userSearch.toLowerCase()) ||
      u.role.toLowerCase().includes(userSearch.toLowerCase());
    const matchesRole = roleFilter === 'ALL' || u.role === roleFilter;
    return matchesSearch && matchesRole;
  });

  return (
    <div>
      <div style={{ marginBottom: '28px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>System Administrator Dashboard</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          High-level operational metrics, role governance, and user account provisioning.
        </p>
      </div>

      {/* KPI Stats Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-primary">
            <Users size={26} />
          </div>
          <div>
            <div className="stat-val">{stats?.totalPatients ?? '—'}</div>
            <div className="stat-label">Registered Patients</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-teal">
            <Stethoscope size={26} />
          </div>
          <div>
            <div className="stat-val">{stats?.totalDoctors ?? '—'}</div>
            <div className="stat-label">Licensed Physicians</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-indigo">
            <Calendar size={26} />
          </div>
          <div>
            <div className="stat-val">{stats?.totalAppointments ?? '—'}</div>
            <div className="stat-label">Total Consultations</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-amber">
            <Activity size={26} />
          </div>
          <div>
            <div className="stat-val">{stats?.activeUsers ?? '—'}</div>
            <div className="stat-label">Active Accounts</div>
          </div>
        </div>
      </div>

      {/* Consultations Breakdown */}
      {stats?.appointmentsByStatus && (
        <div className="card" style={{ marginBottom: '28px' }}>
          <div className="card-header">
            <span className="card-title">
              <Calendar size={20} className="text-primary" /> Consultations by Status
            </span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '16px' }}>
            {Object.entries(stats.appointmentsByStatus).map(([status, count]) => (
              <div
                key={status}
                style={{
                  background: 'var(--slate-50)',
                  border: '1px solid var(--slate-200)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--slate-900)' }}>
                  {count}
                </div>
                <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--slate-500)', marginTop: '4px' }}>
                  {status}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* User Management */}
      <div className="card" id="users">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '12px' }}>
          <span className="card-title">
            <Shield size={20} className="text-primary" /> User Governance & Access Control
          </span>

          <div style={{ display: 'flex', gap: '10px' }}>
            <input
              type="text"
              className="form-control form-control-sm"
              style={{ width: '220px' }}
              placeholder="Search by email..."
              value={userSearch}
              onChange={(e) => setUserSearch(e.target.value)}
            />
            <select
              className="form-control form-control-sm"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
            >
              <option value="ALL">All Roles</option>
              <option value="PATIENT">Patients</option>
              <option value="DOCTOR">Doctors</option>
              <option value="ADMIN">Admins</option>
            </select>
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '36px', color: 'var(--slate-500)' }}>
            Loading users...
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Communication Tone</th>
                  <th>Status</th>
                  <th>Created At</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((u) => (
                  <tr key={u.id}>
                    <td>#{u.id}</td>
                    <td style={{ fontWeight: 600, color: 'var(--slate-900)' }}>{u.email}</td>
                    <td>
                      <span className="badge badge-role">{u.role}</span>
                    </td>
                    <td>{u.communicationPreference || 'SUPPORTIVE'}</td>
                    <td>
                      <span className={`badge ${u.active ? 'badge-confirmed' : 'badge-cancelled'}`}>
                        {u.active ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td>{new Date(u.createdAt).toLocaleDateString()}</td>
                    <td style={{ textAlign: 'right' }}>
                      {u.role !== 'ADMIN' && (
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          style={{ color: u.active ? '#ef4444' : '#10b981' }}
                          onClick={() => handleToggleStatus(u.id)}
                          disabled={actionLoading === u.id}
                        >
                          {u.active ? (
                            <>
                              <UserX size={14} /> Disable
                            </>
                          ) : (
                            <>
                              <UserCheck size={14} /> Enable
                            </>
                          )}
                        </button>
                      )}
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

export default AdminDashboard;
