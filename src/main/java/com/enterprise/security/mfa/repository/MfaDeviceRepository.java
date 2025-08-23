package com.enterprise.security.mfa.repository;

import com.enterprise.security.mfa.model.MfaDevice;
import com.enterprise.security.mfa.model.MfaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaDeviceRepository extends JpaRepository<MfaDevice, UUID> {
    
    List<MfaDevice> findByUserIdAndIsActiveTrue(String userId);
    
    List<MfaDevice> findByUserIdAndTypeAndIsActiveTrue(String userId, MfaType type);
    
    Optional<MfaDevice> findByUserIdAndDeviceIdentifierAndIsActiveTrue(String userId, String deviceIdentifier);
    
    Optional<MfaDevice> findByDeviceIdentifierAndIsActiveTrue(String deviceIdentifier);
    
    @Query("SELECT md FROM MfaDevice md WHERE md.userId = :userId AND md.isActive = true AND md.isVerified = true")
    List<MfaDevice> findVerifiedDevicesByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(md) FROM MfaDevice md WHERE md.userId = :userId AND md.type = :type AND md.isActive = true AND md.isVerified = true")
    long countVerifiedDevicesByUserIdAndType(@Param("userId") String userId, @Param("type") MfaType type);
    
    @Query("SELECT CASE WHEN COUNT(md) > 0 THEN true ELSE false END FROM MfaDevice md WHERE md.userId = :userId AND md.isActive = true AND md.isVerified = true")
    boolean hasVerifiedDevices(@Param("userId") String userId);
    
    @Query("SELECT md FROM MfaDevice md WHERE md.userId = :userId AND md.type IN :types AND md.isActive = true AND md.isVerified = true ORDER BY md.lastUsedAt DESC")
    List<MfaDevice> findPreferredDevicesByTypes(@Param("userId") String userId, @Param("types") List<MfaType> types);
    
    void deleteByUserIdAndIsActiveFalse(String userId);
}