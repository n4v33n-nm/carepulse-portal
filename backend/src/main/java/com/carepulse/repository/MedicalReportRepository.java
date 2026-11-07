package com.carepulse.repository;

import com.carepulse.entity.MedicalReport;
import com.carepulse.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalReportRepository extends JpaRepository<MedicalReport, Long> {

    List<MedicalReport> findByPatientOrderByReportDateDesc(Patient patient);

    List<MedicalReport> findByPatientIdOrderByReportDateDesc(Long patientId);

    List<MedicalReport> findByDoctorIdOrderByReportDateDesc(Long doctorId);

    boolean existsByPatientId(Long patientId);
}

