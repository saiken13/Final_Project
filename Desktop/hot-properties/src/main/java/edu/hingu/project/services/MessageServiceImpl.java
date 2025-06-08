package edu.hingu.project.services;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.hingu.project.entities.Message;
import edu.hingu.project.entities.User;
import edu.hingu.project.repositories.MessageRepository;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public Message getById(Long id) {
        return messageRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteMessageById(Long id) {
        messageRepository.deleteById(id);
    }

    @Override
    public void save(Message message) {
    messageRepository.save(message);
}

    @Override
    public List<Message> getMessagesForAgent(User agent) {
        List<Message> messages = messageRepository.findByPropertyOwner(agent);
        System.out.println(" Messages fetched for " + agent.getEmail() + ": " + messages.size());
        return messages;
    }

    @Override
    public List<Message> getMessagesBySender(User sender) {
        return messageRepository.findBySenderId(sender.getId());
    }



}
