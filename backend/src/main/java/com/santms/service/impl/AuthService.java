package com.santms.service.impl;

import com.santms.dto.request.AuthDTOs.*;
import com.santms.dto.response.AuthResponseDTOs.*;
import com.santms.entity.*;
import com.santms.exception.*;
import com.santms.repository.*;
import com.santms.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authentication service handling login, registration, JWT, password reset
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository orgRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    /**
     * Authenticate user and return JWT tokens
     */
    public LoginResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByUsernameOrEmail(
                request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isAccountLocked()) {
            throw new UnauthorizedException("Account is temporarily locked. Try again later.");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active.");
        }

        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (BadCredentialsException e) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid credentials");
        }

        // Reset login attempts on success
        user.setLoginAttempts(0);
        user.setLastLogin(LocalDateTime.now());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("roles", user.getRoles().stream()
                .map(r -> r.getName().name()).collect(Collectors.toList()));
        if (user.getOrganization() != null) {
            claims.put("orgId", user.getOrganization().getId());
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername()).password("").authorities(List.of()).build());

        user.setRefreshToken(refreshToken);
        if (request.isRememberMe()) {
            user.setRememberMeToken(UUID.randomUUID().toString());
        }
        userRepository.save(user);

        // Audit log
        logAction(user, "LOGIN", "AUTH", ipAddress, "User logged in successfully");

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name()).collect(Collectors.toSet());

        UserInfo userInfo = UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .roles(roles)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .profileImage(user.getProfileImage())
                .lastLogin(user.getLastLogin())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(userInfo)
                .build();
    }

    /**
     * Register a new user
     */
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        Role.RoleName roleName = Role.RoleName.ROLE_READ_ONLY;
        if (request.getRoleName() != null) {
            try { roleName = Role.RoleName.valueOf(request.getRoleName()); }
            catch (IllegalArgumentException ignored) {}
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        Organization org = null;
        if (request.getOrganizationId() != null) {
            org = orgRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrganizationId()));
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .roles(new HashSet<>(Set.of(role)))
                .organization(org)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
        return new MessageResponse("User registered successfully", true);
    }

    /**
     * Refresh access token using refresh token
     */
    public TokenRefreshResponse refreshToken(String refreshToken) {
        User user = userRepository.findAll().stream()
                .filter(u -> refreshToken.equals(u.getRefreshToken()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("roles", user.getRoles().stream()
                .map(r -> r.getName().name()).collect(Collectors.toList()));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), claims);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .build();
    }

    /**
     * Initiate password reset
     */
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with that email"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // In production, send email with reset link
        log.info("Password reset token for {}: {}", user.getEmail(), token);
        return new MessageResponse("Password reset link sent to your email", true);
    }

    /**
     * Reset password with token
     */
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return new MessageResponse("Password reset successfully", true);
    }

    /**
     * Logout user
     */
    public MessageResponse logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRememberMeToken(null);
            userRepository.save(user);
            logAction(user, "LOGOUT", "AUTH", null, "User logged out");
        });
        return new MessageResponse("Logged out successfully", true);
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getLoginAttempts() + 1;
        user.setLoginAttempts(attempts);
        if (attempts >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("Account locked for user: {} after {} failed attempts", user.getUsername(), attempts);
        }
        userRepository.save(user);
    }

    private void logAction(User user, String action, String category, String ip, String desc) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entityType("USER")
                .entityId(user.getId())
                .entityName(user.getUsername())
                .performedBy(user.getUsername())
                .ipAddress(ip)
                .description(desc)
                .category(AuditLog.LogCategory.AUTH)
                .organization(user.getOrganization())
                .build();
        auditLogRepository.save(log);
    }
}
