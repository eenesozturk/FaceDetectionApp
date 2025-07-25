package com.enesozturk.facedetection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "MLKit";
    private PreviewView previewView;
    private TextView faceInfoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        faceInfoText = findViewById(R.id.faceInfoText);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .enableTracking()
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

                FaceDetector detector = FaceDetection.getClient(options);

                ImageAnalysis analysis = new ImageAnalysis.Builder().build();
                analysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    @SuppressWarnings("UnsafeOptInUsageError")
                    Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage,
                                imageProxy.getImageInfo().getRotationDegrees());

                        detector.process(image)
                                .addOnSuccessListener(faces -> {
                                    StringBuilder info = new StringBuilder();
                                    if (faces.isEmpty()) {
                                        info.append("Yüz algılanamadı.");
                                    } else {
                                        for (Face face : faces) {
                                            Float leftEye = face.getLeftEyeOpenProbability();
                                            Float rightEye = face.getRightEyeOpenProbability();
                                            Float smileProb = face.getSmilingProbability();

                                            info.append("Sol Göz: ")
                                                    .append(rightEye != null ? (rightEye > 0.5 ? "Açık" : "Kapalı") : "Bilinmiyor").append("\n");

                                            info.append("Sağ Göz: ")
                                                    .append(leftEye != null ? (leftEye > 0.5 ? "Açık" : "Kapalı") : "Bilinmiyor").append("\n");

                                            info.append("Ağız: ")
                                                    .append(smileProb != null ? (smileProb > 0.5 ? "Açık" : "Kapalı") : "Bilinmiyor").append("\n");
                                        }
                                    }
                                    faceInfoText.setText(info.toString());
                                    imageProxy.close();
                                })
                                .addOnFailureListener(e -> {
                                    faceInfoText.setText("Algılama hatası: " + e.getMessage());
                                    imageProxy.close();
                                });
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
}
