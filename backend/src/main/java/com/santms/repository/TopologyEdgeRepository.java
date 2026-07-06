package com.santms.repository;

import com.santms.entity.TopologyEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TopologyEdgeRepository extends JpaRepository<TopologyEdge, Long> {
    List<TopologyEdge> findByOrganizationId(Long orgId);
    List<TopologyEdge> findByTopologyId(Long topologyId);

    @Query("SELECT e FROM TopologyEdge e WHERE e.sourceNode.id = :nodeId OR e.targetNode.id = :nodeId")
    List<TopologyEdge> findByNodeId(@Param("nodeId") Long nodeId);

    void deleteByOrganizationId(Long orgId);
}
