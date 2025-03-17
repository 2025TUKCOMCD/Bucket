package com.bucket.backend.controller;


import com.bucket.backend.model.users;
import com.bucket.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

//유저 정보 관리 controller (회원가입, 로그인 등..)
@RestController
public class UserController {

    public UserController(UserService userService) {
    }

    //회원가입 API
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody users user) {

        return null;
    }

    //로그인 API
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody users user) {

        return null;
    }
}
