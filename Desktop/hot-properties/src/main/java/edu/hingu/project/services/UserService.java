package edu.hingu.project.services;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.Role;
import edu.hingu.project.entities.User;

public interface UserService {

    @PreAuthorize("isAuthenticated()")
    void prepareDashboardModel(Model model);

    @PreAuthorize("isAuthenticated()")
    void prepareProfileModel(Model model);

    @PreAuthorize("isAuthenticated()")
    void prepareSettingsModel(Model model);

    @PreAuthorize("isAuthenticated()")
    void updateUserSettings(User updatedUser, String password, List<Long> addIds, List<Long> removeIds);

    @PreAuthorize("hasRole('ADMIN')")
    List<User> getAllUsers();

    @PreAuthorize("hasRole('AGENT')")
    List<User> getTeamForCurrentManager();

    String storeProfilePicture(Long userId, MultipartFile file);

    User registerNewUser(User user, List<String> roleNames);

    User updateUser(User savedUser);

    @PreAuthorize("isAuthenticated()")
    User getCurrentUser();

    List<User> getUsersByRole(Role role);

    Optional<User> getUserById(Long id);

    void deleteUser(Long id);

    @PreAuthorize("hasRole('AGENT')")
    void prepareManagePropertiesModel(Model model);

    List<Property> getPropertiesByUser(User user);

    @PreAuthorize("hasRole('BUYER')")
    List<Property> getPropertiesForCurrentUser();
}
