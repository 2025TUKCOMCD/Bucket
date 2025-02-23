#!/usr/bin/env python
# coding: utf-8

# In[9]:

import os
import json
import asyncio
import base64
import cv2
import numpy as np
import mediapipe as mp
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

JSON_DIR = "C:\\Users\\Yang Dong Gyun\\Desktop\\jsontest"
os.makedirs(JSON_DIR, exist_ok=True)


@app.websocket("/ws/connect")
async def receive_json(websocket: WebSocket):
    # AI 모델이 WebSocket을 통해 백엔드의 JSON 데이터를 받고 저장하는 WebSocket 서버
    await websocket.accept()

    print("서버 실행")
    while True:
        try:
            # 백엔드 서버에서 json데이터 수신
            data = await websocket.receive_text()
            json_data = json.loads(data)

            # json 파일 저장
            json_file_path = f"{JSON_DIR}\\user_{json_data['user_id']}.json"
            with open(json_file_path, "w", encoding="utf-8") as json_file:
                json.dump(json_data, json_file, indent=4, ensure_ascii=False)

            print(f"Json 데이터 저장 완료: {json_file_path}")

            # AI 모델의 응답 데이터 생성
            response = {
                "user_id": json_data["user_id"],
                "status": "success",
                "message": "JSON 저장 완료"
            }
        except Exception as e:
            print(f"Websocket Error: {e}")
            await websocket.close()
            break
