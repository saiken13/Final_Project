package edu.hingu.project.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.Role;
import edu.hingu.project.entities.User;
import edu.hingu.project.repositories.PropertyRepository;
import edu.hingu.project.repositories.RoleRepository;
import edu.hingu.project.repositories.UserRepository;
import edu.hingu.project.utils.CurrentUserContext;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private PropertyRepository propertyRepository;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private CurrentUserContext getCurrentUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new CurrentUserContext(user, auth);
    }

    @Override
    public void prepareDashboardModel(Model model) {
        CurrentUserContext context = getCurrentUserContext();
        model.addAttribute("user", context.user());
        model.addAttribute("authorization", context.auth());
    }

    @Override
    public void prepareProfileModel(Model model) {
        model.addAttribute("user", getCurrentUserContext().user());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public User registerAgent(User agent) {
        Role agentRole = roleRepository.findByName("AGENT")
            .orElseThrow(() -> new RuntimeException("Role AGENT not found"));
        agent.setRoles(Set.of(agentRole));
        agent.setEnabled(true);
        return userRepository.save(agent);
    }



    @Override
    public void prepareSettingsModel(Model model) {
        CurrentUserContext context = getCurrentUserContext();
        User user = context.user();
        Authentication auth = context.auth();

        model.addAttribute("user", user);

        boolean isAgent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));

        if (isAgent) {
            List<User> currentEmployees = userRepository.findByAgent(user);
            List<User> availableUsers = userRepository.findByAgentIsNull().stream()
                    .filter(u -> !u.getEmail().equals(user.getEmail()))
                    .collect(Collectors.toList());

            model.addAttribute("currentEmployees", currentEmployees);
            model.addAttribute("availableUsers", availableUsers);
        }
    }

    @Override
    public void updateUserSettings(User updatedUser, String password, List<Long> addIds, List<Long> removeIds) {
        User user = getCurrentUser();

        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setEmail(updatedUser.getEmail());

        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (addIds != null) {
            for (Long empId : addIds) {
                User emp = userRepository.findById(empId).orElseThrow();
                emp.setAgent(user);
                userRepository.save(emp);
            }
        }

        if (removeIds != null) {
            for (Long empId : removeIds) {
                User emp = userRepository.findById(empId).orElseThrow();
                emp.setAgent(user);
                userRepository.save(emp);
            }
        }

        userRepository.save(user);
    }

    @Override
    public User registerNewUser(User user, List<String> roleNames) {
        System.out.println("Registering new user: " + user.getEmail());
        System.out.println("Roles requested: " + roleNames);

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Set<Role> roles = roleNames.stream()
                .map(name -> {
                    Role role = roleRepository.findByName(name)
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name));
                    System.out.println("Resolved role: " + role.getName());
                    return role;
                })
                .collect(Collectors.toSet());

        user.setRoles(roles);
        user.setEnabled(true); // ← just to be sure
        return userRepository.save(user);
    }


    @Override
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByLastNameAsc();
    }

    @Override
    public List<User> getTeamForCurrentManager() {
        return userRepository.findByAgent(getCurrentUser());
    }

    @Override
    public String storeProfilePicture(Long userId, MultipartFile file) {
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "profile-pictures");
            Files.createDirectories(uploadPath);

            User user = userRepository.findById(userId).orElseThrow();

            if (user.getProfilePicture() != null && !user.getProfilePicture().equals("default.jpg")) {
                Path oldPath = uploadPath.resolve(user.getProfilePicture());
                Files.deleteIfExists(oldPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            user.setProfilePicture(filename);
            userRepository.save(user);

            return filename;

        } catch (IOException ex) {
            System.out.println("Failed to save file: " + ex.getMessage());
            throw new RuntimeException("Failed to store profile picture", ex);
        }
    }

    @Override
    public User updateUser(User savedUser) {
        return userRepository.save(savedUser);
    }

    @Override
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRolesContaining(role);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public List<Property> getPropertiesForCurrentUser() {
        User user = getCurrentUser();
        return propertyRepository.findByOwner(user);
    }

    @Override
    public List<Property> getPropertiesByUser(User user) {
        return propertyRepository.findByOwner(user);
    }

    @Override
    public void prepareManagePropertiesModel(Model model) {
        User currentUser = getCurrentUser();
        List<Property> properties = propertyRepository.findByOwner(currentUser);
        model.addAttribute("properties", properties);
    }
}
