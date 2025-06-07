package edu.hingu.project.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.hingu.project.entities.Message;
import edu.hingu.project.entities.User;
import edu.hingu.project.services.MessageService;
import edu.hingu.project.services.UserService;

@Controller
@RequestMapping("/messages")
@PreAuthorize("hasRole('AGENT')")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    public MessageController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    @GetMapping("")
    public String redirectToAgentMessages() {
        return "redirect:/messages/agent";
    }
    

    @GetMapping("/agent")
    public String viewMessagesForAgent(Model model) {
        User currentAgent = userService.getCurrentUser();
        List<Message> messages = messageService.getMessagesForAgent(currentAgent);
        model.addAttribute("messages", messages);
        return "agent-messages"; // the HTML Thymeleaf page
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
}
