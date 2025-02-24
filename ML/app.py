import os
import json
import numpy as np
import tensorflow as tf
from fastapi import FastAPI, UploadFile, File

# 예제용 keypoints 리스트 (실제 사용하는 관절 이름 리스트로 교체하세요)
keypoints = ["nose", "left_eye", "right_eye", "left_ear", "right_ear",
             "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
             "left_wrist", "right_wrist", "left_hip", "right_hip",
             "left_knee", "right_knee", "left_ankle", "right_ankle"]

# FastAPI 인스턴스 생성
app = FastAPI()

# ST-GCN 모델 로드 (미리 저장된 모델 파일 경로)
model = tf.keras.models.load_model("stgcn_model")

def load_json_skeleton(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    num_frames = len(data["frames"])
    num_joints = len(keypoints)
    num_features = 2  # (x, y)
    num_views = 5     # view1 ~ view5

    # (1, 프레임, 뷰, 관절, 좌표) 형태의 데이터 배열 생성
    X_data = np.zeros((1, num_frames, num_views, num_joints, num_features), dtype=np.float32)

    views = ["view1", "view2", "view3", "view4", "view5"]

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
