package com.santms.service.impl;

import com.santms.dto.request.AuthDTOs.RegisterRequest;
import com.santms.dto.response.AuthResponseDTOs.UserInfo;
import com.santms.entity.*;
import com.santms.exception.*;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository orgRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserInfo> getAllUsers(Long orgId) {
        return userRepository.findByOrganizationId(orgId)
                .stream().map(this::toInfo).collect(Collectors.toList());
    }

    public UserInfo getUserById(Long id) {
        return toInfo(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id)));
    }

    public UserInfo getCurrentUser(String username) {
        return toInfo(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username)));
    }

    public UserInfo createUser(RegisterRequest req, Long orgId) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new ConflictException("Email already in use");
        if (userRepository.existsByUsername(req.getUsername()))
            throw new ConflictException("Username already taken");

        Role.RoleName roleName = req.getRoleName() != null ?
                Role.RoleName.valueOf(req.getRoleName()) : Role.RoleName.ROLE_READ_ONLY;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));

        User user = User.builder()
                .username(req.getUsername()).email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName()).lastName(req.getLastName())
                .phone(req.getPhone()).roles(new HashSet<>(Set.of(role)))
                .organization(org).status(User.UserStatus.ACTIVE).build();

        return toInfo(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) throw new ResourceNotFoundException("User", id);
        userRepository.deleteById(id);
    }

    public UserInfo updateStatus(Long id, User.UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setStatus(status);
        return toInfo(userRepository.save(user));
    }

    private UserInfo toInfo(User u) {
        return UserInfo.builder()
                .id(u.getId()).username(u.getUsername()).email(u.getEmail())
                .firstName(u.getFirstName()).lastName(u.getLastName()).fullName(u.getFullName())
                .roles(u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .organizationId(u.getOrganization() != null ? u.getOrganization().getId() : null)
                .organizationName(u.getOrganization() != null ? u.getOrganization().getName() : null)
                .profileImage(u.getProfileImage()).lastLogin(u.getLastLogin())
                .build();
    }
}
