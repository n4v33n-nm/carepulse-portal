package com.carepulse.repository;

import com.carepulse.entity.Conversation;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByPatientOrderByUpdatedAtDesc(Patient patient);

    List<Conversation> findByDoctorOrderByUpdatedAtDesc(Doctor doctor);

    List<Conversation> findByDoctorAndStatusOrderByUpdatedAtDesc(Doctor doctor, String status);

    List<Conversation> findAllByOrderByUpdatedAtDesc();

    List<Conversation> findByStatusOrderByUpdatedAtDesc(String status);

    long countByStatus(String status);

    long countByDoctorAndStatus(Doctor doctor, String status);

    @Query("SELECT c FROM Conversation c WHERE c.doctor = :doctor AND c.status = 'OPEN' ORDER BY c.updatedAt DESC")
    List<Conversation> findUnansweredConversationsForDoctor(@Param("doctor") Doctor doctor);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.status = 'OPEN'")
    long countUnansweredConversations();
}

