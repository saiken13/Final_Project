package edu.hingu.project.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.hingu.project.entities.Role;
import edu.hingu.project.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByAgent(User agent);

    List<User> findByAgentIsNull();

    List<User> findAllByOrderByLastNameAsc();

    List<User> findByRolesContaining(Role role);
    
    Optional<User> findByEmail(String email);

    


}
