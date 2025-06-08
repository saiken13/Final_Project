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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;
import edu.hingu.project.services.PropertyService;
import edu.hingu.project.services.UserService;
import jakarta.servlet.http.HttpServletRequest;

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
            return "redirect:/browse";
        }

        if (propertyService.isFavoritedByUser(currentUser, property)) {
            propertyService.removeFavorite(currentUser, propertyId);
            redirectAttributes.addFlashAttribute("successMessage", "Removed from favorites.");
        } else {
            redirectAttributes.addFlashAttribute("infoMessage", "Property was not in your favorites.");
        }

        return "redirect:/details/" + propertyId; 
    }


    @GetMapping("/favorites")
    @PreAuthorize("hasRole('BUYER')")
    public String viewFavorites(Model model, HttpServletRequest request) {
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

        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        model.addAttribute("_csrf", csrfToken); 

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
    public String showEditForm(@PathVariable Long id, Model model) {
        Property property = propertyService.getById(id).orElse(null);

        if (property == null) {
            return "redirect:/properties/manage";
        }

        String folder = property.getImageFolder();
        List<String> imageUrls = new ArrayList<>();

        try {
            Path imageDir = Paths.get(ResourceUtils.getFile("classpath:static/images/property-images/" + folder).toURI());
            if (Files.exists(imageDir)) {
                Files.list(imageDir).forEach(path -> {
                    String filename = path.getFileName().toString();
                    imageUrls.add(folder + "/" + filename);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        property.setImageUrls(imageUrls);
        model.addAttribute("property", property);
        return "edit-property";
    }

    @InitBinder("property")
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("images");
    }


    @PostMapping("/properties/edit/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public String updateProperty(@PathVariable Long id,
                                @ModelAttribute("property") Property updatedProperty,
                                @RequestParam("images") MultipartFile[] images,
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

        propertyService.updatePropertyWithImages(property, images);
        redirectAttributes.addFlashAttribute("successMessage", "Property updated successfully.");
        return "redirect:/properties/manage";
    }

    @PostMapping("/properties/{id}/images/delete/{folder}/{filename:.+}")
    @PreAuthorize("hasRole('AGENT')")
    public String deleteImage(@PathVariable Long id,
                            @PathVariable String folder,
                            @PathVariable String filename,
                            RedirectAttributes redirectAttributes) {
        Property property = propertyService.getById(id).orElse(null);
        if (property == null || !property.getOwner().getId().equals(userService.getCurrentUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized or Property not found");
            return "redirect:/properties/manage";
        }

        Path imagePath = Paths.get("src/main/resources/static/images/property-images", folder, filename);

        try {
            Files.deleteIfExists(imagePath);
            redirectAttributes.addFlashAttribute("successMessage", "Image deleted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete image: " + e.getMessage());
        }

        return "redirect:/properties/edit/" + id;
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

    @GetMapping("/properties/new")
    @PreAuthorize("hasRole('AGENT')")
    public String showAddPropertyForm(Model model, HttpServletRequest request) {
        System.out.println(" Current authorities:");
        SecurityContextHolder.getContext().getAuthentication().getAuthorities()
            .forEach(auth -> System.out.println(" - " + auth.getAuthority()));

        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        model.addAttribute("_csrf", csrfToken);

        model.addAttribute("property", new Property());
        return "add-property";
    }



    @PostMapping("/properties/new")
    @PreAuthorize("hasRole('AGENT')")
    public String addProperty(@ModelAttribute("property") Property property,
                            @RequestParam("images") MultipartFile[] images,
                            RedirectAttributes redirectAttributes,
                            HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        System.out.println("CSRF Token (from request): " + csrfToken.getToken());

        try {
            Property savedProperty = propertyService.saveProperty(property);
            propertyService.updatePropertyWithImages(savedProperty, images);
            redirectAttributes.addFlashAttribute("successMessage", "Property added successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add property: " + e.getMessage());
        }

        return "redirect:/properties/manage";
    }




}