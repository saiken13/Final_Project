package edu.hingu.project.services;

import edu.hingu.project.entities.Property;
import edu.hingu.project.repositories.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepo;

    public List<Property> getAllProperties() {
        return propertyRepo.findAll();
    }

    public Optional<Property> getById(Long id) {
        return propertyRepo.findById(id);
    }

    public Property saveProperty(Property property) {
        return propertyRepo.save(property);
    }

    public void deleteProperty(Long id) {
        propertyRepo.deleteById(id);
    }

    public List<Property> searchByLocation(String location) {
        return propertyRepo.findByLocationContainingIgnoreCase(location);
    }

    public List<Property> filterByPrice(Double min, Double max) {
        return propertyRepo.findByPriceBetween(min, max);
    }
}
