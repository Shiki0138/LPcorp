package com.enterprise.security.mfa.repository;

import com.enterprise.security.mfa.model.BackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackupCodeRepository extends JpaRepository<BackupCode, UUID> {
    
    List<BackupCode> findByUserIdAndIsUsedFalse(String userId);
    
    Optional<BackupCode> findByHashedCodeAndIsUsedFalse(String hashedCode);
    
    @Query("SELECT COUNT(bc) FROM BackupCode bc WHERE bc.userId = :userId AND bc.isUsed = false")
    long countUnusedCodesByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(bc) FROM BackupCode bc WHERE bc.userId = :userId AND bc.isUsed = true")
    long countUsedCodesByUserId(@Param("userId") String userId);
    
    void deleteByUserId(String userId);
    
    @Query("SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END FROM BackupCode bc WHERE bc.userId = :userId AND bc.isUsed = false")
    boolean hasUnusedCodes(@Param("userId") String userId);
}