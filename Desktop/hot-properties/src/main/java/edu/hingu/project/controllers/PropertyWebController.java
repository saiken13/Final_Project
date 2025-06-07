package edu.hingu.project.controllers;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;
import edu.hingu.project.services.PropertyService;
import edu.hingu.project.services.UserService;

@Controller
public class PropertyWebController {

    private final PropertyService propertyService;
    private final UserService userService;

    public PropertyWebController(PropertyService propertyService, UserService userService) {
        this.propertyService = propertyService;
        this.userService = userService;
    }

    @GetMapping("/browse")
    public String browse(@RequestParam(required = false) String location,
                         @RequestParam(required = false) Integer minSqft,
                         @RequestParam(required = false) Double minPrice,
                         @RequestParam(required = false) Double maxPrice,
                         @RequestParam(required = false) String sort,
                         Model model) {
        List<Property> filtered = propertyService.filterProperties(location, minSqft, minPrice, maxPrice, sort);
        model.addAttribute("properties", filtered);
        return "browse";
    }

    @GetMapping("/details/{id}")
    @PreAuthorize("permitAll()")
    public String viewProperty(@PathVariable Long id, Model model) {
        Property property = propertyService.getById(id).orElse(null);

        if (property == null) {
            return "redirect:/browse";
        }

        String folder = property.getImageFolder();
        List<String> imagePaths = new ArrayList<>();

        try {
            Path staticDir = Paths.get("src/main/resources/static/images/property-images/" + folder);
            if (Files.exists(staticDir)) {
                Files.list(staticDir).forEach(path -> {
                    String filename = path.getFileName().toString();
                    String encodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
                    imagePaths.add("/images/property-images/" + folder + "/" + encodedFilename);
                });
            } else {
                System.err.println("Image folder not found: " + staticDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean isFavorited = false;
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            isFavorited = propertyService.isFavoritedByUser(currentUser, property);
        }

        model.addAttribute("property", property);
        model.addAttribute("imagePaths", imagePaths);
        model.addAttribute("isFavorited", isFavorited);
        return "property-details";
    }

    @PostMapping("/favorites/add/{propertyId}")
    @PreAuthorize("hasRole('BUYER')")
    public String addFavorite(@PathVariable Long propertyId, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        Property property = propertyService.getById(propertyId).orElse(null);

        if (property == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Property not found.");
            return "redirect:/browse";
        }

        if (!propertyService.isFavoritedByUser(currentUser, property)) {
            propertyService.toggleFavorite(currentUser, property);
            redirectAttributes.addFlashAttribute("successMessage", "Added to favorites!");
        } else {
            redirectAttributes.addFlashAttribute("infoMessage", "Already in favorites.");
        }

        return "redirect:/details/" + propertyId;
    }

    @PostMapping("/favorites/remove/{propertyId}")
    @PreAuthorize("hasRole('BUYER')")
    public String removeFavorite(@PathVariable Long propertyId, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        Property property = propertyService.getById(propertyId).orElse(null);

        if (property == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Property not found.");
            return "redirect:/favorites";
        }

        if (propertyService.isFavoritedByUser(currentUser, property)) {
            propertyService.toggleFavorite(currentUser, property);
            redirectAttributes.addFlashAttribute("successMessage", "Removed from favorites.");
        } else {
            redirectAttributes.addFlashAttribute("infoMessage", "Property was not in your favorites.");
        }

        return "redirect:/favorites";
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasRole('BUYER')")
    public String viewFavorites(Model model) {
        User currentUser = userService.getCurrentUser();
        List<Property> favorites = propertyService.getFavoritesByUser(currentUser);

        for (Property property : favorites) {
            String folder = property.getImageFolder();
            String imagePath = null;

            if (folder != null && !folder.isEmpty()) {
                Path folderPath = Paths.get("src/main/resources/static/images/property-images/" + folder);
                try {
                    if (Files.exists(folderPath)) {
                        imagePath = Files.list(folderPath)
                                .filter(Files::isRegularFile)
                                .map(path -> "/images/property-images/" + folder + "/" + path.getFileName().toString())
                                .findFirst()
                                .orElse(null);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            property.setTempImagePath(imagePath);
        }

        model.addAttribute("favorites", favorites);
        return "favorites";
    }

    @GetMapping("/properties/manage")
    @PreAuthorize("hasRole('AGENT')")
    public String showManageProperties(Model model) {
        userService.prepareManagePropertiesModel(model);
        return "manage-properties";
    }

    @GetMapping("/properties/edit/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Property property = propertyService.getById(id).orElse(null);
        if (property == null || !property.getOwner().getId().equals(userService.getCurrentUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized or Property not found");
            return "redirect:/properties/manage";
        }
        model.addAttribute("property", property);
        return "edit-property";
    }

    @PostMapping("/properties/edit/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public String updateProperty(@PathVariable Long id,
                                 @ModelAttribute("property") Property updatedProperty,
                                 RedirectAttributes redirectAttributes) {
        Property property = propertyService.getById(id).orElse(null);
        if (property == null || !property.getOwner().getId().equals(userService.getCurrentUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized or Property not found");
            return "redirect:/properties/manage";
        }

        property.setTitle(updatedProperty.getTitle());
        property.setDescription(updatedProperty.getDescription());
        property.setPrice(updatedProperty.getPrice());
        property.setLocation(updatedProperty.getLocation());
        property.setSize(updatedProperty.getSize());
        property.setImageFolder(updatedProperty.getImageFolder());

        propertyService.saveProperty(property);
        redirectAttributes.addFlashAttribute("successMessage", "Property updated successfully.");
        return "redirect:/properties/manage";
    }

    @GetMapping("/properties/delete/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public String deleteProperty(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Property property = propertyService.getById(id).orElse(null);
        if (property == null || !property.getOwner().getId().equals(userService.getCurrentUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized or Property not found");
            return "redirect:/properties/manage";
        }

        propertyService.deleteProperty(id);
        redirectAttributes.addFlashAttribute("successMessage", "Property deleted successfully.");
        return "redirect:/properties/manage";
    }
}
