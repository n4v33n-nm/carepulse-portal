package com.carepulse.repository;

import com.carepulse.entity.Doctor;
import com.carepulse.entity.MedicalRecord;
import com.carepulse.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientOrderByRecordDateDesc(Patient patient);
    List<MedicalRecord> findByPatientIdOrderByRecordDateDesc(Long patientId);
    List<MedicalRecord> findByDoctorOrderByRecordDateDesc(Doctor doctor);
    List<MedicalRecord> findByDoctorIdOrderByRecordDateDesc(Long doctorId);
    List<MedicalRecord> findByPatientIdAndDoctorIdOrderByRecordDateDesc(Long patientId, Long doctorId);
    boolean existsByPatientIdAndDoctorId(Long patientId, Long doctorId);
}
