package com.example.sportsanalysis

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.sportsanalysis.domain.model.PoseFrame
import com.example.sportsanalysis.domain.model.PoseLandmark
import com.example.sportsanalysis.domain.pose.PoseDetector
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

class AndroidPoseDetector(private val context: Context) : PoseDetector {

    private var landmarker: PoseLandmarker? = null
    private val frameFlow = MutableSharedFlow<PoseFrame>(replay = 0, extraBufferCapacity = 4)
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    var previewProvider: ((Preview) -> Unit)? = null

    override suspend fun initialize() {
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
                // Use the result's own timestamp (in ms) — MPImage.timestamp is not public.
                publishResult(result, result.timestampMs())
            }
            .setErrorListener { e -> e.printStackTrace() }
            .build()

        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    override fun detectStream(): Flow<PoseFrame> = frameFlow.asSharedFlow()

    suspend fun bindToLifecycle(lifecycleOwner: LifecycleOwner) {
        val cameraProvider = awaitCameraProvider(context)
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().also {
            previewProvider?.invoke(it)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also {
                it.setAnalyzer(cameraExecutor) { proxy -> processFrame(proxy) }
            }

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    }

    private fun processFrame(proxy: ImageProxy) {
        val bmp = proxy.toBitmap()
        val mpImage = BitmapImageBuilder(bmp).build()
        // MediaPipe LIVE_STREAM expects a monotonically-increasing timestamp in ms.
        landmarker?.detectAsync(mpImage, proxy.imageInfo.timestamp / 1_000_000L)
        proxy.close()
    }

    private fun publishResult(result: PoseLandmarkerResult, timestampMs: Long) {
        val landmarks = result.landmarks().firstOrNull() ?: return
        val mapped = landmarks.map { lm ->
            PoseLandmark(
                x = lm.x(),
                y = lm.y(),
                z = lm.z(),
                visibility = lm.visibility().orElse(1f)
            )
        }
        frameFlow.tryEmit(PoseFrame(timestampMs = timestampMs, landmarks = mapped))
    }

    override suspend fun release() {
        landmarker?.close()
        landmarker = null
        cameraExecutor.shutdown()
    }

    private suspend fun awaitCameraProvider(ctx: Context): ProcessCameraProvider =
        suspendCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener(
                { cont.resume(future.get()) },
                ContextCompat.getMainExecutor(ctx)
            )
        }
}

class PoseDetectorFactory(private val context: Context) {
    fun create(): PoseDetector = AndroidPoseDetector(context)
}
