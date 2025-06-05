package edu.hingu.project.controllers;

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

    // 🔍 Browse properties page (accessible to all authenticated users)
    @GetMapping("/browse")
    @PreAuthorize("permitAll()") // 👈 Allow public access
    public String browse(@RequestParam(required = false) String zip,
                         @RequestParam(required = false) Integer minSqft,
                         @RequestParam(required = false) Double minPrice,
                         @RequestParam(required = false) Double maxPrice,
                         @RequestParam(required = false) String sort,
                         Model model) {
        List<Property> filtered = propertyService.filterProperties(zip, minSqft, minPrice, maxPrice, sort);
        model.addAttribute("properties", filtered);
        return "browse";
    }

    // 🛠 Manage properties (MANAGER only)
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
