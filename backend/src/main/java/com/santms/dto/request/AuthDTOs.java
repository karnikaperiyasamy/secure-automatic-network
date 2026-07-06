package com.santms.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

public class AuthDTOs {

    @Getter @Setter
    public static class LoginRequest {
        @NotBlank(message = "Username or email is required")
        private String usernameOrEmail;

        @NotBlank(message = "Password is required")
        private String password;

        private boolean rememberMe;
    }

    @Getter @Setter
    public static class RegisterRequest {
        @NotBlank @Size(min=3, max=50)
        private String username;

        @NotBlank @Email
        private String email;

        @NotBlank @Size(min=8, max=100)
        @Pattern(regexp="^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
                 message="Password must contain uppercase, lowercase, digit and special character")
        private String password;

        @NotBlank(message="First name is required")
        private String firstName;

        @NotBlank(message="Last name is required")
        private String lastName;

        private String phone;
        private Long organizationId;
        private String roleName;
    }

    @Getter @Setter
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Getter @Setter
    public static class ResetPasswordRequest {
        @NotBlank private String token;
        @NotBlank @Size(min=8, max=100)
        private String newPassword;
    }

    @Getter @Setter
    public static class VerifyOtpRequest {
        @NotBlank private String email;
        @NotBlank private String otp;
    }

    @Getter @Setter
    public static class RefreshTokenRequest {
        @NotBlank private String refreshToken;
    }

    @Getter @Setter
    public static class ChangePasswordRequest {
        @NotBlank private String currentPassword;
        @NotBlank @Size(min=8) private String newPassword;
    }
}
