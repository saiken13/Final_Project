package edu.hingu.project.services;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;
import edu.hingu.project.repositories.PropertyRepository;

@Service
public class PropertyServiceImpl implements PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Override
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    @Override
    public Optional<Property> getById(Long id) {
        return propertyRepository.findById(id);
    }

    @Override
    public Property saveProperty(Property property) {
        return propertyRepository.save(property);
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
public List<Property> filterProperties(String zip, Integer minSqft, Double minPrice, Double maxPrice, String sort) {
    List<Property> properties = propertyRepository.findAll();

    if (zip != null && !zip.isBlank()) {
        properties = properties.stream()
            .filter(p -> p.getZipCode() != null && p.getZipCode().contains(zip))
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

}
