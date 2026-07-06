package com.santms.repository;

import com.santms.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByIpAddress(String ipAddress);
    Optional<Device> findByMacAddress(String macAddress);
    Optional<Device> findByHostname(String hostname);

    List<Device> findByOrganizationId(Long orgId);
    List<Device> findByStatus(Device.DeviceStatus status);
    List<Device> findByDeviceType(Device.DeviceType type);

    @Query("SELECT d FROM Device d WHERE d.organization.id = :orgId AND d.status = :status")
    List<Device> findByOrgAndStatus(@Param("orgId") Long orgId, @Param("status") Device.DeviceStatus status);

    @Query("SELECT d FROM Device d WHERE d.organization.id = :orgId AND d.deviceType = :type")
    List<Device> findByOrgAndType(@Param("orgId") Long orgId, @Param("type") Device.DeviceType type);

    @Query("SELECT d FROM Device d WHERE d.organization.id = :orgId AND " +
           "(LOWER(d.hostname) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "d.ipAddress LIKE CONCAT('%', :q, '%') OR " +
           "LOWER(d.vendor) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "d.macAddress LIKE CONCAT('%', :q, '%'))")
    Page<Device> searchDevices(@Param("orgId") Long orgId, @Param("q") String query, Pageable pageable);

    @Query("SELECT COUNT(d) FROM Device d WHERE d.organization.id = :orgId")
    long countByOrganization(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(d) FROM Device d WHERE d.organization.id = :orgId AND d.status = :status")
    long countByOrgAndStatus(@Param("orgId") Long orgId, @Param("status") Device.DeviceStatus status);

    @Query("SELECT COUNT(d) FROM Device d WHERE d.organization.id = :orgId AND d.deviceType = :type")
    long countByOrgAndType(@Param("orgId") Long orgId, @Param("type") Device.DeviceType type);

    @Query("SELECT d FROM Device d WHERE d.organization.id = :orgId AND d.isAuthorized = false")
    List<Device> findUnauthorizedDevices(@Param("orgId") Long orgId);

    @Query("SELECT d FROM Device d WHERE d.organization.id = :orgId AND d.lastSeen < :threshold AND d.status = 'ONLINE'")
    List<Device> findStaleDevices(@Param("orgId") Long orgId, @Param("threshold") LocalDateTime threshold);

    @Query("SELECT d FROM Device d WHERE d.organization.id = :orgId ORDER BY d.cpuUsage DESC")
    List<Device> findTopByOrgOrderByCpuUsage(@Param("orgId") Long orgId, Pageable pageable);

    @Query("SELECT d.deviceType, COUNT(d) FROM Device d WHERE d.organization.id = :orgId GROUP BY d.deviceType")
    List<Object[]> countByTypeForOrg(@Param("orgId") Long orgId);

    boolean existsByIpAddressAndOrganizationId(String ipAddress, Long orgId);
    boolean existsByMacAddressAndOrganizationId(String macAddress, Long orgId);
}
