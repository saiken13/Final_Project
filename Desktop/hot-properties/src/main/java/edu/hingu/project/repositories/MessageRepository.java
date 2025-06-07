package edu.hingu.project.repositories;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.hingu.project.entities.Message;
import edu.hingu.project.entities.User;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByPropertyId(Long propertyId);
    List<Message> findBySenderId(Long senderId);
    List<Message> findByPropertyOwner(User agent);
}
