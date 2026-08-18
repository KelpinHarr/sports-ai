package com.example.sportsanalysis.domain.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.sportsanalysis.domain.model.PoseFrame
import com.example.sportsanalysis.domain.model.PoseLandmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidPoseDetector(
    private val context: Context
) : PoseDetector {

    private var landmarker: PoseLandmarker? = null

    private val frameFlow = MutableSharedFlow<PoseFrame>(
        replay = 0,
        extraBufferCapacity = 4
    )

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    var previewProvider: ((Preview) -> Unit)? = null

    override fun initialize() {
        if (landmarker != null) return

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .setDelegate(Delegate.GPU)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setNumPoses(1)
            .setResultListener { result, _ ->
                publishResult(
                    result = result,
                    timestampMs = result.timestampMs()
                )
            }
            .setErrorListener { error ->
                error.printStackTrace()
            }
            .build()

        landmarker = PoseLandmarker.createFromOptions(
            context,
            options
        )
    }

    override fun detectStream(): Flow<PoseFrame> {
        return frameFlow.asSharedFlow()
    }

    suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner
    ) {
        val cameraProvider = awaitCameraProvider(context)

        cameraProvider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also { preview ->
                previewProvider?.invoke(preview)
            }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            )
            .build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer(
                    cameraExecutor
                ) { imageProxy ->
                    processFrame(imageProxy)
                }
            }

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    }

    private fun processFrame(proxy: ImageProxy) {
        val bitmap = proxy.toBitmap()
        val rotationDegrees = proxy.imageInfo.rotationDegrees

        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }

            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        } else {
            bitmap
        }

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        landmarker?.detectAsync(
            mpImage,
            proxy.imageInfo.timestamp / 1_000_000L
        )

        proxy.close()
    }

    private fun publishResult(
        result: PoseLandmarkerResult,
        timestampMs: Long
    ) {
        val landmarks =
            result.landmarks().firstOrNull()
                ?: return

        val mapped = landmarks.map { landmark ->
            PoseLandmark(
                x = landmark.x(),
                y = landmark.y(),
                z = landmark.z(),
                visibility = landmark.visibility().orElse(1f)
            )
        }

        frameFlow.tryEmit(
            PoseFrame(
                timestampMs = timestampMs,
                landmarks = mapped
            )
        )
    }

    override fun release() {
        landmarker?.close()
        landmarker = null
        cameraExecutor.shutdown()
    }

    private suspend fun awaitCameraProvider(
        context: Context
    ): ProcessCameraProvider =
        suspendCoroutine { continuation ->
            val future =
                ProcessCameraProvider.Companion.getInstance(context)

            future.addListener(
                {
                    continuation.resume(
                        future.get()
                    )
                },
                ContextCompat.getMainExecutor(context)
            )
        }
}