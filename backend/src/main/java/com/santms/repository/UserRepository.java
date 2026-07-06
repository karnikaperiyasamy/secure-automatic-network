package com.santms.repository;

import com.santms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByRememberMeToken(String token);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.organization.id = :orgId")
    List<User> findByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.id = :id")
    void incrementLoginAttempts(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = 0 WHERE u.id = :id")
    void resetLoginAttempts(@Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :orgId AND u.status = 'ACTIVE'")
    long countActiveByOrganization(@Param("orgId") Long orgId);
}
