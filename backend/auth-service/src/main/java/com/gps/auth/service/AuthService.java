package com.gps.auth.service;

import com.gps.auth.dto.request.LoginRequest;
import com.gps.auth.dto.request.RegisterRequest;
import com.gps.auth.dto.response.LoginResponse;
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

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

    @Transactional(readOnly = true)
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

        return new LoginResponse(
              "Login successful",
              user.getPublicId().toString(),
              accessToken,
              "Bearer"
        );

    }
}
