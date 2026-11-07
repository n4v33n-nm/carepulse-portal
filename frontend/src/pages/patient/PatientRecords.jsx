import React, { useState, useEffect } from 'react';
import {
  FileText,
  Calendar,
  User,
  Stethoscope,
  Activity,
  Pill,
  ShieldCheck,
  Search,
  Printer,
  Upload,
  FileCheck,
  Download,
  Trash2,
  Eye,
  AlertCircle,
  CheckCircle2,
  FileUp,
} from 'lucide-react';
import { recordService, reportService, doctorService } from '../../services/api';
import Modal from '../../components/Modal';

const PatientRecords = () => {
  const [activeTab, setActiveTab] = useState('RECORDS'); // 'RECORDS' | 'REPORTS'
  const [records, setRecords] = useState([]);
  const [reports, setReports] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  // Upload Modal State (Phase 5)
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [reportTitle, setReportTitle] = useState('');
  const [reportType, setReportType] = useState('BLOOD_TEST');
  const [reportDate, setReportDate] = useState(new Date().toISOString().split('T')[0]);
  const [reportDescription, setReportDescription] = useState('');
  const [associatedDoctorId, setAssociatedDoctorId] = useState('');
  const [uploadLoading, setUploadLoading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [uploadSuccess, setUploadSuccess] = useState('');

  // Preview Modal State
  const [previewModalOpen, setPreviewModalOpen] = useState(false);
  const [previewReport, setPreviewReport] = useState(null);
  const [previewBlobUrl, setPreviewBlobUrl] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [recRes, repRes, docRes] = await Promise.all([
        recordService.getMyRecords(),
        reportService.getMyReports().catch(() => ({ data: [] })),
        doctorService.getAllDoctors().catch(() => ({ data: [] })),
      ]);
      setRecords(recRes.data || []);
      setReports(repRes.data || []);
      setDoctors(docRes.data || []);
    } catch (err) {
      console.error('Failed to fetch records or reports:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        setUploadError('File size exceeds the 10MB limit.');
        setSelectedFile(null);
        return;
      }
      const ext = file.name.split('.').pop().toLowerCase();
      if (!['pdf', 'jpg', 'jpeg', 'png'].includes(ext)) {
        setUploadError('Unsupported file type. Please upload a PDF, JPG, or PNG.');
        setSelectedFile(null);
        return;
      }
      setUploadError('');
      setSelectedFile(file);
      if (!reportTitle) {
        setReportTitle(file.name.replace(/\.[^/.]+$/, ''));
      }
    }
  };

  const handleUploadSubmit = async (e) => {
    e.preventDefault();
    if (!selectedFile) {
      setUploadError('Please select a valid report file to upload.');
      return;
    }

    setUploadLoading(true);
    setUploadError('');
    setUploadSuccess('');

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('title', reportTitle.trim());
      formData.append('reportType', reportType);
      formData.append('reportDate', reportDate);
      if (reportDescription) {
        formData.append('description', reportDescription.trim());
      }
      if (associatedDoctorId) {
        formData.append('doctorId', associatedDoctorId);
      }

      await reportService.uploadReport(formData);
      setUploadSuccess('Medical report securely uploaded and stored.');
      await fetchData();
      setTimeout(() => {
        setIsUploadModalOpen(false);
        setUploadSuccess('');
        setSelectedFile(null);
        setReportTitle('');
        setReportDescription('');
      }, 1200);
    } catch (err) {
      setUploadError(err.response?.data?.message || 'Failed to upload report. Please check file format.');
    } finally {
      setUploadLoading(false);
    }
  };

  const handleDownload = async (report) => {
    try {
      const res = await reportService.downloadReportBlob(report.id);
      const url = window.URL.createObjectURL(new Blob([res.data], { type: report.fileType }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', report.originalFilename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to download report');
    }
  };

  const handlePreview = async (report) => {
    setPreviewReport(report);
    setPreviewLoading(true);
    setPreviewModalOpen(true);
    try {
      const res = await reportService.viewReportBlob(report.id);
      const url = window.URL.createObjectURL(new Blob([res.data], { type: report.fileType }));
      setPreviewBlobUrl(url);
    } catch (err) {
      alert('Unable to load document preview');
      setPreviewModalOpen(false);
    } finally {
      setPreviewLoading(false);
    }
  };

  const closePreviewModal = () => {
    if (previewBlobUrl) {
      window.URL.revokeObjectURL(previewBlobUrl);
      setPreviewBlobUrl(null);
    }
    setPreviewModalOpen(false);
    setPreviewReport(null);
  };

  const handleDelete = async (reportId) => {
    if (!window.confirm('Are you sure you want to delete this medical report? This cannot be undone.')) {
      return;
    }
    try {
      await reportService.deleteReport(reportId);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to delete report');
    }
  };

  const filteredRecords = records.filter((rec) => {
    if (!search) return true;
    const term = search.toLowerCase();
    return (
      rec.diagnosis?.toLowerCase().includes(term) ||
      rec.doctor?.fullName?.toLowerCase().includes(term) ||
      rec.doctor?.specialization?.toLowerCase().includes(term) ||
      rec.symptoms?.toLowerCase().includes(term) ||
      rec.treatment?.toLowerCase().includes(term) ||
      rec.consultationNotes?.toLowerCase().includes(term)
    );
  });

  const filteredReports = reports.filter((rep) => {
    if (!search) return true;
    const term = search.toLowerCase();
    return (
      rep.title?.toLowerCase().includes(term) ||
      rep.reportType?.toLowerCase().includes(term) ||
      rep.originalFilename?.toLowerCase().includes(term) ||
      rep.description?.toLowerCase().includes(term) ||
      rep.doctorName?.toLowerCase().includes(term)
    );
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '8px' }}>Medical Records & Health Documents</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Secure repository of clinical consultations, doctor notes, diagnostic blood tests, and medical scans.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setUploadError('');
              setUploadSuccess('');
              setSelectedFile(null);
              setIsUploadModalOpen(true);
            }}
          >
            <Upload size={16} /> Upload Medical Report
          </button>
          <button type="button" className="btn btn-secondary" onClick={() => window.print()}>
            <Printer size={16} /> Print Records
          </button>
        </div>
      </div>

      {/* Security and Integrity Banner */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', background: '#f8fafc', border: '1px solid var(--slate-200)', borderRadius: 'var(--radius-md)', padding: '10px 14px', marginBottom: '20px', fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
        <ShieldCheck size={18} style={{ color: 'var(--primary-600)', flexShrink: 0 }} />
        <span>
          <strong>Encrypted & Authorized Document Storage:</strong> Files are stored securely on isolated servers and only accessible by you, your authorized consulting physicians, and caregivers.
        </span>
      </div>

      {/* Tab Navigation */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderBottom: '1px solid var(--slate-200)',
          paddingBottom: '12px',
          marginBottom: '20px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className={`btn btn-sm ${activeTab === 'RECORDS' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab('RECORDS')}
          >
            <FileText size={14} /> Clinical Consultations ({records.length})
          </button>
          <button
            className={`btn btn-sm ${activeTab === 'REPORTS' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab('REPORTS')}
          >
            <FileUp size={14} /> Uploaded Reports & Scans ({reports.length})
          </button>
        </div>

        <div style={{ minWidth: '220px', maxWidth: '340px', flex: 1 }}>
          <input
            type="text"
            className="form-control"
            placeholder={activeTab === 'RECORDS' ? "Search diagnoses, doctors, or symptoms..." : "Search report title, type, or file..."}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
          Loading your health repository...
        </div>
      ) : activeTab === 'RECORDS' ? (
        filteredRecords.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
            <FileText size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
            <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>No medical records found</h3>
            <p style={{ color: 'var(--slate-500)', fontSize: '0.9375rem' }}>
              {search ? 'No records match your search criteria.' : 'Medical records will automatically populate here after your doctor concludes a consultation.'}
            </p>
          </div>
        ) : (
          <div className="timeline">
            {filteredRecords.map((rec) => (
              <div key={rec.id} className="timeline-item">
                <div className="timeline-dot" />
                <div className="timeline-card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '10px', marginBottom: '12px' }}>
                    <div>
                      <div style={{ display: 'inline-block', background: 'var(--primary-100)', color: 'var(--primary-800)', fontSize: '0.75rem', fontWeight: 700, padding: '3px 8px', borderRadius: '4px', marginBottom: '6px' }}>
                        CONSULTATION RECORD #{rec.id}
                      </div>
                      <h3 style={{ fontSize: '1.25rem', color: 'var(--slate-900)' }}>{rec.diagnosis}</h3>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--slate-500)', fontSize: '0.875rem', fontWeight: 600 }}>
                      <Calendar size={16} />
                      <span>{rec.recordDate}</span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px', color: 'var(--slate-700)', fontSize: '0.9375rem' }}>
                    <Stethoscope size={18} style={{ color: 'var(--primary-600)' }} />
                    <span>
                      <strong>Attending Physician:</strong> Dr. {rec.doctor.fullName} ({rec.doctor.specialization})
                    </span>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', background: 'var(--slate-50)', padding: '16px', borderRadius: 'var(--radius-md)', marginBottom: '16px' }}>
                    <div>
                      <h4 style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', textTransform: 'uppercase', marginBottom: '4px' }}>
                        Reported Symptoms
                      </h4>
                      <p style={{ fontSize: '0.875rem', color: 'var(--slate-800)' }}>
                        {rec.symptoms || 'None specified'}
                      </p>
                    </div>
                    <div>
                      <h4 style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', textTransform: 'uppercase', marginBottom: '4px' }}>
                        Prescribed Treatment Protocol
                      </h4>
                      <p style={{ fontSize: '0.875rem', color: 'var(--slate-800)' }}>
                        {rec.treatment || 'Observation and monitoring'}
                      </p>
                    </div>
                  </div>

                  {rec.consultationNotes && (
                    <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)', background: '#ffffff', border: '1px solid var(--slate-200)', padding: '12px 14px', borderRadius: '6px' }}>
                      <strong>Clinical Consultation Notes:</strong> {rec.consultationNotes}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        /* Uploaded Reports & Scans Tab (Phase 5) */
        filteredReports.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
            <FileUp size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
            <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>No medical reports uploaded</h3>
            <p style={{ color: 'var(--slate-500)', fontSize: '0.9375rem', marginBottom: '18px' }}>
              {search ? 'No reports match your search criteria.' : 'Upload your blood test panels, MRI/CT scans, and clinical PDFs to share them with your doctors.'}
            </p>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setIsUploadModalOpen(true)}
            >
              <Upload size={16} /> Upload First Report
            </button>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '18px' }}>
            {filteredReports.map((report) => (
              <div
                key={report.id}
                className="card"
                style={{
                  padding: '18px',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  boxShadow: 'var(--shadow-sm)',
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <div
                        style={{
                          width: '40px',
                          height: '40px',
                          borderRadius: '8px',
                          background: report.fileType.includes('pdf') ? '#fee2e2' : '#e0f2fe',
                          color: report.fileType.includes('pdf') ? '#dc2626' : '#0284c7',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                        }}
                      >
                        <FileText size={22} />
                      </div>
                      <div>
                        <h4 style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--slate-900)', margin: 0 }}>
                          {report.title}
                        </h4>
                        <span className="badge badge-primary" style={{ fontSize: '0.7rem', marginTop: '3px' }}>
                          {report.reportType.replace('_', ' ')}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div style={{ background: 'var(--slate-50)', padding: '10px 12px', borderRadius: '6px', fontSize: '0.8125rem', marginBottom: '12px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--slate-600)', marginBottom: '4px' }}>
                      <span>Report Date:</span>
                      <strong>{report.reportDate}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--slate-600)', marginBottom: '4px' }}>
                      <span>File:</span>
                      <span style={{ maxWidth: '180px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {report.originalFilename}
                      </span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--slate-600)' }}>
                      <span>Size:</span>
                      <span>{report.formattedSize}</span>
                    </div>
                  </div>

                  {report.description && (
                    <p style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginBottom: '12px', lineHeight: 1.4 }}>
                      {report.description}
                    </p>
                  )}

                  {report.doctorName && (
                    <div style={{ fontSize: '0.75rem', color: 'var(--primary-700)', fontWeight: 600, marginBottom: '12px' }}>
                      Associated Physician: Dr. {report.doctorName}
                    </div>
                  )}
                </div>

                <div style={{ borderTop: '1px solid var(--slate-100)', paddingTop: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', gap: '6px' }}>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => handlePreview(report)}
                      title="Preview Document"
                    >
                      <Eye size={14} /> Preview
                    </button>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => handleDownload(report)}
                      title="Download Securely"
                    >
                      <Download size={14} /> Download
                    </button>
                  </div>

                  <button
                    type="button"
                    style={{ background: 'none', border: 'none', color: '#ef4444', padding: '4px', cursor: 'pointer' }}
                    onClick={() => handleDelete(report.id)}
                    title="Delete Report"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )
      )}

      {/* Upload Medical Report Modal (Phase 5) */}
      <Modal
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        title="Upload Medical Document or Scan"
        maxWidth="560px"
      >
        {uploadSuccess ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <CheckCircle2 size={44} style={{ color: 'var(--teal-600)', margin: '0 auto 12px' }} />
            <p style={{ fontWeight: 600 }}>{uploadSuccess}</p>
          </div>
        ) : (
          <form onSubmit={handleUploadSubmit}>
            {uploadError && (
              <div style={{ padding: '10px 14px', background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', borderRadius: '6px', marginBottom: '16px', fontSize: '0.875rem' }}>
                {uploadError}
              </div>
            )}

            {/* Drag & Drop File Selector */}
            <div className="form-group">
              <label className="form-label">Medical Document / Scan File *</label>
              <div
                style={{
                  border: '2px dashed var(--slate-300)',
                  borderRadius: 'var(--radius-md)',
                  padding: '24px 16px',
                  textAlign: 'center',
                  background: 'var(--slate-50)',
                  cursor: 'pointer',
                  position: 'relative',
                }}
              >
                <input
                  type="file"
                  accept=".pdf,.png,.jpg,.jpeg"
                  onChange={handleFileChange}
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    height: '100%',
                    opacity: 0,
                    cursor: 'pointer',
                  }}
                  required
                />
                <FileUp size={32} style={{ color: 'var(--primary-600)', margin: '0 auto 8px' }} />
                {selectedFile ? (
                  <div>
                    <div style={{ fontWeight: 700, color: 'var(--slate-900)' }}>{selectedFile.name}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', marginTop: '2px' }}>
                      {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB • Ready to upload
                    </div>
                  </div>
                ) : (
                  <div>
                    <div style={{ fontWeight: 600, color: 'var(--slate-700)', fontSize: '0.9375rem' }}>
                      Click or drag file to upload
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', marginTop: '4px' }}>
                      Permitted formats: PDF, JPG, PNG (Max 10MB)
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Report Document Title *</label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. Complete Blood Count, Spine MRI, Ultrasound Scan"
                value={reportTitle}
                onChange={(e) => setReportTitle(e.target.value)}
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
              <div className="form-group">
                <label className="form-label">Report Category *</label>
                <select
                  className="form-control"
                  value={reportType}
                  onChange={(e) => setReportType(e.target.value)}
                >
                  <option value="BLOOD_TEST">Blood Test Panel</option>
                  <option value="SCAN_REPORT">Diagnostic Scan (MRI/CT/X-Ray)</option>
                  <option value="LAB_REPORT">General Laboratory Report</option>
                  <option value="DISCHARGE_SUMMARY">Hospital Discharge Summary</option>
                  <option value="OTHER">Other Clinical Document</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Date of Report / Test *</label>
                <input
                  type="date"
                  className="form-control"
                  value={reportDate}
                  onChange={(e) => setReportDate(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Associated Attending Physician (Optional)</label>
              <select
                className="form-control"
                value={associatedDoctorId}
                onChange={(e) => setAssociatedDoctorId(e.target.value)}
              >
                <option value="">None / General Health Record</option>
                {doctors.map((d) => (
                  <option key={d.id} value={d.id}>
                    Dr. {d.fullName} ({d.specialization})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Clinical Remarks / Notes (Optional)</label>
              <textarea
                className="form-control"
                rows="2"
                placeholder="e.g. Fasting blood test taken at City Lab. Follow-up recommended."
                value={reportDescription}
                onChange={(e) => setReportDescription(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsUploadModalOpen(false)}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={uploadLoading || !selectedFile}
              >
                {uploadLoading ? 'Uploading & Encrypting...' : 'Upload Report Securely'}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* Report Preview Modal */}
      <Modal
        isOpen={previewModalOpen}
        onClose={closePreviewModal}
        title={`Report Preview: ${previewReport?.title || ''}`}
        maxWidth="820px"
      >
        {previewLoading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
            Retrieving encrypted document stream...
          </div>
        ) : previewBlobUrl ? (
          <div>
            {previewReport?.fileType.includes('pdf') ? (
              <iframe
                src={previewBlobUrl}
                title="Report PDF Preview"
                style={{ width: '100%', height: '540px', border: 'none', borderRadius: '6px' }}
              />
            ) : (
              <div style={{ textAlign: 'center', maxHeight: '540px', overflowY: 'auto' }}>
                <img
                  src={previewBlobUrl}
                  alt={previewReport?.title}
                  style={{ maxWidth: '100%', maxHeight: '500px', borderRadius: '6px' }}
                />
              </div>
            )}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => handleDownload(previewReport)}
              >
                <Download size={14} /> Download File
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={closePreviewModal}
              >
                Close Preview
              </button>
            </div>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '30px', color: 'var(--slate-500)' }}>
            Preview unavailable for this format. Please use the Download option.
          </div>
        )}
      </Modal>
    </div>
  );
};

export default PatientRecords;
