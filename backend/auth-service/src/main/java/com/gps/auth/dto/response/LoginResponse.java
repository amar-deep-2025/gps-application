package com.gps.auth.dto.response;



public record LoginResponse (

    String message,
    String  publicId,
    String accessToken,
    String refreshToken,
    String tokenType

){}
