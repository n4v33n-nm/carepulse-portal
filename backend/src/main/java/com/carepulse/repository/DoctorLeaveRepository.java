package com.carepulse.repository;

import com.carepulse.entity.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {

    List<DoctorLeave> findByDoctorIdOrderByStartDateDesc(Long doctorId);

    List<DoctorLeave> findByStatusOrderByCreatedAtDesc(String status);

    List<DoctorLeave> findAllByOrderByCreatedAtDesc();

    List<DoctorLeave> findByDoctorIdAndStatus(Long doctorId, String status);

    @Query("SELECT COUNT(l) > 0 FROM DoctorLeave l WHERE l.doctor.id = :doctorId AND l.status IN ('PENDING', 'APPROVED') AND l.startDate <= :endDate AND l.endDate >= :startDate")
    boolean hasOverlappingLeave(@Param("doctorId") Long doctorId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(l) > 0 FROM DoctorLeave l WHERE l.doctor.id = :doctorId AND l.status = 'APPROVED' AND :targetDate BETWEEN l.startDate AND l.endDate")
    boolean isDoctorOnApprovedLeave(@Param("doctorId") Long doctorId,
                                    @Param("targetDate") LocalDate targetDate);

    @Query("SELECT l FROM DoctorLeave l WHERE l.doctor.id = :doctorId AND l.status = 'APPROVED' AND :targetDate BETWEEN l.startDate AND l.endDate")
    List<DoctorLeave> findApprovedLeavesOnDate(@Param("doctorId") Long doctorId,
                                              @Param("targetDate") LocalDate targetDate);
}

