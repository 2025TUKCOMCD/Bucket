package com.bucket.backend.controller;

import lombok.Getter;

//커스텀 응답 개체 -> 클라이언트와의 통신에서 응답 데이터를 구조화할 수 있음
@Getter
public class ApiResponse {


    private int id;
    private String message;

    public ApiResponse(int id, String message) {
        this.id = id;
        this.message = message;
    }
}
