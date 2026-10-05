package com.gps.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message="Name is required")
    @Size(max=50,message="Name must not be exceed 50 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "please provide a valid email address")
    @Size(max=100, message="Email must not be 100 characters")
    private String email;

    @NotBlank(message="Password is required")
    @Size(min=6, max=30, message = "password must be between 6 and 20 characters")
    private String password;

    @NotBlank(message="Phone number is required")
    private String phone;
}
