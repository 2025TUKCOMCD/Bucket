#!/usr/bin/env python
# coding: utf-8

# In[9]:

import os
import json
import numpy as np
import tensorflow as tf
# import asyncio
# import base64
# import cv2
# import numpy as np
# import mediapipe as mp
from fastapi import FastAPI, WebSocket
from fastapi.middleware.cors import CORSMiddleware

keypoints = [
    "Point_0", "Point_2", "Point_5", "Point_7", "Point_8", "Point_11", 
    "Point_12", "Point_13", "Point_14", "Point_15", "Point_16", "Point_17", 
    "Point_18", "Point_21", "Point_23", "Point_25", "Point_26", "Point_28", 
    "Point_30", "Point_31", "Point_32"
]

# FastAPI 앱 생성
app = FastAPI()

# ST-GCN 모델 로드 (미리 저장된 모델 파일 경로)
model = tf.keras.models.load_model("stgcn_model1.keras")



def load_json_skeleton(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    num_frames = len(data["frames"])
    num_joints = len(keypoints)
    num_features = 2  # (x, y)
    num_views = 1

    # (1, 프레임, 뷰, 관절, 좌표) 형태의 데이터 배열 생성
    X_data = np.zeros((1, num_frames, num_views, num_joints, num_features), dtype=np.float32)

    views = ["view1"]

    # JSON 데이터를 배열로 변환
    for frame_idx, frame in enumerate(data["frames"]):
        for view_idx, view in enumerate(views):
            pts = frame.get(view, {}).get("pts", {})
            for joint_idx, joint_name in enumerate(keypoints):
                if joint_name in pts:
                    X_data[0, frame_idx, view_idx, joint_idx, 0] = pts[joint_name]["x"]
                    X_data[0, frame_idx, view_idx, joint_idx, 1] = pts[joint_name]["y"]

    return X_data, data.get("type_info", None)

def predict_json_skeleton(file_path):
    # JSON 파일을 로드하고 전처리
    X_data, _ = load_json_skeleton(file_path)
    # 모델 예측
    prediction = model.predict(X_data)
    predicted_class = int(np.argmax(prediction, axis=-1)[0])
    confidence = float(prediction[0][predicted_class])
    
    if predicted_class == 0:
        result = f"✅ 올바른 자세 ({confidence * 100:.2f}% 확신)"
    else:
        result = f"❌ 잘못된 자세 감지 ({confidence * 100:.2f}% 확신)"
    return result

# CORS 설정 (모든 출처 허용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

#Json파일을 저장할 경로
#JSON_DIR = "C:\\Users\\Yang Dong Gyun\\Desktop\\jsontest"
JSON_DIR = "C:\\Users\\DongGyunYang\\Desktop\\jsontest"
os.makedirs(JSON_DIR, exist_ok=True)


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

                # 응답을 한 번만 전송하도록 수정
                if "response_sent" not in json_data:
                    response = {
                        #"user_id": user_id,
                        "status": "success",
                        "message": "JSON 저장 완료"
                    }
                await websocket.send_text(json.dumps(response))
                json_data["response_sent"] = True  # 응답이 이미 전송되었음을 기록
                # # AI 모델의 응답 데이터 생성
                # response = {
                #     #"user_id": json_data["user_id"],
                #     "status": "success",
                #     "message": "JSON 저장 완료"
                # }

                await websocket.send_text(json.dumps(response))
                print(f"spring로 응답 전송:{response}")
            except Exception as e:
                print(f"Websocket Error: {e}")
                break
    except Exception as e:
        print(f"Websocket Error: {e}")
    finally:
        print("Websocket 종료 처리 완료.")


# 파일 업로드 후 예측 수행하는 엔드포인트
@app.post("/predict/")
async def predict_endpoint(file: UploadFile = File(...)):
    # 업로드된 파일을 임시 파일로 저장
    temp_file_path = f"temp_{file.filename}"
    with open(temp_file_path, "wb") as f:
        f.write(await file.read())
    
    try:
        # 업로드한 JSON 파일을 이용해 예측 수행
        result = predict_json_skeleton(temp_file_path)
    except Exception as e:
        result = f"❌ 예측 실패 (오류: {e})"
    finally:
        # 임시 파일 삭제
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)
    
    return {"result": result}

@app.get("/")
async def root():
    return {"message": "ST-GCN 예측 서버 실행 중"}