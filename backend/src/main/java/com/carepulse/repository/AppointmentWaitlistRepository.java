package com.carepulse.repository;

import com.carepulse.entity.AppointmentWaitlist;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentWaitlistRepository extends JpaRepository<AppointmentWaitlist, Long> {

    List<AppointmentWaitlist> findByPatientOrderByCreatedAtDesc(Patient patient);

    List<AppointmentWaitlist> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<AppointmentWaitlist> findByDoctorAndPreferredDateAndStatusOrderByCreatedAtAsc(Doctor doctor, LocalDate preferredDate, String status);

    @Query("SELECT w FROM AppointmentWaitlist w WHERE w.doctor.id = :doctorId AND w.preferredDate = :date AND w.status = :status ORDER BY w.createdAt ASC")
    List<AppointmentWaitlist> findWaitingForDoctorAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date, @Param("status") String status);

    @Query("SELECT w FROM AppointmentWaitlist w WHERE (w.doctor.id = :doctorId OR (w.doctor IS NULL AND LOWER(w.specialization) = LOWER(:specialization))) AND w.preferredDate = :date AND w.status = 'WAITING' ORDER BY w.createdAt ASC")
    List<AppointmentWaitlist> findEligibleWaitingCandidates(@Param("doctorId") Long doctorId, @Param("specialization") String specialization, @Param("date") LocalDate date);

    List<AppointmentWaitlist> findByStatusAndPreferredDateLessThan(String status, LocalDate date);

    Optional<AppointmentWaitlist> findByPatientIdAndDoctorIdAndPreferredDateAndStatus(Long patientId, Long doctorId, LocalDate preferredDate, String status);

    long countByStatus(String status);
}
