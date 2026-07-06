package com.santms.repository;
import com.santms.entity.SecurityScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SecurityScanRepository extends JpaRepository<SecurityScan, Long> {
    List<SecurityScan> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);
    List<SecurityScan> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}
