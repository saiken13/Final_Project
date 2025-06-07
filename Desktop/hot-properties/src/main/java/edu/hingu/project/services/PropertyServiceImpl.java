package edu.hingu.project.services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    // @Override
    // public Property saveProperty(Property property) {
    //     if (property.getImageFolder() == null || property.getImageFolder().isBlank()) {
    //         property.setImageFolder(property.getTitle());
    //     }
    //     return propertyRepository.save(property);
    // }

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
        List<Favorite> favorites = favoriteRepository.findByBuyerId(user.getId());
        return favorites.stream()
                .map(Favorite::getProperty)
                .collect(Collectors.toList());
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

        if (minPrice != null && maxPrice != null) {
            properties = properties.stream()
                    .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
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
        // ❌ This is too late, because the property object may be detached/stale
        // ✅ FIX: Re-fetch the property by ID from DB before proceeding
        Optional<Property> existingProperty = propertyRepository.findById(property.getId());

        if (existingProperty.isEmpty()) {
            throw new IllegalArgumentException("❌ Property with ID " + property.getId() + " does not exist in the database.");
        }

        property = existingProperty.get();

        Optional<Favorite> existing = favoriteRepository.findByBuyerAndProperty(user, property);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setBuyer(user);
            favorite.setProperty(property);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(favorite);
            return true;
        }
    }




    @Override
    public boolean isFavoritedByUser(User user, Property property) {
        return favoriteRepository.findByBuyerAndProperty(user, property).isPresent();
    }

    @Override
    public List<Property> getPropertiesByUser(User user) {
        return propertyRepository.findByOwnerId(user.getId());
    }

    @Override
    public Property saveProperty(Property property) {
        if (property.getImageFolder() == null || property.getImageFolder().isBlank()) {
            property.setImageFolder(property.getTitle().replaceAll(" ", "_"));
        }
    
        if (property.getOwner() == null) {
            User currentUser = userService.getCurrentUser();
            boolean isAgent = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("AGENT"));
    
            if (!isAgent) {
                throw new IllegalArgumentException("Property must have an assigned owner.");
            }
    
            property.setOwner(currentUser);
        }
    
        return propertyRepository.save(property);
    }
    

}
