package com.gps.auth.repository;

import com.gps.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);

    Optional<User>  findByEmailIgnoreCase(String email);
    Optional<User> findByPublicId(UUID publicId);

}
