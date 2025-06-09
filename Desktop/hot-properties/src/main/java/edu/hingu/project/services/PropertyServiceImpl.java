package edu.hingu.project.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.hingu.project.entities.Favorite;
import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;
import edu.hingu.project.repositories.FavoriteRepository;
import edu.hingu.project.repositories.PropertyRepository;


@Service
public class PropertyServiceImpl implements PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserService userService;


    @Override
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    @Override
    public Optional<Property> getById(Long id) {
        return propertyRepository.findById(id);
    }

    @Override
    public void deleteProperty(Long id) {
        propertyRepository.deleteById(id);
    }

    @Override
    public List<Property> searchByLocation(String location) {
        return propertyRepository.findByLocationContainingIgnoreCase(location);
    }

    @Override
    public List<Property> filterByPrice(Double minPrice, Double maxPrice) {
        return propertyRepository.findByPriceBetween(minPrice, maxPrice);
    }

    @Override
    public List<Property> getPropertiesByOwner(User owner) {
        return propertyRepository.findByOwner(owner);
    }

    @Override
    public List<Property> getFavoritesByUser(User user) {
        if (user == null) {
            System.out.println(" User is null in getFavoritesByUser");
            return Collections.emptyList();
        }

        List<Favorite> favorites = favoriteRepository.findByBuyerId(user.getId());

        System.out.println("Current favorites for user " + user.getEmail() + ":");
        for (Favorite f : favorites) {
            if (f.getProperty() != null) {
                System.out.println(" - Property ID: " + f.getProperty().getId() + " | Title: " + f.getProperty().getTitle());
            } else {
                System.out.println(" - Property is null in Favorite ID: " + f.getId());
            }
        }

        return favorites.stream()
                .map(Favorite::getProperty)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    
    @Override
    public void removeFavorite(User user, Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new RuntimeException("Property not found"));


        User managedUser = userService.getUserById(user.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Favorite> favoriteOpt = favoriteRepository.findByBuyerAndProperty(managedUser, property);

        if (favoriteOpt.isPresent()) {
            favoriteRepository.delete(favoriteOpt.get());
            favoriteRepository.flush();
            System.out.println("Deleted favorite for propertyId = " + propertyId);
        } else {
            System.out.println("Favorite NOT found for user " + managedUser.getEmail());
        }
    }

    @Override
    public List<Property> filterProperties(String location, Integer minSqft, Double minPrice, Double maxPrice, String sort) {
        List<Property> properties = propertyRepository.findAll();

        if (location != null && !location.isBlank()) {
            properties = properties.stream()
                    .filter(p -> p.getLocation() != null && p.getLocation().toLowerCase().contains(location.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (minSqft != null) {
            properties = properties.stream()
                    .filter(p -> p.getSize() >= minSqft)
                    .collect(Collectors.toList());
        }

        if (minPrice != null) {
            properties = properties.stream()
                    .filter(p -> p.getPrice() >= minPrice)
                    .collect(Collectors.toList());
        }

        if (maxPrice != null) {
            properties = properties.stream()
                    .filter(p -> p.getPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        if ("high".equals(sort)) {
            properties.sort(Comparator.comparing(Property::getPrice).reversed());
        } else if ("low".equals(sort)) {
            properties.sort(Comparator.comparing(Property::getPrice));
        }

        return properties;
    }

    @Override
    public boolean toggleFavorite(User user, Property property) {

        User managedUser = userService.getUserById(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Property managedProperty = propertyRepository.findById(property.getId())
            .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        Optional<Favorite> existing = favoriteRepository.findByBuyerAndProperty(managedUser, managedProperty);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setBuyer(managedUser);
            favorite.setProperty(managedProperty);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(favorite);
            return true;
        }
    }






    @Override
    public boolean isFavoritedByUser(User user, Property property) {
        User managedUser = userService.getUserById(user.getId())
        .orElseThrow(() -> new RuntimeException("User not found"));

        Property managedProperty = propertyRepository.findById(property.getId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        return favoriteRepository.findByBuyerAndProperty(managedUser, managedProperty).isPresent();
    }


    @Override
    public List<Property> getPropertiesByUser(User user) {
        List<Property> properties = propertyRepository.findByOwnerId(user.getId());

        List<Favorite> allFavorites = favoriteRepository.findAll();

        for (Property property : properties) {
            if (property != null && property.getId() != null) {
                long favoriteCount = allFavorites.stream()
                    .filter(fav -> fav.getProperty() != null && fav.getProperty().getId().equals(property.getId()))
                    .count();
                property.setFavoriteCount((int) favoriteCount);
            }
        }

        return properties;
    }




    @Override
    public void deleteImage(Property property, String relativePath) {
        try {
            Path imagePath = Paths.get("src/main/resources/static/images/property-images").resolve(relativePath);
            System.out.println("🧹 Deleting: " + imagePath.toAbsolutePath());

            boolean deleted = Files.deleteIfExists(imagePath);
            System.out.println(deleted ? "Deleted image." : "Image not found.");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete image: " + relativePath, e);
        }
    }

    public Property saveProperty(Property property) {
        if (property.getTitle() == null || property.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
    
        if (property.getImageFolder() == null || property.getImageFolder().isBlank()) {
            property.setImageFolder(property.getTitle().replaceAll(" ", "_"));
        }
    
        if (property.getOwner() == null) {
            User currentUser = userService.getCurrentUser();
            if (currentUser == null || currentUser.getRoles().stream()
                    .noneMatch(role -> role.getName().equalsIgnoreCase("AGENT"))) {
                throw new IllegalArgumentException("Property must have an assigned agent owner.");
            }
    
            property.setOwner(currentUser);
        }
    
        return propertyRepository.save(property);
    }
    

    @Override
    public void updatePropertyWithImages(Property property, MultipartFile[] images) {
        if (images != null && images.length > 0) {
            String folder = property.getImageFolder();
            Path folderPath = Paths.get("src/main/resources/static/images/property-images/" + folder);
    
            try {
                if (!Files.exists(folderPath)) {
                    Files.createDirectories(folderPath);
                }
    
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        Path filePath = folderPath.resolve(image.getOriginalFilename());
                        Files.write(filePath, image.getBytes());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        propertyRepository.save(property);
    }
    


}
