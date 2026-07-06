package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * TopologyNode entity for network graph visualization
 */
@Entity
@Table(name = "topology_nodes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TopologyNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "x_position")
    @Builder.Default
    private Double xPosition = 0.0;

    @Column(name = "y_position")
    @Builder.Default
    private Double yPosition = 0.0;

    @Column(name = "node_group", length = 50)
    @Builder.Default
    private String nodeGroup = "default";

    @Column(name = "node_color", length = 20)
    private String nodeColor;

    @Column(name = "node_size")
    @Builder.Default
    private Integer nodeSize = 30;

    @Column(name = "is_visible")
    @Builder.Default
    private Boolean isVisible = true;

    @Column(name = "label_visible")
    @Builder.Default
    private Boolean labelVisible = true;

    @Column(name = "topology_id")
    private Long topologyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
