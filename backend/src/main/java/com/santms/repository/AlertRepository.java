package com.santms.repository;

import com.santms.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);

    Page<Alert> findByOrganizationId(Long orgId, Pageable pageable);

    List<Alert> findByOrganizationIdAndStatus(Long orgId, Alert.AlertStatus status);

    List<Alert> findByOrganizationIdAndSeverity(Long orgId, Alert.AlertSeverity severity);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.organization.id = :orgId AND a.status = 'OPEN'")
    long countOpenAlerts(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.organization.id = :orgId AND a.severity = :sev AND a.status = 'OPEN'")
    long countBySeverityAndOrg(@Param("orgId") Long orgId, @Param("sev") Alert.AlertSeverity severity);

    @Query("SELECT a FROM Alert a WHERE a.organization.id = :orgId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("orgId") Long orgId, @Param("since") LocalDateTime since);

    @Query("SELECT a.type, COUNT(a) FROM Alert a WHERE a.organization.id = :orgId GROUP BY a.type")
    List<Object[]> countByTypeForOrg(@Param("orgId") Long orgId);

    List<Alert> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}
