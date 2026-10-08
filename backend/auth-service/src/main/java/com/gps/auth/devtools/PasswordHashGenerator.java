package com.gps.auth.devtools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {

        BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
        String password="amardeep@2025";
        String hash=encoder.encode(password);
        System.out.println(hash);

        int res=60000*30;
        System.out.println(res);

    }
}
