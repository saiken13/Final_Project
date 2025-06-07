package edu.hingu.project.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.hingu.project.entities.Message;
import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;
import edu.hingu.project.services.MessageService;
import edu.hingu.project.services.PropertyService;
import edu.hingu.project.services.UserService;

@Controller
@RequestMapping("/messages")
@PreAuthorize("hasRole('AGENT')")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final PropertyService propertyService;


    public MessageController(MessageService messageService, UserService userService, PropertyService propertyService) {
        this.messageService = messageService;
        this.userService = userService;
        this.propertyService = propertyService;
    }
    

    @GetMapping("")
    public String redirectToAgentMessages() {
        return "redirect:/messages/agent";
    }
    

    @GetMapping("/agent")
    public String viewMessagesForAgent(Model model) {
        User currentAgent = userService.getCurrentUser();
        System.out.println("👤 Current Agent: " + currentAgent.getEmail());

        List<Message> messages = messageService.getMessagesForAgent(currentAgent);
        System.out.println("📩 Messages found: " + messages.size());
        messages.forEach(m -> System.out.println("Message from " + m.getSender().getName() + " - Property: " + m.getProperty().getTitle()));

        model.addAttribute("messages", messages);
        return "agent-messages";
    }


    @GetMapping("/buyer")
    @PreAuthorize("hasRole('BUYER')")
    public String viewMessagesForBuyer(Model model) {
        User buyer = userService.getCurrentUser();
        List<Message> messages = messageService.getMessagesBySender(buyer);
        model.addAttribute("messages", messages);
        return "buyer-messages"; // create this HTML page
    }


    @GetMapping("/view/{id}")
    public String viewSingleMessage(@PathVariable Long id, Model model) {
        Message message = messageService.getById(id);
        model.addAttribute("message", message);
        return "view-messages"; // optional page to show message details
    }

    @PostMapping("/delete/{id}")
    public String deleteMessage(@PathVariable Long id) {
        messageService.deleteMessageById(id);
        return "redirect:/messages/agent";
    }

    
    @PostMapping("/send/{propertyId}")
    @PreAuthorize("hasRole('BUYER')")
    public String sendMessageToAgent(@PathVariable Long propertyId,
                                    @RequestParam("content") String content) {
        User sender = userService.getCurrentUser();
        Property property = propertyService.getById(propertyId).orElse(null);

        if (property == null) {
            System.err.println("❌ Property with ID " + propertyId + " not found!");
            return "redirect:/browse";
        }
        
        if (sender == null) {
            System.err.println("❌ Sender is null (unauthenticated or not logged in?)");
            return "redirect:/browse";
        }        

        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        message.setProperty(property);
        message.setTimestamp(LocalDateTime.now());

        System.out.println("📩 Message from " + sender.getEmail() + " about property " + property.getId() + ": " + content);
        messageService.save(message);

        return "redirect:/details/" + propertyId;
    }
    }
    

    


