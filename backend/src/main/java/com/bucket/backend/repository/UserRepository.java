package com.bucket.backend.repository;

import com.bucket.backend.model.users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<users, Integer> {

    //Id로 회원조회
    Optional<users> findById(String id);

    //중복 검사
    boolean existsById(String id);



}
