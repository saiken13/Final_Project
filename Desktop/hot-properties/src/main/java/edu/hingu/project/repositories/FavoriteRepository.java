package edu.hingu.project.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.hingu.project.entities.Favorite;
import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByBuyerId(Long buyerId);
    boolean existsByBuyerIdAndPropertyId(Long buyerId, Long propertyId);
    void deleteByBuyerIdAndPropertyId(Long buyerId, Long propertyId);
    
    Optional<Favorite> findByBuyerAndProperty(User buyer, Property property);

    

}
