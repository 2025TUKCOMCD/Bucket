package com.bucket.backend.service;


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

    private final AmazonS3 amazonS3;

    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        // UUID + 파일 명으로 고유 키 생성
        String key = folderName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)   //s3 내 저장될 파일명
                .build();

        // 실제 파일 업로드
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        return key; // 또는 전체 URL 반환하고 싶으면 https://{bucket}.s3.{region}.amazonaws.com/{key}
    }


}
