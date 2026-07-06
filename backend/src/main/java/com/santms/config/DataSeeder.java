package com.santms.config;

import com.santms.entity.*;
import com.santms.repository.*;
import com.santms.service.impl.NetworkScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Seeds the database with default data on startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final NetworkScanService scanService;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedOrganization();
        seedUsers();
        seedSampleDevices();
        log.info("✅ Database seeded successfully");
    }

    private void seedRoles() {
        for (Role.RoleName name : Role.RoleName.values()) {
            if (roleRepo.findByName(name).isEmpty()) {
                roleRepo.save(Role.builder().name(name)
                    .description(name.name().replace("ROLE_", "").replace("_", " ")).build());
            }
        }
    }

    private void seedOrganization() {
        if (orgRepo.count() == 0) {
            orgRepo.save(Organization.builder()
                .name("SANTMS Corp").description("Enterprise Network Management")
                .domain("santms.com").industry("Technology")
                .address("123 Tech Street").city("San Francisco").country("USA")
                .status(Organization.OrgStatus.ACTIVE).maxDevices(5000).maxUsers(200)
                .build());
        }
    }

    private void seedUsers() {
        if (userRepo.count() > 0) return;
        Organization org = orgRepo.findAll().get(0);

        Map<String, Role.RoleName> users = Map.of(
            "superadmin", Role.RoleName.ROLE_SUPER_ADMIN,
            "netadmin",   Role.RoleName.ROLE_NETWORK_ADMIN,
            "secanalyst", Role.RoleName.ROLE_SECURITY_ANALYST,
            "readonly",   Role.RoleName.ROLE_READ_ONLY
        );

        Map<String, String[]> info = Map.of(
            "superadmin", new String[]{"Super", "Admin", "superadmin@santms.com"},
            "netadmin",   new String[]{"Network", "Admin", "netadmin@santms.com"},
            "secanalyst", new String[]{"Security", "Analyst", "secanalyst@santms.com"},
            "readonly",   new String[]{"Read", "Only", "readonly@santms.com"}
        );

        users.forEach((username, roleName) -> {
            Role role = roleRepo.findByName(roleName).orElseThrow();
            String[] i = info.get(username);
            userRepo.save(User.builder()
                .username(username).email(i[2])
                .password(encoder.encode("Admin@1234"))
                .firstName(i[0]).lastName(i[1])
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(new HashSet<>(Set.of(role)))
                .organization(org)
                .build());
        });
        log.info("👤 Default users created (password: Admin@1234)");
    }

    private void seedSampleDevices() {
        Organization org = orgRepo.findAll().get(0);
        // Trigger async network scan to populate devices
        scanService.startScan("192.168.1.0/24", org.getId(), "system");
    }
}
