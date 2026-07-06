package com.santms.service.impl;

import com.santms.entity.*;
import com.santms.exception.BadRequestException;
import com.santms.exception.ConflictException;
import com.santms.exception.ResourceNotFoundException;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * IP Address Pool management (used by the IP Manager page).
 *
 * This was previously missing entirely — the IPAddressPool entity/repository
 * existed but no service or controller ever exposed them, which is why
 * "Add IP" and "Detect Conflicts" never actually persisted or checked anything.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IpPoolService {

    private final IPAddressPoolRepository ipRepository;
    private final DeviceRepository deviceRepository;
    private final OrganizationRepository orgRepository;

    public List<IPAddressPool> getPool(Long orgId) {
        return ipRepository.findByOrganizationId(orgId);
    }

    public IPAddressPool addIp(Long orgId, String ipAddress, String subnet, String gateway,
                                IPAddressPool.IPType type, Boolean reserved, Integer vlanId,
                                String description) {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new BadRequestException("IP address is required");
        }
        if (ipRepository.findByIpAddress(ipAddress).isPresent()
                || deviceRepository.findByIpAddress(ipAddress).isPresent()) {
            throw new ConflictException("IP address " + ipAddress + " is already in use");
        }

        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        IPAddressPool entry = IPAddressPool.builder()
                .ipAddress(ipAddress)
                .subnet(subnet != null && !subnet.isBlank() ? subnet : "192.168.1.0/24")
                .gateway(gateway)
                .type(type != null ? type : IPAddressPool.IPType.STATIC)
                .status(Boolean.TRUE.equals(reserved) ? IPAddressPool.IPStatus.RESERVED : IPAddressPool.IPStatus.AVAILABLE)
                .isReserved(Boolean.TRUE.equals(reserved))
                .vlanId(vlanId != null ? vlanId : 1)
                .description(description)
                .organization(org)
                .build();

        return ipRepository.save(entry);
    }

    public void deleteIp(Long id) {
        IPAddressPool entry = ipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IP entry not found: " + id));
        ipRepository.delete(entry);
    }

    public IPAddressPool reserveIp(Long id) {
        IPAddressPool entry = ipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IP entry not found: " + id));
        entry.setStatus(IPAddressPool.IPStatus.RESERVED);
        entry.setIsReserved(true);
        return ipRepository.save(entry);
    }

    /**
     * Detects real IP conflicts by checking for duplicate addresses across
     * the device inventory and the managed IP pool for this organization.
     */
    public List<String> detectConflicts(Long orgId) {
        List<String> allIps = new ArrayList<>();
        deviceRepository.findByOrganizationId(orgId).forEach(d -> {
            if (d.getIpAddress() != null) allIps.add(d.getIpAddress());
        });
        ipRepository.findByOrganizationId(orgId).forEach(p -> {
            if (p.getIpAddress() != null) allIps.add(p.getIpAddress());
        });

        Map<String, Long> counts = allIps.stream()
                .collect(Collectors.groupingBy(ip -> ip, Collectors.counting()));

        return counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
