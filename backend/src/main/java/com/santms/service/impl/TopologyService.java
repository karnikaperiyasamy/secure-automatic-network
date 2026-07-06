package com.santms.service.impl;

import com.santms.dto.response.TopologyResponse;
import com.santms.entity.*;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Topology generation service - builds vis.js compatible graph data
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TopologyService {

    private final DeviceRepository deviceRepository;
    private final TopologyNodeRepository nodeRepository;
    private final TopologyEdgeRepository edgeRepository;

    /**
     * Generate full topology for an organization
     */
    public TopologyResponse getTopology(Long orgId) {
        List<Device> devices = deviceRepository.findByOrganizationId(orgId);

        // Auto-generate nodes if none exist
        List<TopologyNode> nodes = nodeRepository.findByOrganizationId(orgId);
        if (nodes.isEmpty()) {
            nodes = generateNodes(devices, orgId);
        } else {
            // Sync: add nodes for any devices discovered since the last topology
            // build (e.g. by a network scan) instead of only generating once.
            java.util.Set<Long> nodedDeviceIds = nodes.stream()
                    .filter(n -> n.getDevice() != null)
                    .map(n -> n.getDevice().getId())
                    .collect(Collectors.toSet());
            List<Device> newDevices = devices.stream()
                    .filter(d -> !nodedDeviceIds.contains(d.getId()))
                    .collect(Collectors.toList());
            if (!newDevices.isEmpty()) {
                List<TopologyNode> newNodes = generateNodes(newDevices, orgId);
                nodes = new ArrayList<>(nodes);
                nodes.addAll(newNodes);
                // Rebuild edges so the newly discovered nodes are connected too
                edgeRepository.deleteByOrganizationId(orgId);
                generateEdges(nodes, orgId);
            }
        }

        List<TopologyEdge> edges = edgeRepository.findByOrganizationId(orgId);
        if (edges.isEmpty()) {
            edges = generateEdges(nodes, orgId);
        }

        List<TopologyResponse.NodeDto> nodeDtos = nodes.stream()
                .filter(n -> n.getDevice() != null)
                .map(this::toNodeDto)
                .collect(Collectors.toList());

        List<TopologyResponse.EdgeDto> edgeDtos = edges.stream()
                .map(this::toEdgeDto)
                .collect(Collectors.toList());

        return TopologyResponse.builder()
                .nodes(nodeDtos)
                .edges(edgeDtos)
                .totalNodes(nodeDtos.size())
                .totalEdges(edgeDtos.size())
                .lastUpdated(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    /**
     * Regenerate topology from scratch
     */
    public TopologyResponse regenerateTopology(Long orgId) {
        edgeRepository.deleteByOrganizationId(orgId);
        nodeRepository.deleteByOrganizationId(orgId);

        List<Device> devices = deviceRepository.findByOrganizationId(orgId);
        List<TopologyNode> nodes = generateNodes(devices, orgId);
        List<TopologyEdge> edges = generateEdges(nodes, orgId);

        return getTopology(orgId);
    }

    /**
     * Update node position (drag & drop)
     */
    public void updateNodePosition(Long nodeId, double x, double y) {
        nodeRepository.findById(nodeId).ifPresent(node -> {
            node.setXPosition(x);
            node.setYPosition(y);
            nodeRepository.save(node);
        });
    }

    private List<TopologyNode> generateNodes(List<Device> devices, Long orgId) {
        if (devices.isEmpty()) return List.of();

        Organization org = devices.get(0).getOrganization();
        List<TopologyNode> nodes = new ArrayList<>();
        Random rnd = new Random(orgId);

        // Layout: routers/firewalls at center-top, switches middle, endpoints bottom
        int routerCount = 0, switchCount = 0, endpointCount = 0;

        for (Device device : devices) {
            double x, y;
            String group;

            switch (device.getDeviceType()) {
                case ROUTER, FIREWALL -> {
                    x = 300 + routerCount * 200;
                    y = 100;
                    routerCount++;
                    group = "core";
                }
                case SWITCH -> {
                    x = 150 + switchCount * 250;
                    y = 280;
                    switchCount++;
                    group = "distribution";
                }
                default -> {
                    x = 50 + (endpointCount % 8) * 180;
                    y = 480 + (endpointCount / 8) * 150;
                    endpointCount++;
                    group = "access";
                }
            }

            TopologyNode node = TopologyNode.builder()
                    .device(device)
                    .xPosition(x + rnd.nextDouble() * 30)
                    .yPosition(y + rnd.nextDouble() * 20)
                    .nodeGroup(group)
                    .nodeColor(getColorForDevice(device))
                    .nodeSize(getSizeForDevice(device))
                    .isVisible(true)
                    .labelVisible(true)
                    .organization(org)
                    .build();

            nodes.add(nodeRepository.save(node));
        }

        return nodes;
    }

    private List<TopologyEdge> generateEdges(List<TopologyNode> nodes, Long orgId) {
        if (nodes.size() < 2) return List.of();

        Organization org = nodes.get(0).getOrganization();
        List<TopologyEdge> edges = new ArrayList<>();

        // Find core nodes (routers/firewalls)
        List<TopologyNode> coreNodes = nodes.stream()
                .filter(n -> "core".equals(n.getNodeGroup()))
                .collect(Collectors.toList());

        List<TopologyNode> distNodes = nodes.stream()
                .filter(n -> "distribution".equals(n.getNodeGroup()))
                .collect(Collectors.toList());

        List<TopologyNode> accessNodes = nodes.stream()
                .filter(n -> "access".equals(n.getNodeGroup()))
                .collect(Collectors.toList());

        // Connect core to distribution
        for (TopologyNode dist : distNodes) {
            TopologyNode core = coreNodes.isEmpty() ? null :
                    coreNodes.get((int)(Math.random() * coreNodes.size()));
            if (core != null) {
                edges.add(createEdge(core, dist, "Uplink", "ETHERNET", 1000, org));
            }
        }

        // Connect distribution to access
        for (int i = 0; i < accessNodes.size(); i++) {
            TopologyNode access = accessNodes.get(i);
            TopologyNode upstream = distNodes.isEmpty() ?
                    (coreNodes.isEmpty() ? null : coreNodes.get(0)) :
                    distNodes.get(i % distNodes.size());
            if (upstream != null) {
                edges.add(createEdge(upstream, access, null, "ETHERNET", 100, org));
            }
        }

        // Connect core nodes to each other (redundancy)
        for (int i = 0; i < coreNodes.size() - 1; i++) {
            edges.add(createEdge(coreNodes.get(i), coreNodes.get(i + 1), "Core Link", "FIBER", 10000, org));
        }

        return edgeRepository.saveAll(edges);
    }

    private TopologyEdge createEdge(TopologyNode src, TopologyNode tgt,
                                     String label, String type, int bw, Organization org) {
        return TopologyEdge.builder()
                .sourceNode(src)
                .targetNode(tgt)
                .label(label)
                .connectionType(type)
                .bandwidthMbps(bw)
                .isActive(src.getDevice().getStatus() != Device.DeviceStatus.OFFLINE &&
                          tgt.getDevice().getStatus() != Device.DeviceStatus.OFFLINE)
                .edgeColor(getEdgeColor(type))
                .organization(org)
                .build();
    }

    private TopologyResponse.NodeDto toNodeDto(TopologyNode n) {
        Device d = n.getDevice();
        return TopologyResponse.NodeDto.builder()
                .id(n.getId())
                .deviceId(d.getId())
                .label(d.getHostname())
                .title(buildTooltip(d))
                .group(n.getNodeGroup())
                .shape(getShapeForDevice(d))
                .color(getColorForDevice(d))
                .size(n.getNodeSize())
                .x(n.getXPosition())
                .y(n.getYPosition())
                .status(d.getStatus() != null ? d.getStatus().name() : "UNKNOWN")
                .deviceType(d.getDeviceType().name())
                .ipAddress(d.getIpAddress())
                .macAddress(d.getMacAddress())
                .vendor(d.getVendor())
                .cpuUsage(d.getCpuUsage())
                .ramUsage(d.getRamUsage())
                .latency(d.getLatencyMs())
                .riskScore(d.getRiskScore())
                .build();
    }

    private TopologyResponse.EdgeDto toEdgeDto(TopologyEdge e) {
        return TopologyResponse.EdgeDto.builder()
                .id(e.getId())
                .from(e.getSourceNode().getId())
                .to(e.getTargetNode().getId())
                .label(e.getLabel())
                .color(e.getEdgeColor())
                .connectionType(e.getConnectionType())
                .animated(e.getIsActive())
                .bandwidth(e.getBandwidthMbps() != null ? e.getBandwidthMbps() : 100)
                .build();
    }

    private String buildTooltip(Device d) {
        return String.format("<b>%s</b><br>IP: %s<br>MAC: %s<br>Type: %s<br>Status: %s<br>CPU: %.1f%%<br>RAM: %.1f%%",
                d.getHostname(), d.getIpAddress(), d.getMacAddress(),
                d.getDeviceType(), d.getStatus(), d.getCpuUsage(), d.getRamUsage());
    }

    private String getColorForDevice(Device d) {
        if (d.getStatus() == Device.DeviceStatus.OFFLINE) return "#ff6b6b";
        if (d.getStatus() == Device.DeviceStatus.CRITICAL) return "#ff4757";
        if (d.getStatus() == Device.DeviceStatus.WARNING) return "#ffa502";
        if (d.getStatus() == Device.DeviceStatus.MAINTENANCE) return "#a4b0be";
        return switch (d.getDeviceType()) {
            case ROUTER -> "#5352ed";
            case SWITCH -> "#2ed573";
            case FIREWALL -> "#ff4757";
            case SERVER -> "#1e90ff";
            case COMPUTER -> "#54a0ff";
            case PRINTER -> "#ffa502";
            case ACCESS_POINT -> "#00d2d3";
            case IOT_DEVICE -> "#ff6348";
            default -> "#a4b0be";
        };
    }

    private String getShapeForDevice(Device d) {
        return switch (d.getDeviceType()) {
            case ROUTER -> "triangle";
            case SWITCH -> "square";
            case FIREWALL -> "diamond";
            case SERVER -> "database";
            case COMPUTER, PRINTER -> "box";
            default -> "ellipse";
        };
    }

    private int getSizeForDevice(Device d) {
        return switch (d.getDeviceType()) {
            case ROUTER, FIREWALL -> 45;
            case SWITCH -> 40;
            case SERVER -> 35;
            default -> 28;
        };
    }

    private String getEdgeColor(String type) {
        return switch (type) {
            case "FIBER" -> "#2ed573";
            case "WIRELESS" -> "#a29bfe";
            default -> "#74b9ff";
        };
    }
}
