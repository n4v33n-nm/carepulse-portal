package com.carepulse.repository;

import com.carepulse.entity.AppointmentReassignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentReassignmentRepository extends JpaRepository<AppointmentReassignment, Long> {

    List<AppointmentReassignment> findByAppointmentId(Long appointmentId);

    Optional<AppointmentReassignment> findFirstByAppointmentIdAndPatientConsentStatusOrderByReassignedAtDesc(
            Long appointmentId, String patientConsentStatus);

    List<AppointmentReassignment> findByAppointmentPatientIdAndPatientConsentStatusOrderByReassignedAtDesc(
            Long patientId, String status);

    List<AppointmentReassignment> findByAppointmentPatientIdOrderByReassignedAtDesc(Long patientId);

    List<AppointmentReassignment> findByReplacementDoctorIdOrderByReassignedAtDesc(Long doctorId);

    List<AppointmentReassignment> findByOriginalDoctorIdOrderByReassignedAtDesc(Long doctorId);

    List<AppointmentReassignment> findByDoctorLeaveId(Long leaveId);

    @Query("SELECT r FROM AppointmentReassignment r WHERE r.appointment.id = :appointmentId ORDER BY r.reassignedAt DESC")
    List<AppointmentReassignment> findAllByAppointmentIdOrderByReassignedAtDesc(@Param("appointmentId") Long appointmentId);
}

