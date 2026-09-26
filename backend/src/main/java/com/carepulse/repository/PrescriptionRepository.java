package com.carepulse.repository;

import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import com.carepulse.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientOrderByIssuedDateDesc(Patient patient);
    List<Prescription> findByPatientIdOrderByIssuedDateDesc(Long patientId);
    List<Prescription> findByDoctorOrderByIssuedDateDesc(Doctor doctor);
    List<Prescription> findByDoctorIdOrderByIssuedDateDesc(Long doctorId);
    List<Prescription> findByMedicalRecordId(Long medicalRecordId);
}
