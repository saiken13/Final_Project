package edu.hingu.project.controllers;

import edu.hingu.project.entities.Property;
import edu.hingu.project.services.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/property")
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @GetMapping("/all")
    public List<Property> getAll() {
        return propertyService.getAllProperties();
    }

    @GetMapping("/{id}")
    public Optional<Property> getOne(@PathVariable Long id) {
        return propertyService.getById(id);
    }

    @PostMapping("/create")
    public Property create(@RequestBody Property property) {
        return propertyService.saveProperty(property);
    }

    @PutMapping("/update")
    public Property update(@RequestBody Property property) {
        return propertyService.saveProperty(property);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        propertyService.deleteProperty(id);
        return "Deleted successfully.";
    }

    @GetMapping("/search")
    public List<Property> searchByLocation(@RequestParam String location) {
        return propertyService.searchByLocation(location);
    }

    @GetMapping("/filter")
    public List<Property> filterByPrice(@RequestParam Double min, @RequestParam Double max) {
        return propertyService.filterByPrice(min, max);
    }
}
