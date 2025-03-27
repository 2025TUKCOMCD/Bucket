package com.bucket.backend.controller;


import com.bucket.backend.model.users;
import com.bucket.backend.service.UserService;
import jakarta.servlet.http.HttpSession;
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
    public ResponseEntity<String> login(@RequestParam String id, @RequestParam String password, HttpSession session) {
        String response = userService.loginUser(id, password, session);
        if(response.equals("로그인 성공!")){
            return ResponseEntity.ok(response);
        }
        else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        String response = userService.logoutUser(session);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        users user = (users)session.getAttribute("user");
        if(user != null) {
            return ResponseEntity.ok(user);
        }else{
            return ResponseEntity.status(401).body("로그인 되지 않음");
        }
    }
}
