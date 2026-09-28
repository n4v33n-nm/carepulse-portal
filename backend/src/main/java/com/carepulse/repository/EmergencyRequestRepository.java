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

    List<EmergencyRequest> findByStatusOrderByPriorityDescRequestTimeAsc(String status);

    List<EmergencyRequest> findByStatusAndAssignedTimeBefore(String status, LocalDateTime threshold);

    List<EmergencyRequest> findByRequestTimeBetweenOrderByRequestTimeDesc(LocalDateTime start, LocalDateTime end);

    long countByStatus(String status);

    long countByRequestTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndRequestTimeBetween(String status, LocalDateTime start, LocalDateTime end);

    long countByAssignedDoctorAndStatusIn(Doctor doctor, List<String> statuses);

    long countByAssignedDoctorAndRequestTimeBetween(Doctor doctor, LocalDateTime start, LocalDateTime end);

    long countByAssignedDoctorAndStatusAndRequestTimeBetween(Doctor doctor, String status, LocalDateTime start, LocalDateTime end);

    long countByAssignedDoctorAndStatusInAndRequestTimeBetween(Doctor doctor, List<String> statuses, LocalDateTime start, LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(e) FROM EmergencyRequest e WHERE e.assignedDoctor.id = :doctorId AND e.status IN ('ASSIGNED', 'IN_PROGRESS')")
    long countActiveEmergenciesForDoctor(@org.springframework.data.repository.query.Param("doctorId") Long doctorId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(e) FROM EmergencyRequest e WHERE e.assignedDoctor.id = :doctorId AND CAST(e.requestTime AS date) = :date AND e.status != 'CANCELLED'")
    long countEmergencyCasesForDoctorOnDate(@org.springframework.data.repository.query.Param("doctorId") Long doctorId, @org.springframework.data.repository.query.Param("date") java.time.LocalDate date);

    boolean existsByAssignedDoctorIdAndPatientId(Long doctorId, Long patientId);
}
