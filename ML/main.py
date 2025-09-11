import os
import json
import numpy as np
import tensorflow as tf
from tensorflow.keras import layers
from tensorflow.keras.models import load_model
from stgcn_model import STGCN
import scipy.signal
from fastapi import FastAPI, WebSocket
from fastapi.middleware.cors import CORSMiddleware
from collections import deque
import logging
from scipy.signal import savgol_filter
from stgcn_sport2 import STGCN_sport2

keypoints = [f"Point_{i}" for i in [0,7,8,11,12,13,14,15,16,17,18,21,22,23,24,25,26,27,28,29,30]]
keypoints_2 = [f"Point_{i}" for i in [0,7,11,12,23,24,25,26,27,28,29,30,31,32]]

frame_buffer = deque(maxlen=16)
step_size = 4  # 4프레임마다 결과 출력

def load_json_skeleton(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    num_frames = len(data["frames"])
    num_joints = len(keypoints)
    num_features = 2  # (x, y)
    num_views = 1

    X_data = np.zeros((1, num_frames, num_views, num_joints, num_features), dtype=np.float32)

    views = ["view3"]

    # ✅ JSON 데이터 -> 배열 변환
    for frame_idx, frame in enumerate(data["frames"]):
        for view_idx, view in enumerate(views):
            pts = frame.get(view, {}).get("pts", {})
            for joint_idx, joint_name in enumerate(keypoints):
                if joint_name in pts:
                    X_data[0, frame_idx, view_idx, joint_idx, 0] = pts[joint_name]["x"]
                    X_data[0, frame_idx, view_idx, joint_idx, 1] = pts[joint_name]["y"]

    return X_data
    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    num_frames = len(data["frames"])
    num_views = 1

    X_data = np.zeros((1, num_frames, num_views, num_joints_2, num_features_2), dtype=np.float32)

    views = ["view3"]

    all_xyz = []

    # ✅ JSON 데이터 -> 배열 변환
    for frame_idx, frame in enumerate(data["frames"]):
        for view_idx, view in enumerate(views):
            pts = frame.get(view, {}).get("pts", {})

             # 기준점: Point_23 & Point_24 평균
            if "Point_23" in pts and "Point_24" in pts:
                cx = (pts["Point_23"]["x"] + pts["Point_24"]["x"]) / 2
                cy = (pts["Point_23"]["y"] + pts["Point_24"]["y"]) / 2
                cz = (pts["Point_23"]["z"] + pts["Point_24"]["z"]) / 2
            else:
                cx, cy, cz = 0, 0, 0

            for joint_idx, joint_name in enumerate(keypoints):
                if joint_name in pts:
                    x = pts[joint_name]["x"] - cx
                    y = pts[joint_name]["y"] - cy
                    z = pts[joint_name]["z"] - cz
                    X_data[0, frame_idx, view_idx, joint_idx] = [x, y, z]
                    all_xyz.append([x, y, z])
    # (N*T*V*J, 3)
    all_xyz = np.array(all_xyz)
    mean = all_xyz.mean()
    std = all_xyz.std()

    # 전체 평균 기준으로 z-score 정규화
    X_data = (X_data - mean) / std

    return X_data, data.get("type_info", None)

def center_pose_sequence(X_data):
    X = X_data.copy()
    idx_23 = keypoints_2.index("Point_23")
    idx_24 = keypoints_2.index("Point_24")

    for t in range(X.shape[1]):
        center = (X[0, t, idx_23] + X[0, t, idx_24]) / 2
        X[0, t] = X[0, t] - center  # 모든 관절에서 중심 좌표 빼기

    return X

def normalize_pose_scale(X_data):
    X = X_data.copy()
    T, V = X.shape[1], X.shape[3]

    for t in range(T):
        joints = X[0, t, 0]  # shape (V, 3)
        dists = []
        for i in range(V):
            for j in range(i + 1, V):
                dists.append(np.linalg.norm(joints[i] - joints[j]))
        mean_dist = np.mean(dists)
        if mean_dist > 1e-8:
            X[0, t, 0] /= mean_dist  # 스케일 정규화
    return X

def preprocess_3d_pose(X_data):
    X_data = center_pose_sequence(X_data)
    # mean = np.load("train_mean.npy")
    # std = np.load("train_std.npy")
    # X_data = (X_data - mean) / std
    
    # (1, 16, joints, 3) → (N*T*V, C)
    flat = X_data.reshape(-1, X_data.shape[-1])  # shape: (16*V, 3)
    mean = np.mean(flat, axis=0)
    std = np.std(flat, axis=0) + 1e-8  # 0 나눗셈 방지용 epsilon

    X_data = (X_data - mean) / std
    X_data = normalize_pose_scale(X_data)
    return X_data

def process_json_data(json_data):
    """ JSON 데이터를 받아 1프레임 데이터를 추출하여 슬라이딩 윈도우에 추가 """
    frame_data = np.zeros((1, 1, num_joints, num_features), dtype=np.float32)

    if isinstance(json_data, list) and len(json_data) > 0:
        view3_data = json_data[0].get("view3", {}).get("pts", {})
    
    for joint_idx, joint_name in enumerate(keypoints):
        if joint_name in view3_data:
            frame_data[0, 0, joint_idx, 0] = view3_data[joint_name]["x"]
            frame_data[0, 0, joint_idx, 1] = view3_data[joint_name]["y"]
    
    return frame_data
    
def process_json_data_2(json_data):
    """ JSON 데이터를 받아 1프레임 데이터를 추출하여 슬라이딩 윈도우에 추가 """
    frame_data = np.zeros((1, 1, num_joints_2, num_features_2), dtype=np.float32)

    if isinstance(json_data, list) and len(json_data) > 0:
        view3_data = json_data[0].get("view3", {}).get("pts", {})
    
    for joint_idx, joint_name in enumerate(keypoints_2):
        if joint_name in view3_data:
            frame_data[0, 0, joint_idx, 0] = view3_data[joint_name]["x"]
            frame_data[0, 0, joint_idx, 1] = view3_data[joint_name]["y"]
            frame_data[0, 0, joint_idx, 2] = view3_data[joint_name]["z"]
    
    return frame_data

class PushUpPostureAnalyzer:
    def __init__(self, model):
        """
        ST-GCN 모델을 활용한 푸쉬업 자세 분석기.
        """
        self.model = model
        self.joint_indices = {
            "head": keypoints.index("Point_0"),
            "upper_back": keypoints.index("Point_11"),
            "lower_back": keypoints.index("Point_23"),
            "shoulder": keypoints.index("Point_11"),
            "elbow": keypoints.index("Point_13"),
            "wrist": keypoints.index("Point_15"),
            "left_wrist": keypoints.index("Point_15"),
            "right_wrist": keypoints.index("Point_16"),
            "left_elbow": keypoints.index("Point_13"),
            "right_elbow": keypoints.index("Point_14"),
            "left_hip": keypoints.index("Point_23"),
            "right_hip": keypoints.index("Point_24"),
            "left_knee": keypoints.index("Point_25"),
            "right_knee": keypoints.index("Point_26"),
            "left_ankle": keypoints.index("Point_27"),
            "right_ankle": keypoints.index("Point_28"),
            "chest": keypoints.index("Point_11")  # 가슴 (왼쪽 어깨)
        }

    def detect_faulty_posture(self, skeleton_sequence):
        """푸쉬업 동작을 분석하고 잘못된 자세를 감지합니다."""
        predictions = self.model.predict(skeleton_sequence)
        predicted_label = np.argmax(predictions, axis=-1)[0]
        confidence = predictions[0][predicted_label]

        logger.info(f"예측된 레이블: {predicted_label}, 확신도: {confidence * 100:.2f}%")
        logger.info(f"클래스 확률 분포: {predictions[0] * 100}")
        
        # ✅ 결과 저장
        if predicted_label == 0:
            result = f"✅ 올바른 자세 ({confidence * 100:.2f}% 확신)"
        else:
            result = f"❌ 잘못된 자세 감지 ({confidence * 100:.2f}% 확신)"

        faults = {}
        
        # ✅ 2. 뷰 차원이 1이면 squeeze() 적용
        if skeleton_sequence.shape[2] == 1:
            skeleton_sequence = np.squeeze(skeleton_sequence, axis=2)  # (batch, frames, joints, features)
            
        if predicted_label == 1:  # 잘못된 자세로 분류된 경우
            faults["척추"] = self.check_neutral_spine(skeleton_sequence)
            faults["가슴"] = self.check_chest_movement(skeleton_sequence)
            faults["손 위치"] = self.check_hand_position(skeleton_sequence)
            faults["머리 정렬"] = self.check_head_alignment(skeleton_sequence)
        
        return {k: v for k, v in faults.items() if v is not None}, result
    
    def check_neutral_spine(self, skeleton_sequence):
        """척추가 중립적인 상태를 유지하는지 확인합니다."""
        spine_joints = [self.joint_indices['upper_back'], self.joint_indices['lower_back']]
    
        upper_back = skeleton_sequence[..., spine_joints[0], :]
        lower_back = skeleton_sequence[..., spine_joints[1], :]
    
        spine_vector = upper_back - lower_back
        spine_angle = np.arctan2(spine_vector[..., 1], spine_vector[..., 0]) * (180 / np.pi)
    
        avg_spine_angle = np.mean(spine_angle)
        std_spine_angle = np.std(spine_angle)
    
        # print(f"평균 척추 각도: {avg_spine_angle:.2f}, 표준 편차: {std_spine_angle:.2f}")
    
        # ✅ 허용 범위 확대 (±20 → ±25), 표준 편차 기준 완화 (7 → 10)
        threshold_angle = 25  # 허용되는 최대 각도 차이
        threshold_std = 12  # 허용되는 표준 편차
        min_faulty_frames_ratio = 0.3  # 최소 30% 프레임 이상 벗어나야 경고
    
        # ✅ 몇 개의 프레임이 기준을 벗어났는지 계산
        faulty_frames = np.sum(np.abs(spine_angle - 90) > threshold_angle)
        total_frames = spine_angle.shape[0]
    
        faulty_ratio = faulty_frames / total_frames
    
        # print(f"기준 초과 프레임 비율: {faulty_ratio:.2f}")
    
        # ✅ 전체 프레임 중 30% 이상이 기준을 벗어난 경우에만 경고
        if faulty_ratio > min_faulty_frames_ratio and std_spine_angle > threshold_std:
            return "척추가 중립적이지 않습니다. 허리를 곧게 펴세요."
    
        return None

    def check_chest_movement(self, skeleton_sequence):
        """푸쉬업 중 가슴이 충분히 아래로 내려가는지 확인합니다."""
        chest_index = self.joint_indices['chest']

        # ✅ 1. 전체 프레임 수 계산
        num_frames = skeleton_sequence.shape[1]
        start_frame = int(num_frames * 0.2)  # 10% 지점 (푸쉬업 시작 구간 제외)
        end_frame = int(num_frames * 0.8)  # 90% 지점 (푸쉬업 끝 구간 제외)
    
        # ✅ 3. 푸쉬업 동작 중 가슴 높이(Y좌표)만 가져오기
        chest_positions = skeleton_sequence[:, start_frame:end_frame, chest_index, 1]  # Y좌표(높이) 추출
    
        # ✅ 4. 데이터 스무딩 적용 (Moving Average)
        chest_positions_smoothed = scipy.signal.savgol_filter(chest_positions, window_length=5, polyorder=2, axis=1)
    
        # ✅ 5. 이상치 제거: 하위 10% 백분위수를 `min_height`로 사용
        min_height = np.percentile(chest_positions_smoothed, 10)  
        max_height = np.percentile(chest_positions_smoothed, 90)  # 최대 높이
        median_height = np.median(chest_positions_smoothed)  # 중앙값 (수정된 비교 기준)
        movement_range = max_height - min_height  # 가슴이 이동한 거리
    
        # ✅ 6. 기준값을 조정 (기존 15% → 12%)
        threshold = np.median(chest_positions_smoothed) * 0.04 
    
        # print(f"가슴 높이 변화: {movement_range:.3f}, 허용 기준: {threshold:.3f}")
    
        # ✅ 9. 최소 65%의 프레임이 기준을 넘으면 정상으로 판단
        if movement_range < threshold:
            return "가슴이 충분히 내려가지 않았습니다. 몸을 더 낮추세요."
    
        return None

    def check_hand_position(self, skeleton_sequence):
        """손의 위치가 가슴과 일직선상에 있는지 확인합니다."""
        
        num_frames = skeleton_sequence.shape[1]
        start_frame = int(num_frames * 0.2)  # 푸쉬업 시작 및 끝 프레임 제외
        end_frame = int(num_frames * 0.8)  # ✅ 50% → 80%로 검사 범위 확장
        
        # ✅ 왼손과 오른손 모두 검사하도록 수정
        wrist_indices = [self.joint_indices['left_wrist'], self.joint_indices['right_wrist']]
        chest_index = self.joint_indices['chest']
        
        # ✅ 손목과 가슴의 X좌표 가져오기
        hand_positions = skeleton_sequence[:, start_frame:end_frame, wrist_indices, 0]  # (batch, frames, 2)
        chest_position = skeleton_sequence[:, start_frame:end_frame, chest_index, 0]  # (batch, frames)
        
        # ✅ 차원 맞추기 (브로드캐스팅 가능하게 변경)
        chest_position = chest_position[:, :, np.newaxis]  # (batch, frames, 1)
        
        # ✅ 각 프레임별 손-가슴 정렬 차이 계산 (평균값을 먼저 내지 않음)
        hand_misalignment_per_frame = np.abs(hand_positions - chest_position)
        
        # ✅ 모든 프레임에서 평균 오차 계산
        avg_hand_misalignment = np.mean(hand_misalignment_per_frame)
    
        # ✅ 허용 기준 조정 (기존 0.015 → 0.04)
        if avg_hand_misalignment > 0.04:
            return "손이 가슴과 정렬되지 않았습니다. 손의 위치를 조정하세요."
    
        return None
        
    def check_head_alignment(self, skeleton_sequence):
        """머리가 바르게 정렬되어 있는지 확인합니다."""
        head_index = self.joint_indices['head']
        neck_index = self.joint_indices['upper_back']
    
        # ✅ Head와 Neck 위치 비교 (Y축 및 X축 차이 계산)
        head_y_movement = np.abs(skeleton_sequence[:, :, head_index, 1] - skeleton_sequence[:, :, neck_index, 1])
    
        # ✅ 머리 움직임 스무딩 적용 (노이즈 제거)
        head_y_movement_smoothed = scipy.signal.savgol_filter(head_y_movement, window_length=5, polyorder=2, axis=1)

        # ✅ 머리가 너무 위아래로 흔들린 경우 감지 (기준: Y축 차이가 0.1 초과)
        head_y_misalignment_ratio = np.sum(head_y_movement_smoothed > 0.1) / head_y_movement.shape[1]
    
        # print(f"머리 전방 기울기 비율: {head_forward_ratio:.2f}, 머리 상하 움직임 비율: {head_y_misalignment_ratio:.2f}")
    
        # ✅ 60% 이상의 프레임에서 머리 정렬이 틀어졌다면 오류 발생
        if head_y_misalignment_ratio > 0.6:
            return "머리 위치가 올바르지 않습니다. 머리를 중립적으로 유지하세요."
    
        return None
    
    def provide_feedback(self, skeleton_sequence):
        """감지된 자세 오류를 기반으로 실시간 피드백을 제공합니다."""
        faults, result = self.detect_faulty_posture(skeleton_sequence)
        
        if not faults:
            return f"자세가 올바릅니다."

        feedback = f"다음 사항을 수정하세요: "

        for key, message in faults.items():
            feedback += f"\n - {message}"
        
        return feedback
    
class LungePostureAnalyzer:
    def __init__(self, model):
        self.model = model
        self.joint_indices = {
            "head": keypoints_2.index("Point_0"),
            "left_shoulder": keypoints_2.index("Point_11"),
            "right_shoulder": keypoints_2.index("Point_12"),
            "left_hip": keypoints_2.index("Point_23"),
            "right_hip": keypoints_2.index("Point_24"),
            "left_knee": keypoints_2.index("Point_25"),
            "right_knee": keypoints_2.index("Point_26"),
            "left_ankle": keypoints_2.index("Point_27"),
            "right_ankle": keypoints_2.index("Point_28"),
        }

    def detect_faulty_posture(self, skeleton_sequence):
        predictions = self.model.predict(skeleton_sequence)
        predicted_label = np.argmax(predictions, axis=-1)[0]
        confidence = predictions[0][predicted_label]

        logger.info(f"예측된 레이블: {predicted_label}, 확신도: {confidence * 100:.2f}%")
        logger.info(f"클래스 확률 분포: {predictions[0] * 100}")
        
        # ✅ 결과 저장
        if predicted_label == 0:
            result = f"✅ 올바른 자세 ({confidence * 100:.2f}% 확신)"
        else:
            result = f"❌ 잘못된 자세 감지 ({confidence * 100:.2f}% 확신)"
                
        faults = {}
        
        # ✅ 2. 뷰 차원이 1이면 squeeze() 적용
        if skeleton_sequence.shape[2] == 1:
            skeleton_sequence = np.squeeze(skeleton_sequence, axis=2)  # (batch, frames, joints, features)
            
        if predicted_label == 1:  # 잘못된 자세로 분류된 경우
            faults["발방향"] = self.check_feet_direction(skeleton_sequence)
            faults["어깨수평"] = self.check_shoulders_level(skeleton_sequence)
            faults["비틀림"] = self.check_body_twist(skeleton_sequence)
        
        return {k: v for k, v in faults.items() if v is not None}, result

    def calculate_vector_angle(self, v1, v2):
        dot = np.sum(v1 * v2, axis=-1)
        norm = (np.linalg.norm(v1, axis=-1) * np.linalg.norm(v2, axis=-1)) + 1e-6
        cos_angle = np.clip(dot / norm, -1.0, 1.0)
        angle = np.arccos(cos_angle)
        return np.degrees(angle)
        
    def check_feet_direction(self, skeleton_sequence):
        la = self.joint_indices['left_ankle']
        ra = self.joint_indices['right_ankle']
        
        baseline_left = skeleton_sequence[:, 0, la, 0]
        baseline_right = skeleton_sequence[:, 0, ra, 0]
        
        left_diff = np.abs(skeleton_sequence[:, :, la, 0] - baseline_left[:, None])
        right_diff = np.abs(skeleton_sequence[:, :, ra, 0] - baseline_right[:, None])

        left_x  = skeleton_sequence[:, :, la, 0]
        right_x = skeleton_sequence[:, :, ra, 0]
    
        left_range  = np.max(left_x) - np.min(left_x)
        right_range = np.max(right_x) - np.min(right_x)

        max_range = max(float(left_range), float(right_range))
        logger.info("\nfeet_range:", max_range)  
        
        max_diff = np.maximum(left_diff, right_diff)
        logger.info('\nmax_diff:', np.max(max_diff))  
        
        if max_range > 0.1:
            return "몸과 발의 방향이 일치하지 않습니다. 발의 방향을 정면으로 유지하세요"
        # la = self.joint_indices['left_ankle']
        # ra = self.joint_indices['right_ankle']
        # ls = self.joint_indices["left_shoulder"]
        # rs = self.joint_indices["right_shoulder"]
        # left_diff = np.abs(skeleton_sequence[:, :, ls, 0] - skeleton_sequence[:, :, la, 0])
        # right_diff = np.abs(skeleton_sequence[:, :, rs, 0] - skeleton_sequence[:, :, ra, 0])
        # avg_diff = np.mean(np.array([left_diff, right_diff]))
        # logger.info(f"avg_diff: {avg_diff}")
        # if avg_diff > 2.1:
        #     return "몸과 발의 방향이 일치하지 않습니다. 발의 방향을 정면으로 유지하세요"
        return None
        
    def check_shoulders_level(self, skeleton_sequence):
        ls = self.joint_indices["left_shoulder"]
        rs = self.joint_indices["right_shoulder"]
    
        shoulder_diff0 = np.abs(skeleton_sequence[:, 0, rs, 1] - skeleton_sequence[:, 0, ls, 1])
        shoulder_diff_total = np.abs(skeleton_sequence[:, :, rs, 1] - skeleton_sequence[:, :, ls, 1])
        exceed_amounts = shoulder_diff_total - shoulder_diff0[:, None]
        logger.info('\nexceed_amounts:', np.max(exceed_amounts))  
        if np.max(exceed_amounts > 0.1):  
            return "어깨 높이가 비대칭입니다. 양쪽 어깨를 수평으로 맞춰주세요."
        # ls = self.joint_indices["left_shoulder"]
        # rs = self.joint_indices["right_shoulder"]
        
        # shoulder_vec = skeleton_sequence[:, :, rs, :] - skeleton_sequence[:, :, ls, :]  # shape: (1, T, 3)
        
        # dy = shoulder_vec[:, :, 1]  # (1, T)
        # shoulder_length = np.linalg.norm(shoulder_vec, axis=-1) + 1e-6
        # angle = np.degrees(np.arcsin(dy / shoulder_length))  
        # avg_angle = np.mean(np.abs(angle))
        # logger.info(f"avg_angle: {avg_angle}")
        # if avg_angle > 20:  
        #     return "어깨 높이가 비대칭입니다. 양쪽 어깨를 수평으로 맞춰주세요."
#         dy = shoulder_vec[:, :, 1]  # (1, T)
#         shoulder_length = np.linalg.norm(shoulder_vec, axis=-1) + 1e-6
#         angle = np.degrees(np.arcsin(dy / shoulder_length))  
#         avg_angle = np.mean(np.abs(angle))
#         logger.info(f"avg_angle: {avg_angle}")
#         if avg_angle > 90:  
# #         shoulder_diff = np.abs(skeleton_sequence[:, :, ls, 1] - skeleton_sequence[:, :, rs, 1])
# #         avg_shoulder_diff = np.mean(shoulder_diff)       
# #         logger.info(f"ls: {skeleton_sequence[:, :, ls, 1]}")   
# #         logger.info(f"rs: {skeleton_sequence[:, :, rs, 1]}")
# #         logger.info(f"avg_shoulder_diff: {avg_shoulder_diff}")
# #         if avg_shoulder_diff > 2.7:  # 기준값은 데이터 스케일에 따라 조정
#             return "어깨 높이가 비대칭입니다. 양쪽 어깨를 수평으로 맞춰주세요."
        return None

    def check_body_twist(self, skeleton_sequence):
        ls = self.joint_indices["left_shoulder"]
        rs = self.joint_indices["right_shoulder"]
        lh = self.joint_indices["left_hip"]
        rh = self.joint_indices["right_hip"]
    
        shoulder_vec = skeleton_sequence[:, :, rs, :] - skeleton_sequence[:, :, ls, :]
        hip_vec = skeleton_sequence[:, :, rh, :] - skeleton_sequence[:, :, lh, :]
    
        # 회전량 측정: cross product의 y 성분
        twist = np.cross(shoulder_vec, hip_vec)[..., 1]  # y축 방향 회전 정도
        avg_twist = np.mean(np.abs(twist))
        logger.info(f"avg_twist: {avg_twist}")
        if avg_twist > 0.01:
            return "몸통이 비틀어져 있습니다. 정면을 유지하세요."
        return None

    def provide_feedback(self, skeleton_sequence):
        """감지된 자세 오류를 기반으로 실시간 피드백을 제공합니다."""
        faults, result = self.detect_faulty_posture(skeleton_sequence)
        
        if not faults:
            return f"자세가 올바릅니다."

        feedback = f"다음 사항을 수정하세요: "

        for key, message in faults.items():
            feedback += f"\n - {message}"
            
        return feedback

# FastAPI 앱 생성
app = FastAPI()

# 로그 설정 (파일 + 콘솔 출력)
logging.basicConfig(
    filename="/home/ubuntu/bucket/fastapi.log", 
    filemode="w", 
    format="%(asctime)s - %(levelname)s - %(message)s",  # 로그 형식
    level=logging.DEBUG)

logger = logging.getLogger("uvicorn")
logger.setLevel(logging.DEBUG)

# 콘솔 로그 핸들러 추가 (콘솔 + 파일 로그 저장)
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.DEBUG)
formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
console_handler.setFormatter(formatter)
logger.addHandler(console_handler)

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
JSON_DIR = "/home/ubuntu/bucket/skeletonData"
os.makedirs(JSON_DIR, exist_ok=True)

# ST-GCN 모델 로드 (미리 저장된 모델 파일 경로)
num_joints = 21  
num_features = 2 
num_classes = 2  
adjacency_matrix_norm = np.load("adjacency_matrix.npy")
def pushup_load():

    model = STGCN(num_joints, num_features, adjacency_matrix_norm, num_classes)

    dummy_input = np.random.rand(1, 10, num_joints, num_features).astype(np.float32)
    model(dummy_input)

    model.load_weights("stgcn_model6.weights.h5")

    pushup_analyzer = PushUpPostureAnalyzer(model)

    return pushup_analyzer

num_joints_2 = 14 
num_features_2 = 3 
num_classes_2 = 2  
adjacency_matrix_norm_2 = np.load("adjacency_matrix_sport2.npy")

def lunge_load():
    model = STGCN_sport2(num_joints_2, num_features_2, adjacency_matrix_norm_2, num_classes_2)

    dummy_input = np.random.rand(1, 10, num_joints_2, num_features_2).astype(np.float32)
    model(dummy_input)

    model.load_weights("stgcn_model_sport2_C1.weights.h5")
    
    lunge_analyzer = LungePostureAnalyzer(model)

    return lunge_analyzer

current_exercise = None

@app.websocket("/ws/connect")
async def receive_json(websocket: WebSocket):
    # AI 모델이 WebSocket을 통해 백엔드의 JSON 데이터를 받고 저장하는 WebSocket 서버
    await websocket.accept()

    global current_exercise
    
    #print("FastAPI WebSocket 연결 성공!")
    logger.info("FastAPI WebSocket 연결 성공!")
    try:    
        while True:
            try:
                # 백엔드 서버에서 json데이터 수신
                data = await websocket.receive_text()
                msg = json.loads(data)

                logger.info(f"메시지 수신됨 test: {data}")

                msg_type = msg.get("type")

                # 운동 선택인 경우
                if msg_type == "select":
                    exercise = msg.get("exercise")
                    logger.info("운동 선택 확인")
                    analyzer = None
                    if exercise == "pushup":
                        #푸쉬업 모델 실행
                        analyzer = pushup_load()
                        frame_buffer.clear()
                        logger.info("푸쉬업 모델 로딩 완료")
                    elif exercise == "lunge":
                        # 런지 모델 실행
                        analyzer = lunge_load()
                        frame_buffer.clear()
                        logger.info("런지 모델 로딩 완료")
                    else:
                        await websocket.send_text("지원하지 않는 운동입니다.")
                        continue

                    current_exercise = exercise
                    await websocket.send_text(f"{exercise} 모델 로딩 완료")

                # 스켈레톤 데이터인 경우
                elif msg_type == "pose":
                    json_data = msg.get("frames",[])
                    logger.info("스켈레톤 데이터 확인")
                    if current_exercise is None:
                        await websocket.send_text("아직 운동이 선택되지 않았습니다.")
                        continue
                    logger.info(f"3 current_exercise = {current_exercise}")
                    # 버퍼 초기화
                    if current_exercise == "pushup":
                        logger.info(f"4 current_exercise = {current_exercise}")
                        new_frame = process_json_data(json_data)
                    else:
                        logger.info(f"6 current_exercise = {current_exercise}")
                        new_frame = process_json_data_2(json_data)
                    frame_buffer.append(new_frame)

                    # 피드백 생성
                    if len(frame_buffer) == 16:
                        skeleton_sequence = np.concatenate(frame_buffer, axis=1)
                        if skeleton_sequence.ndim == 5 and skeleton_sequence.shape[2] == 1:
                            skeleton_sequence = np.squeeze(skeleton_sequence, axis=2)
                        logger.info(f"[모델 입력 shape]: {skeleton_sequence.shape}")
                        skeleton_sequence = preprocess_3d_pose(skeleton_sequence)
                        logger.info(f"[정규화 후] mean: {np.mean(skeleton_sequence):.4f}, std: {np.std(skeleton_sequence):.4f}")
                        feedback = analyzer.provide_feedback(skeleton_sequence)
                    
                        response = {
                            "status": "success",
                            "message": "AI process OK",
                            "prediction_result": feedback
                        }
                        await websocket.send_text(json.dumps(response, ensure_ascii=False))
                    
                        #print(f"spring로 응답 전송:{response}")
                        logger.info(f"spring로 응답 전송:{response}")

                        for _ in range(step_size):
                            if frame_buffer:
                                frame_buffer.popleft()  # 가장 오래된 프레임 제거

                # json 파일 저장
                #json_file_path = f"{JSON_DIR}/user.json"
                #with open(json_file_path, "w", encoding="utf-8") as json_file:
                    #json.dump(json_data, json_file, indent=4, ensure_ascii=False)

                if len(frame_buffer) == 16:
                    skeleton_sequence = np.concatenate(frame_buffer, axis=1)  # (1, 32, joints, features)
                    feedback = analyzer.provide_feedback(skeleton_sequence)
                    
                    response = {
                        "status": "success",
                        "message": "AI process OK",
                        "prediction_result": feedback
                    }
                    await websocket.send_text(json.dumps(response, ensure_ascii=False))
                    
                    #print(f"spring로 응답 전송:{response}")
                    logger.info(f"spring로 응답 전송:{response}")

                    for _ in range(step_size):
                        if frame_buffer:
                            frame_buffer.popleft()  # 가장 오래된 프레임 제거

            except Exception as e:
                logger.error(f"메시지 수신 중 오류:{e}")
                print(f"Websocket Error: {e}")
                break
    except Exception as e:
        logger.error("websocket 세션 종료: {e}")
        print(f"Websocket Error: {e}")
    finally:
        print("Websocket 종료 처리 완료.")
