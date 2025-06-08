package edu.hingu.project.services;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;

public interface PropertyService {

    List<Property> getAllProperties();

    Optional<Property> getById(Long id);

    Property saveProperty(Property property);

    void deleteProperty(Long id);

    List<Property> searchByLocation(String location);

    List<Property> filterByPrice(Double minPrice, Double maxPrice);

    List<Property> getPropertiesByOwner(User owner);

    List<Property> getFavoritesByUser(User user);

    void deleteImage(Property property, String filename);

    void updatePropertyWithImages(Property property, MultipartFile[] images);

    List<Property> filterProperties(String location, Integer minSqft, Double minPrice, Double maxPrice, String sort);

    boolean toggleFavorite(User user, Property property);
    boolean isFavoritedByUser(User user, Property property);

    List<Property> getPropertiesByUser(User user);

    void removeFavorite(User user, Long propertyId);


}
