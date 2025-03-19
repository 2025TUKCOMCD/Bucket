package com.bucket.backend.controller;


import com.bucket.backend.model.users;
import com.bucket.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//유저 정보 관리 controller (회원가입, 로그인 등..)
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //회원가입 API
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody users user) {

        String response = userService.registerUser(user);
        if(response.equals("회원가입 성공!")) {
            return ResponseEntity.ok(response);
        }else{
            return ResponseEntity.badRequest().body(response);
        }
    }

    //로그인 API
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String id, @RequestParam String password) {
        String response = userService.loginUser(id, password);
        if(response.equals("로그인 성공!")){
            return ResponseEntity.ok(response);
        }
        else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
