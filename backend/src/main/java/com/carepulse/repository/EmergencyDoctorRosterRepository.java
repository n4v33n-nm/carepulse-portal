package com.carepulse.repository;

import com.carepulse.entity.EmergencyDoctorRoster;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyDoctorRosterRepository extends JpaRepository<EmergencyDoctorRoster, Long> {

    List<EmergencyDoctorRoster> findByRosterDateOrderByShiftStartAsc(LocalDate rosterDate);

    List<EmergencyDoctorRoster> findByRosterDateBetweenOrderByRosterDateAscShiftStartAsc(LocalDate start, LocalDate end);

    List<EmergencyDoctorRoster> findByDoctorIdAndRosterDate(Long doctorId, LocalDate rosterDate);

    Optional<EmergencyDoctorRoster> findByDoctorIdAndRosterDateAndShiftName(Long doctorId, LocalDate rosterDate, String shiftName);

    boolean existsByDoctorIdAndRosterDateAndShiftName(Long doctorId, LocalDate rosterDate, String shiftName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM EmergencyDoctorRoster r WHERE r.id = :id")
    Optional<EmergencyDoctorRoster> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT r FROM EmergencyDoctorRoster r JOIN FETCH r.doctor d " +
           "WHERE (r.rosterDate = :today OR (r.rosterDate = :yesterday AND r.shiftStart > r.shiftEnd)) " +
           "AND r.dutyStatus = 'EMERGENCY_DUTY' " +
           "AND r.doctorAvailabilityStatus = 'AVAILABLE'")
    List<EmergencyDoctorRoster> findAvailableEmergencyRosters(@Param("today") LocalDate today, @Param("yesterday") LocalDate yesterday);

    @Query("SELECT r FROM EmergencyDoctorRoster r WHERE r.doctor.id = :doctorId AND (r.rosterDate = :today OR (r.rosterDate = :yesterday AND r.shiftStart > r.shiftEnd)) AND r.dutyStatus = 'EMERGENCY_DUTY'")
    List<EmergencyDoctorRoster> findTodayDutyForDoctor(@Param("doctorId") Long doctorId, @Param("today") LocalDate today, @Param("yesterday") LocalDate yesterday);
}
