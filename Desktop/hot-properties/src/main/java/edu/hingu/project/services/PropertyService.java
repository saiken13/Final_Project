package edu.hingu.project.services;

import java.util.List;
import java.util.Optional;

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

    // ✅ Add this declaration to fix the error
    List<Property> filterProperties(String zip, Integer minSqft, Double minPrice, Double maxPrice, String sort);
}
