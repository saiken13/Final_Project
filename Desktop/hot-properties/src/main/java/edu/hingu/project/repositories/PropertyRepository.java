package edu.hingu.project.repositories;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.hingu.project.entities.Property;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByLocationContainingIgnoreCase(String location);
    List<Property> findByPriceBetween(Double minPrice, Double maxPrice);
}
