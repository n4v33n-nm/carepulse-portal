package com.carepulse.repository;

import com.carepulse.entity.Patient;
import com.carepulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUser(User user);
    Optional<Patient> findByUserEmail(String email);
    Optional<Patient> findByUserId(Long userId);
}
