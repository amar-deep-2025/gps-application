package com.gps.auth.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
public class LoginResponse {

    private String message;
    private String accessToken;
    private String tokenType;

}
