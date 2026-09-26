import React, { useState, useEffect } from 'react';
import { Users, UserPlus, ShieldAlert, CheckCircle, Trash2, Key, AlertCircle } from 'lucide-react';
import { caregiverService } from '../../services/api';
import Modal from '../../components/Modal';

const CaregiverPage = () => {
  const [caregivers, setCaregivers] = useState([]);
  const [accessiblePatients, setAccessiblePatients] = useState([]);
  const [loading, setLoading] = useState(true);

  // Add Caregiver Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    caregiverEmail: '',
    caregiverName: '',
    relationship: 'Family Member',
    permission: 'VIEW_ALL',
  });
  const [addLoading, setAddLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [cgRes, patRes] = await Promise.all([
        caregiverService.getMyCaregivers(),
        caregiverService.getAccessiblePatients(),
      ]);
      setCaregivers(cgRes.data || []);
      setAccessiblePatients(patRes.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setAddLoading(true);

    try {
      await caregiverService.grantAccess(formData);
      setSuccess('Caregiver successfully designated.');
      fetchData();
      setTimeout(() => {
        setIsModalOpen(false);
        setSuccess('');
        setFormData({
          caregiverEmail: '',
          caregiverName: '',
          relationship: 'Family Member',
          permission: 'VIEW_ALL',
        });
      }, 1200);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to grant caregiver access');
    } finally {
      setAddLoading(false);
    }
  };

  const handleRevoke = async (id, email) => {
    if (window.confirm(`Revoke all healthcare delegate access for ${email}?`)) {
      try {
        await caregiverService.revokeAccess(id);
        fetchData();
      } catch (err) {
        alert(err.response?.data?.message || 'Failed to revoke access');
      }
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '8px' }}>Caregiver Access & Proxy Management</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Grant trusted family members or caregivers secure view-only permissions to your healthcare coordination.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => {
            setError('');
            setSuccess('');
            setIsModalOpen(true);
          }}
        >
          <UserPlus size={16} /> Grant Caregiver Access
        </button>
      </div>

      {/* Designated Caregivers Table */}
      <div className="card" style={{ marginBottom: '32px' }}>
        <div className="card-header">
          <span className="card-title">
            <Users size={20} className="text-primary" /> Caregivers Authorized for My Records
          </span>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '32px', color: 'var(--slate-500)' }}>
            Loading caregiver delegations...
          </div>
        ) : caregivers.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--slate-500)' }}>
            <Users size={36} style={{ color: 'var(--slate-300)', margin: '0 auto 12px' }} />
            <p>You have not designated any caregivers yet.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Caregiver</th>
                  <th>Relationship</th>
                  <th>Permission Level</th>
                  <th>Status</th>
                  <th>Granted On</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {caregivers.map((cg) => (
                  <tr key={cg.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--slate-900)' }}>
                        {cg.caregiverName || cg.caregiverEmail}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        {cg.caregiverEmail}
                      </div>
                    </td>
                    <td>{cg.relationship || 'Caregiver'}</td>
                    <td>
                      <span
                        style={{
                          fontSize: '0.8125rem',
                          background: 'var(--primary-50)',
                          color: 'var(--primary-700)',
                          padding: '3px 8px',
                          borderRadius: '4px',
                          fontWeight: 600,
                        }}
                      >
                        {cg.permission.replace('VIEW_', 'VIEW ')}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          cg.status === 'ACTIVE' ? 'badge-confirmed' : 'badge-cancelled'
                        }`}
                      >
                        {cg.status}
                      </span>
                    </td>
                    <td>{new Date(cg.grantedAt).toLocaleDateString()}</td>
                    <td style={{ textAlign: 'right' }}>
                      {cg.status === 'ACTIVE' ? (
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          style={{ color: '#ef4444' }}
                          onClick={() => handleRevoke(cg.id, cg.caregiverEmail)}
                        >
                          <Trash2 size={14} /> Revoke
                        </button>
                      ) : (
                        <span style={{ fontSize: '0.8125rem', color: 'var(--slate-400)' }}>Revoked</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Accessible Patients Section */}
      {accessiblePatients.length > 0 && (
        <div className="card">
          <div className="card-header">
            <span className="card-title">
              <Key size={20} className="text-teal" /> Patients Who Granted You Caregiver Access
            </span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
            {accessiblePatients.map((ap) => (
              <div
                key={ap.id}
                style={{
                  border: '1px solid var(--slate-200)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px',
                  background: 'var(--slate-50)',
                }}
              >
                <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--slate-900)' }}>
                  {ap.patient.fullName}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', marginBottom: '8px' }}>
                  Relationship: {ap.relationship} • Access: {ap.permission}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--primary-700)', fontWeight: 600 }}>
                  Authorized since {new Date(ap.grantedAt).toLocaleDateString()}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Grant Caregiver Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Designate Caregiver Access"
      >
        {success ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <CheckCircle size={44} style={{ color: 'var(--teal-600)', margin: '0 auto 12px' }} />
            <p style={{ fontWeight: 600, color: 'var(--slate-800)' }}>{success}</p>
          </div>
        ) : (
          <form onSubmit={handleAddSubmit}>
            {error && (
              <div
                style={{
                  padding: '10px 14px',
                  background: 'var(--danger-bg)',
                  border: '1px solid var(--danger-border)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--danger-text)',
                  fontSize: '0.875rem',
                  marginBottom: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <AlertCircle size={16} />
                <span>{error}</span>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="cg-email-input">Caregiver's Email Address *</label>
              <input
                id="cg-email-input"
                type="email"
                className="form-control"
                placeholder="caregiver@example.com"
                value={formData.caregiverEmail}
                onChange={(e) => setFormData({ ...formData, caregiverEmail: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="cg-name-input">Caregiver Full Name</label>
              <input
                id="cg-name-input"
                type="text"
                className="form-control"
                placeholder="e.g. John Watson"
                value={formData.caregiverName}
                onChange={(e) => setFormData({ ...formData, caregiverName: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="cg-rel-input">Relationship</label>
              <input
                id="cg-rel-input"
                type="text"
                className="form-control"
                placeholder="e.g. Spouse, Adult Child, Guardian"
                value={formData.relationship}
                onChange={(e) => setFormData({ ...formData, relationship: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="cg-perm-input">Authorized Permission Scope *</label>
              <select
                id="cg-perm-input"
                className="form-control"
                value={formData.permission}
                onChange={(e) => setFormData({ ...formData, permission: e.target.value })}
              >
                <option value="VIEW_ALL">Full Access (Appointments, Records & Prescriptions)</option>
                <option value="VIEW_APPOINTMENTS">Appointments Only</option>
                <option value="VIEW_MEDICAL_RECORDS">Medical Records Only</option>
                <option value="VIEW_PRESCRIPTIONS">Prescriptions Only</option>
              </select>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsModalOpen(false)}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={addLoading}>
                {addLoading ? 'Designating...' : 'Authorize Caregiver'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};

export default CaregiverPage;
