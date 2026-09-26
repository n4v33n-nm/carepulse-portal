package com.carepulse.repository;

import com.carepulse.entity.CaregiverAccess;
import com.carepulse.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaregiverAccessRepository extends JpaRepository<CaregiverAccess, Long> {
    List<CaregiverAccess> findByPatientOrderByGrantedAtDesc(Patient patient);
    List<CaregiverAccess> findByPatientIdOrderByGrantedAtDesc(Long patientId);
    List<CaregiverAccess> findByCaregiverEmailAndStatus(String caregiverEmail, String status);
    Optional<CaregiverAccess> findByPatientIdAndCaregiverEmailAndStatus(Long patientId, String caregiverEmail, String status);
}
