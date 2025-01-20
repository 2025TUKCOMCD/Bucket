package com.bucket.backend.model;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;

import java.time.LocalDate;


//Lombok
@Setter
@Getter
@Entity
@Table(name="user_videos")
public class user_videos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int vid;

    @Column(nullable = false,length = 20)
    private String sportname;

    @Column(nullable = false, length = 20)
    private LocalDate recordDate;

    @Column(length = 40)
    private String video_url;

    @Column(length = 200)
    private String feedback;

    @ManyToOne  //user 테이블과의 관계 N:1
    @JoinColumn(nullable = false)
    private users user;


}
