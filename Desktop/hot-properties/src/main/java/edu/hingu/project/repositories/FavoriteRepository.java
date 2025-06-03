package edu.hingu.project.repositories;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.hingu.project.entities.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByBuyerId(Long buyerId);
    boolean existsByBuyerIdAndPropertyId(Long buyerId, Long propertyId);
    void deleteByBuyerIdAndPropertyId(Long buyerId, Long propertyId);
}
