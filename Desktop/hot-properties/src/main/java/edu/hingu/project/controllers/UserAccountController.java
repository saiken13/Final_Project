package edu.hingu.project.controllers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.hingu.project.entities.User;
import edu.hingu.project.services.AuthService;
import edu.hingu.project.services.UserService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class UserAccountController {

    private final AuthService authService;
    private final UserService userService;

    public UserAccountController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping({"/", "/index"})
    public String showIndex() {
        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    

@PostMapping("/login")
public String processLogin(@ModelAttribute("user") User user, HttpServletResponse response, Model model) {
    try {
        // ✅ Generate JWT and return Spring's ResponseCookie
        ResponseCookie jwtCookie = authService.loginAndCreateJwtCookieByEmail(user);

        // ✅ Add cookie to response header
        response.addHeader("Set-Cookie", jwtCookie.toString());

        return "redirect:/dashboard";
    } catch (BadCredentialsException e) {
        model.addAttribute("error", "Invalid email or password");
        return "login";
    }
}

    @GetMapping("/logout")
    @PreAuthorize("hasAnyRole('BUYER', 'AGENT', 'ADMIN')")
    public String logout(HttpServletResponse response) {
        authService.clearJwtCookie(response);
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('BUYER', 'AGENT', 'ADMIN')")
    public String showDashboard(Model model) {
        userService.prepareDashboardModel(model);
        return "dashboard";
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('BUYER', 'AGENT', 'ADMIN')")
    public String showProfile(Model model) {
        userService.prepareProfileModel(model);
        return "profile";
    }

    @GetMapping("/edit-profile")
    @PreAuthorize("isAuthenticated()")
    public String showEditProfileForm(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("user", currentUser);
        return "edit-profile";
    }



    @PostMapping("/edit-profile")
    @PreAuthorize("hasAnyRole('BUYER', 'AGENT', 'ADMIN')")
    public String updateSettings(@ModelAttribute("user") User updatedUser,
                                 @RequestParam(required = false) String password,
                                 @RequestParam(required = false) List<Long> addIds,
                                 @RequestParam(required = false) List<Long> removeIds,
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            User actualUser = userService.getCurrentUser();
            actualUser.setFirstName(updatedUser.getFirstName());
            actualUser.setLastName(updatedUser.getLastName());
            actualUser.setEmail(updatedUser.getEmail());

            userService.updateUserSettings(actualUser, password, addIds, removeIds);

            if (file != null && !file.isEmpty()) {
                String filename = userService.storeProfilePicture(actualUser.getId(), file);
                actualUser.setProfilePicture(filename);
                userService.updateUser(actualUser);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Account updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update account: " + ex.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/manager/team")
    public String showMyTeam(Model model) {
        model.addAttribute("team", userService.getTeamForCurrentManager());
        return "my_team";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam("selectedRoles") List<String> roleNames,
                               @RequestParam(value = "file", required = false) MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        try {
            User savedUser = userService.registerNewUser(user, roleNames);

            if (file != null && !file.isEmpty()) {
                String filename = userService.storeProfilePicture(savedUser.getId(), file);
                savedUser.setProfilePicture(filename);
                userService.updateUser(savedUser);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @PostMapping("/users/{id}/upload-profile-picture")
    @PreAuthorize("hasAnyRole('BUYER', 'AGENT', 'ADMIN')")
    public String uploadProfilePicture(@PathVariable Long id,
                                       @RequestParam("file") MultipartFile file,
                                       RedirectAttributes redirectAttributes) {
        try {
            String filename = userService.storeProfilePicture(id, file);
            redirectAttributes.addFlashAttribute("message", "Profile picture uploaded: " + filename);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/profile-pictures/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveProfilePicture(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/profile-pictures/").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/admin/create-agent")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateAgentForm(Model model) {
        model.addAttribute("user", new User());
        return "create-agent";
    }

    @PostMapping("/admin/create-agent")
    @PreAuthorize("hasRole('ADMIN')")
    public String createAgent(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            userService.registerNewUser(user, Collections.singletonList("AGENT"));
            redirectAttributes.addFlashAttribute("successMessage", "Agent created successfully.");
            return "redirect:/dashboard";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create agent: " + e.getMessage());
            return "redirect:/admin/create-agent";
        }
    }



    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewAllUsers(Model model) {
        List<User> allUsers = userService.getAllUsers();

        for (User user : allUsers) {
            System.out.println("User: " + user.getEmail() + " → Roles: " + user.getRoles());
        }

        model.addAttribute("users", allUsers);
        return "manage-users";
    }

}
