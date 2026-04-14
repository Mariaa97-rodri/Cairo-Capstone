package com.cairo.cairobackend.repository;

import com.cairo.cairobackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Security calls this on every authenticated request
    // to load the user by their email (which we use as username)
    Optional<User> findByEmail(String email);

    // Used during registration to check for duplicate emails
    boolean existsByEmail(String email);
}