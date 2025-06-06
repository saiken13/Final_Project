package edu.hingu.project.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
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

    public PropertyWebController(PropertyService propertyService,
                                 UserService userService) {
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
            Path imageDir = Paths.get(ResourceUtils.getFile("classpath:static/images/property-images/" + folder).toURI());
            if (Files.exists(imageDir)) {
                Files.list(imageDir).forEach(path -> {
                    String filename = path.getFileName().toString();
                    imagePaths.add("/images/property-images/" + folder + "/" + filename);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        model.addAttribute("property", property);
        model.addAttribute("imagePaths", imagePaths);
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


    @GetMapping("/properties/manage")
    @PreAuthorize("hasRole('MANAGER')")
    public String showManageProperties(Model model) {
        userService.prepareManagePropertiesModel(model);
        return "manage-properties";
    }

    @GetMapping("/properties/edit/{id}")
    @PreAuthorize("hasRole('MANAGER')")
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
    @PreAuthorize("hasRole('MANAGER')")
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
    @PreAuthorize("hasRole('MANAGER')")
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
