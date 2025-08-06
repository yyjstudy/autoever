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
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    Page<User> findByUsernameContaining(String username, Pageable pageable);
    
    Page<User> findByNameContaining(String name, Pageable pageable);
    
    Page<User> findByUsernameContainingAndNameContaining(String username, String name, Pageable pageable);
    
    long countByCreatedAtGreaterThanEqual(LocalDateTime startOfDay);
    
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR u.username LIKE %:username%) AND " +
           "(:name IS NULL OR u.name LIKE %:name%)")
    Page<User> findUsersWithOptionalFilters(String username, String name, Pageable pageable);
    
    /**
     * 특정 연령대의 사용자를 페이지 단위로 조회
     * 주민등록번호의 연도와 성별 코드를 기반으로 계산
     */
    @Query(value = "SELECT u FROM User u WHERE " +
           "YEAR(CURRENT_DATE) - " +
           "CASE " +
           "  WHEN SUBSTRING(u.socialNumber, 8, 1) IN ('1', '2') THEN 1900 + CAST(SUBSTRING(u.socialNumber, 1, 2) AS INTEGER) " +
           "  WHEN SUBSTRING(u.socialNumber, 8, 1) IN ('3', '4') THEN 2000 + CAST(SUBSTRING(u.socialNumber, 1, 2) AS INTEGER) " +
           "END " +
           "BETWEEN :minAge AND :maxAge")
    Page<User> findUsersByAgeRange(int minAge, int maxAge, Pageable pageable);
    
    /**
     * 특정 연령대의 사용자 수 조회
     */
    @Query(value = "SELECT COUNT(u) FROM User u WHERE " +
           "YEAR(CURRENT_DATE) - " +
           "CASE " +
           "  WHEN SUBSTRING(u.socialNumber, 8, 1) IN ('1', '2') THEN 1900 + CAST(SUBSTRING(u.socialNumber, 1, 2) AS INTEGER) " +
           "  WHEN SUBSTRING(u.socialNumber, 8, 1) IN ('3', '4') THEN 2000 + CAST(SUBSTRING(u.socialNumber, 1, 2) AS INTEGER) " +
           "END " +
           "BETWEEN :minAge AND :maxAge")
    long countUsersByAgeRange(int minAge, int maxAge);
}