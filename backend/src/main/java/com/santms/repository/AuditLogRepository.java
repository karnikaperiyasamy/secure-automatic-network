package com.santms.repository;

import com.santms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(Long orgId, Pageable pageable);

    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String username);

    @Query("SELECT a FROM AuditLog a WHERE a.organization.id = :orgId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentByOrg(@Param("orgId") Long orgId, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM AuditLog a WHERE a.organization.id = :orgId AND a.category = :cat ORDER BY a.createdAt DESC")
    List<AuditLog> findByCategoryAndOrg(@Param("orgId") Long orgId, @Param("cat") AuditLog.LogCategory category);
}
