package edu.hingu.project.services;

import java.util.List;

import edu.hingu.project.entities.Message;
import edu.hingu.project.entities.User;

public interface MessageService {
    List<Message> getMessagesForAgent(User agent);
    Message getById(Long id);
    void deleteMessageById(Long id);
    List<Message> getMessagesBySender(User sender);


    void save(Message message);

}
