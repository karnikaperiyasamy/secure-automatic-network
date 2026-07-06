package com.santms.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

public class AuthResponseDTOs {

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private UserInfo user;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private Set<String> roles;
        private String organizationName;
        private Long organizationId;
        private String profileImage;
        private LocalDateTime lastLogin;
    }

    @Getter @Setter @AllArgsConstructor
    public static class MessageResponse {
        private String message;
        private boolean success;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TokenRefreshResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private long expiresIn;
    }
}
