package com.carepulse.repository;

import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyRequest;
import com.carepulse.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {

    List<EmergencyRequest> findByPatientOrderByRequestTimeDesc(Patient patient);

    List<EmergencyRequest> findByAssignedDoctorOrderByRequestTimeDesc(Doctor doctor);

    List<EmergencyRequest> findByOrderByRequestTimeDesc();

    long countByStatus(String status);

    long countByAssignedDoctorAndStatusIn(Doctor doctor, List<String> statuses);

    long countByAssignedDoctorAndRequestTimeBetween(Doctor doctor, LocalDateTime start, LocalDateTime end);
}
