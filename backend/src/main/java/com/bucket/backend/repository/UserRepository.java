package com.bucket.backend.repository;

import com.bucket.backend.model.users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<users, Integer> {


}
