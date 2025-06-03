package edu.hingu.project.repositories;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.hingu.project.entities.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByPropertyId(Long propertyId);
    List<Message> findBySenderId(Long senderId);
}
