import tensorflow as tf
from tensorflow.keras import layers
import numpy as np

class GraphConvLayer(layers.Layer):
    def __init__(self, units, adjacency_matrix):
        super(GraphConvLayer, self).__init__()
        self.units = units
        self.adjacency_matrix = tf.Variable(adjacency_matrix, dtype=tf.float32, trainable=False)
        self.batch_norm = layers.BatchNormalization()  # 배치 정규화 추가

    def build(self, input_shape):
        # input_shape: (batch*frames, joints, features)
        self.kernel = self.add_weight(
            shape=(input_shape[-1], self.units),
            initializer="glorot_uniform",
            trainable=True
        )

    def call(self, inputs):
        # inputs: (batch*frames, joints, features)
        x = tf.linalg.matmul(self.adjacency_matrix, inputs)  # Graph convolution
        x = tf.linalg.matmul(x, self.kernel)  # Apply learnable weights
        x = self.batch_norm(x)  # Batch normalization
        x = tf.nn.leaky_relu(x)  # Activation function applied AFTER batch norm
        return x

# ✅ ST-GCN 모델 정의
@tf.keras.utils.register_keras_serializable()
class STGCN(tf.keras.Model):
    def __init__(self, num_joints, num_features, adjacency_matrix, num_classes, **kwargs):
        super(STGCN, self).__init__(**kwargs)

        self.num_joints = num_joints
        self.num_features = num_features
        self.num_classes = num_classes
        self.adjacency_matrix = tf.convert_to_tensor(adjacency_matrix, dtype=tf.float32)  # 텐서로 변환
        
        self.graph_conv1 = GraphConvLayer(64, adjacency_matrix)
        self.graph_conv2 = GraphConvLayer(128, adjacency_matrix)
        self.graph_conv3 = GraphConvLayer(256, adjacency_matrix)  # 추가된 Graph Conv
        self.graph_conv4 = GraphConvLayer(512, adjacency_matrix)  # 추가된 Graph Conv
        
        self.temporal_conv1 = layers.Conv1D(512, kernel_size=7, padding="same")
        self.temporal_conv2 = layers.Conv1D(256, kernel_size=7, padding="same")
        self.temporal_conv3 = layers.Conv1D(128, kernel_size=5, padding="same")
        self.temporal_conv4 = layers.Conv1D(64, kernel_size=3, padding="same")
        
        self.batch_norm1 = layers.BatchNormalization()
        self.batch_norm2 = layers.BatchNormalization()
        self.batch_norm3 = layers.BatchNormalization()
        self.batch_norm4 = layers.BatchNormalization()
        
        self.activation = layers.Activation("relu")
        self.global_pool = layers.GlobalAveragePooling1D()
        self.fc = layers.Dense(num_classes, activation="softmax", kernel_regularizer=tf.keras.regularizers.l2(0.1))
        self.dropout = layers.Dropout(0.5) 

    def build(self, input_shape):
        super().build(input_shape)

    def call(self, inputs):
        # ✅ 입력 처리: (batch, frames, views, joints, features)
        if len(inputs.shape) == 5:
            # 여러 각도(View) 데이터가 있는 경우 평균 내기
            inputs = tf.reduce_mean(inputs, axis=2)  # (batch, frames, joints, features)
        
        batch_size = tf.shape(inputs)[0]
        frames = tf.shape(inputs)[1]
        joints = tf.shape(inputs)[2]
        features = tf.shape(inputs)[3]
        x = tf.reshape(inputs, (batch_size * frames, joints, features))  # (batch * joints, frames, features)
        
        # ✅ 모델 처리
        x = self.graph_conv1(x)
        x = self.batch_norm1(x)
        x = self.activation(x)
        
        x = self.graph_conv2(x)
        x = self.batch_norm2(x)
        x = self.activation(x)

        x = self.graph_conv3(x)
        x = self.batch_norm3(x)
        x = self.activation(x)

        x = self.graph_conv4(x)
        x = self.batch_norm4(x)
        x = self.activation(x)

        # Temporal Conv를 위해 프레임별로 모든 관절의 정보를 하나의 벡터로 결합
        out_features = tf.shape(x)[-1]
        x = tf.reshape(x, (batch_size, frames, joints * out_features))

        x = self.temporal_conv1(x)
        x = self.temporal_conv2(x)
        x = self.temporal_conv3(x)
        x = self.temporal_conv4(x)

        # frames 차원을 평균 내어 최종 특징 벡터 생성
        x = self.global_pool(x)
        x = self.dropout(x)
        
        return self.fc(x)

    def get_config(self):
        config = super(STGCN, self).get_config()
        config.update({
            "num_joints": self.num_joints,
            "num_features": self.num_features,
            "adjacency_matrix": self.adjacency_matrix.numpy().tolist(),  # ✅ 변경: numpy() 사용
            "num_classes": self.num_classes,
            "name": self.name  # 👈 추가
        })
        return config


    @classmethod
    def from_config(cls, config):
        import numpy as np
        config["adjacency_matrix"] = np.array(config["adjacency_matrix"])  # 리스트를 numpy 배열로 변환
        return cls(**config)