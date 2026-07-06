package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TopologyEdge entity representing connections between network nodes
 */
@Entity
@Table(name = "topology_edges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TopologyEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_node_id")
    private TopologyNode sourceNode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_node_id")
    private TopologyNode targetNode;

    @Column(length = 100)
    private String label;

    @Column(name = "connection_type", length = 50)
    @Builder.Default
    private String connectionType = "ETHERNET";

    @Column(name = "bandwidth_mbps")
    private Integer bandwidthMbps;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "edge_color", length = 20)
    @Builder.Default
    private String edgeColor = "#4ecdc4";

    @Column(name = "topology_id")
    private Long topologyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
