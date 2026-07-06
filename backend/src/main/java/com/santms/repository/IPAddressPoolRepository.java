package com.santms.repository;
import com.santms.entity.IPAddressPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.Optional;

@Repository
public interface IPAddressPoolRepository extends JpaRepository<IPAddressPool, Long> {
    Optional<IPAddressPool> findByIpAddress(String ip);
    List<IPAddressPool> findByOrganizationId(Long orgId);
    List<IPAddressPool> findByOrganizationIdAndStatus(Long orgId, IPAddressPool.IPStatus status);
    boolean existsByIpAddressAndOrganizationId(String ip, Long orgId);
    long countByOrganizationIdAndStatus(Long orgId, IPAddressPool.IPStatus status);
}
