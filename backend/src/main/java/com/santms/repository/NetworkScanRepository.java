package com.santms.repository;

import com.santms.entity.NetworkScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NetworkScanRepository extends JpaRepository<NetworkScan, Long> {
    List<NetworkScan> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);
    Page<NetworkScan> findByOrganizationId(Long orgId, Pageable pageable);
    List<NetworkScan> findByStatus(NetworkScan.ScanStatus status);
}
