import tensorflow as tf
from tensorflow.keras import layers
import numpy as np

class GraphConvLayer(layers.Layer):
    def __init__(self, units, adjacency_matrix):
        super(GraphConvLayer, self).__init__()
        self.units = units
        self.adjacency_matrix = tf.constant(adjacency_matrix, dtype=tf.float32)
        self.batch_norm = layers.BatchNormalization()

    def build(self, input_shape):
        self.kernel = self.add_weight(
            shape=(input_shape[-1], self.units),
            initializer="glorot_uniform",
            trainable=True
        )

    def call(self, inputs):
        x = tf.linalg.matmul(self.adjacency_matrix, inputs)
        x = tf.linalg.matmul(x, self.kernel)
        x = self.batch_norm(x)
        x = tf.nn.leaky_relu(x)
        return x

@tf.keras.utils.register_keras_serializable()
class STGCN_sport2(tf.keras.Model):
    def __init__(self, num_joints, num_features, adjacency_matrix, num_classes, **kwargs):
        super(STGCN_sport2, self).__init__(**kwargs)
        self.num_joints = num_joints
        self.num_features = num_features
        self.num_classes = num_classes
        self.adjacency_matrix = tf.convert_to_tensor(adjacency_matrix, dtype=tf.float32)
        self.graph_conv1 = GraphConvLayer(64, adjacency_matrix)
        self.graph_conv2 = GraphConvLayer(128, adjacency_matrix)
        self.temporal_conv1 = layers.Conv1D(128, kernel_size=7, padding="same")
        self.temporal_conv2 = layers.Conv1D(64, kernel_size=7, padding="same")
        self.batch_norm1 = layers.BatchNormalization()
        self.batch_norm2 = layers.BatchNormalization()
        self.activation = layers.Activation("relu")
        self.global_pool = layers.GlobalAveragePooling1D()
        self.fc = layers.Dense(num_classes, activation="softmax", kernel_regularizer=tf.keras.regularizers.l2(0.01))
        self.dropout = layers.Dropout(0.3)

    def call(self, inputs):
        if len(inputs.shape) == 5:
            inputs = tf.reduce_mean(inputs, axis=2)
        batch_size = tf.shape(inputs)[0]
        frames = tf.shape(inputs)[1]
        joints = tf.shape(inputs)[2]
        features = tf.shape(inputs)[3]
        x = tf.reshape(inputs, (batch_size * frames, joints, features))
        x = self.graph_conv1(x)
        x = self.batch_norm1(x)
        x = self.activation(x)
        x = self.graph_conv2(x)
        x = self.batch_norm2(x)
        x = self.activation(x)
        out_features = tf.shape(x)[-1]
        x = tf.reshape(x, (batch_size, frames, joints * out_features))
        x = self.temporal_conv1(x)
        x = self.temporal_conv2(x)
        x = self.global_pool(x)
        x = self.dropout(x)
        return self.fc(x)
    def get_config(self):
        config = super(STGCN_sport2, self).get_config()
        config.update({
            "num_joints": self.num_joints,
            "num_features": self.num_features,
            "adjacency_matrix_sport2": self.adjacency_matrix_sport2.numpy().tolist(), 
            "num_classes": self.num_classes,
            "name": self.name  
        })
        return config
    @classmethod
    def from_config(cls, config):
        import numpy as np
        config["adjacency_matrix_sport2"] = np.array(config["adjacency_matrix_sport2"])
        return cls(**config)