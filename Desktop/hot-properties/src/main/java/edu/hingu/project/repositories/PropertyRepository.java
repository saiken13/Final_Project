package edu.hingu.project.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    // ✅ Eagerly fetch images when loading all properties (for browse page)
    @EntityGraph(attributePaths = {"images"})
    List<Property> findAll();

    List<Property> findByLocationContainingIgnoreCase(String location);

    List<Property> findByPriceBetween(Double minPrice, Double maxPrice);

    List<Property> findByOwner(User owner);
}
