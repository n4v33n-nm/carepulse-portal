package com.carepulse.repository;

import com.carepulse.entity.Doctor;
import com.carepulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUser(User user);
    Optional<Doctor> findByUserEmail(String email);
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findBySpecializationContainingIgnoreCase(String specialization);
    List<Doctor> findByFullNameContainingIgnoreCase(String name);
    List<Doctor> findByAvailabilityStatus(String status);
}
