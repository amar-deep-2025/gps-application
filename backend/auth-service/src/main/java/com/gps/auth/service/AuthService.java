package com.gps.auth.service;

import com.gps.auth.dto.request.ChangePasswordRequest;
import com.gps.auth.dto.request.LoginRequest;
import com.gps.auth.dto.request.RegisterRequest;
import com.gps.auth.dto.response.LoginResponse;
import com.gps.auth.dto.response.UserResponse;
import com.gps.auth.entity.User;
import com.gps.auth.enums.Role;
import com.gps.auth.enums.Status;
import com.gps.auth.jwt.JwtService;
import com.gps.auth.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void register(RegisterRequest request){

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())){
            throw new IllegalArgumentException("Email is Already registered");
        }
        if(userRepository.existsByPhone(request.getPhone())){
            throw new IllegalArgumentException("Phone number is already registered");
        }
        User user=new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone().trim());
        user.setRole(Role.USER);

        userRepository.save(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request){

        User user=userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(()->new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "invalid email or password"
                ));
        if(!passwordEncoder.matches(request.getPassword(),
                user.getPasswordHash())){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid email or password"
            );
        }

        if (user.getStatus()!= Status.ACTIVE){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is not active"
            );
        }
        String accessToken= jwtService.generateAccessToken(user.getPublicId().toString(), user.getEmail());
        String refreshToken=refreshTokenService.createRefreshToken(user);
        return new LoginResponse(
              "Login successful",
              user.getPublicId().toString(),
              accessToken,
              refreshToken,
              "Bearer"
        );

    }

    @Transactional
    public void logoutAll(String publicId){

        User user=userRepository.findByPublicId(UUID.fromString(publicId))
                .orElseThrow(()->
                        new IllegalArgumentException("User not found"));

        refreshTokenService.logoutAll(user.getId());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String publicId){
        UUID userPublicId=UUID.fromString(publicId);
        User user=userRepository.findByPublicId(userPublicId)
                .orElseThrow(()->new IllegalArgumentException("User not found"));

        return new UserResponse(
                user.getPublicId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.isPhoneVerified()

        );
    }

    @Transactional
    public void changePassword(String publicId, ChangePasswordRequest request){

        UUID userPublicId=UUID.fromString(publicId);

        User user=userRepository.findByPublicId(userPublicId)
                .orElseThrow(()->new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.oldPassword(),user.getPasswordHash())){
            throw new IllegalArgumentException("old password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())){
            throw new IllegalArgumentException("New password cannot be same as old password");
        }
        if (!request.newPassword().equals(request.confirmPassword())){
            throw new IllegalArgumentException("New password and Confirm Password do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenService.logoutAll(user.getId());

    }
}
