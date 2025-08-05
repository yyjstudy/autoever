package com.autoever.member.repository;

import com.autoever.member.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    boolean existsBySocialNumber(String socialNumber);
    
    Page<User> findByUsernameContaining(String username, Pageable pageable);
    
    Page<User> findByNameContaining(String name, Pageable pageable);
    
    Page<User> findByUsernameContainingAndNameContaining(String username, String name, Pageable pageable);
    
    long countByCreatedAtGreaterThanEqual(LocalDateTime startOfDay);
    
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR u.username LIKE %:username%) AND " +
           "(:name IS NULL OR u.name LIKE %:name%)")
    Page<User> findUsersWithOptionalFilters(String username, String name, Pageable pageable);
}