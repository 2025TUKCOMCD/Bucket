package com.bucket.backend.service;

import com.bucket.backend.model.users;
import com.bucket.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //회원가입 기능
    public String registerUser(users user) {
        if(userRepository.existsById(user.getUid())){
            return "이미 사용 중인 ID입니다.";
        }
        user.setPwd(user.getPwd());
        userRepository.save(user);
        return "회원가입 성공!";
    }

    //로그인 기능
    public String loginUser(String id, String pwd) {
        Optional<users> OPuser = userRepository.findById(id);

        if(OPuser.isPresent()){
            users user = OPuser.get();
            if(user.getPwd().equals(pwd)){
                return "로그인 성공!";
            }else{
                return "비밀번호가 일치하지 않습니다.";
            }
        } else{
            return "존재하지 않는 ID입니다.";
        }
    }
}
