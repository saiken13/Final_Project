package edu.hingu.project.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.hingu.project.entities.PropertyImage;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
}
