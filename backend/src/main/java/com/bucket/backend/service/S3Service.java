package com.bucket.backend.service;


import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        // UUID + 파일 명으로 고유 키 생성
        String key = folderName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 업로드할 파일의 MIME 타입 자동 감지
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream"; // 기본값 (모르는 타입일 때)
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)   //s3 내 저장될 파일명
                .contentType(contentType)
                .build();

        // 실제 파일 업로드
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        //S3의 URL
        return "https://"+ bucketName + ".s3." + region+".amazonaws.com/" + key;
    }


}
