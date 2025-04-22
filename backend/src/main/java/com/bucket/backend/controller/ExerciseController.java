package com.bucket.backend.controller;


import com.bucket.backend.websocket.AIClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/exercise")
public class ExerciseController {

    private static final Logger log = LoggerFactory.getLogger(ExerciseController.class);
    private final AIClient aiClient;

    public ExerciseController(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    @PostMapping("/select")
    public ResponseEntity<String> selectExercise(@RequestBody Map<String, String> payload) throws IOException {
        String exercise = payload.get("exercise");

        try{
            AIWebSocketHandler.setSelectedExercise(exercise);

//            Map<String, String> message = Map.of("command", "select_model", "exercise", exercise);
//            String json = new ObjectMapper().writeValueAsString(message);
//            aiClient.sendToAI(json);

            log.info("운동 선택됨 : {}", exercise);
            return ResponseEntity.ok("운동 선택 완료: " + exercise);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
        }
    }
}
