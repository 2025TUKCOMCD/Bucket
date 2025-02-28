import os
import json
import numpy as np
import mediapipe as mp
import tensorflow as tf
from fastapi import FastAPI, WebSocket
from fastapi.middleware.cors import CORSMiddleware

# FastAPI 앱 생성
app = FastAPI()

# CORS 설정 (모든 출처 허용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

#Json파일을 저장할 경로
#로컬 경로
#JSON_DIR = "C:\\Users\\DongGyunYang\\Desktop\\jsontest"
#서버 경로
JSON_DIR = "/home/ubuntu/bucket"
os.makedirs(JSON_DIR, exist_ok=True)

# ST-GCN 모델 로드 (미리 저장된 모델 파일 경로)
model = tf.keras.models.load_model("stgcn_model1.keras")

# 예제용 keypoints 리스트 (실제 사용하는 관절 이름 리스트로 교체하세요)
keypoints = ["nose", "left_eye", "right_eye", "left_ear", "right_ear",
             "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
             "left_wrist", "right_wrist", "left_hip", "right_hip",
             "left_knee", "right_knee", "left_ankle", "right_ankle"]

# WebSocket 엔드포인트: Json 데이터 수신 및 AI 분석
@app.websocket("/ws/connect")
async def receive_json(websocket: WebSocket):
    # AI 모델이 WebSocket을 통해 백엔드의 JSON 데이터를 받고 저장하는 WebSocket 서버
    await websocket.accept()

    print("FastAPI WebSocket 연결 성공!")
    try:    
        while True:
            try:
                # 백엔드 서버에서 json데이터 수신
                data = await websocket.receive_text()
                json_data = json.loads(data)

                # json 파일 저장
                json_file_path = f"{JSON_DIR}\\user.json"
                with open(json_file_path, "w", encoding="utf-8") as json_file:
                    json.dump(json_data, json_file, indent=4, ensure_ascii=False)

                print(f"Json 데이터 저장 완료: {json_file_path}")

                
                response = {
                    #"user_id": user_id,
                    "status": "success",
                    "message": "JSON 저장 완료"
                }
                await websocket.send_text(json.dumps(response))

                print(f"spring로 응답 전송:{response}")
            except Exception as e:
                print(f"Websocket Error: {e}")
                break
    except Exception as e:
        print(f"Websocket Error: {e}")
    finally:
        print("Websocket 종료 처리 완료.")
