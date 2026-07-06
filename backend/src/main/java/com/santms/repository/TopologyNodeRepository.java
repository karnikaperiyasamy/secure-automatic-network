package com.santms.repository;

import com.santms.entity.TopologyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopologyNodeRepository extends JpaRepository<TopologyNode, Long> {
    List<TopologyNode> findByOrganizationId(Long orgId);
    List<TopologyNode> findByTopologyId(Long topologyId);
    Optional<TopologyNode> findByDeviceId(Long deviceId);
    void deleteByOrganizationId(Long orgId);
}
