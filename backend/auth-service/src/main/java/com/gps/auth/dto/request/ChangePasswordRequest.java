package com.gps.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message="old password must not be blank")
        String oldPassword,


        @NotBlank(message="New Password is required")
        @Size(min=6,max=30, message="Password must be between 6 and 30 characters")
        String newPassword,

        @NotBlank(message="Confirm Password is required")
        String confirmPassword
) {


}
