package com.bucket.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

//Lombok
@Getter
@Setter
@Entity
@Table(name="users")
public class users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //auto_increment
    private int uid;


    @Column(nullable = false, length = 20)
    private String username;

    private Date birthday;

    @Column(nullable = false, unique = true, length = 20)
    private String id;

    @Column(nullable = false, length = 20)
    private String pwd;

    @Column(nullable = false,unique = true, length = 20 )
    private String email;

}
