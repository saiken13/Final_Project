package edu.hingu.project.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.hingu.project.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
