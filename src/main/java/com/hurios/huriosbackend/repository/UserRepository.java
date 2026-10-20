package com.hurios.huriosbackend.repository;

import com.hurios.huriosbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/*
 UserRepository: JPA repo para consultas a tabla users
*/
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
