package com.santms.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TopologyResponse {
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;
    private int totalNodes;
    private int totalEdges;
    private String lastUpdated;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NodeDto {
        private Long id;
        private Long deviceId;
        private String label;
        private String title;
        private String group;
        private String shape;
        private String color;
        private int size;
        private double x;
        private double y;
        private String status;
        private String deviceType;
        private String ipAddress;
        private String macAddress;
        private String vendor;
        private double cpuUsage;
        private double ramUsage;
        private double latency;
        private int riskScore;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EdgeDto {
        private Long id;
        private Long from;
        private Long to;
        private String label;
        private String color;
        private String connectionType;
        private boolean animated;
        private int bandwidth;
    }
}
