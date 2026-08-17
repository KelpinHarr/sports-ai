import AVFoundation
import UIKit
import MediaPipeTasksVision   // From CocoaPods: pod 'MediaPipeTasksVision'
import ComposeApp

/// A UIView that owns the AVCaptureSession and runs MediaPipe PoseLandmarker.
/// It pushes results back to the shared Kotlin `IosPoseDetector` via the
/// detector instance that Koin provided to the Compose ViewModel.
final class CameraView: UIView {
    private let session = AVCaptureSession()
    private var landmarker: PoseLandmarker?
    private let videoQueue = DispatchQueue(label: "video.queue")

    /// Resolved lazily from Koin once Compose has initialised the graph.
    private var detector: IosPoseDetector? {
        // KoinHelper is a tiny shim you add to commonMain that exposes
        // `PoseDetectorFactoryKt.create()` for Swift. See README.
        return KoinHelperKt.providePoseDetector() as? IosPoseDetector
    }

    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupCamera()
        setupLandmarker()
    }
    required init?(coder: NSCoder) { fatalError() }

    private func setupCamera() {
        previewLayer.session = session
        previewLayer.videoGravity = .resizeAspectFill

        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: device) else { return }
        session.addInput(input)

        let output = AVCaptureVideoDataOutput()
        output.setSampleBufferDelegate(self, queue: videoQueue)
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        session.addOutput(output)

        DispatchQueue.global().async { self.session.startRunning() }
    }

    private func setupLandmarker() {
        let modelPath = Bundle.main.path(forResource: "pose_landmarker_lite", ofType: "task")!
        let options = PoseLandmarkerOptions()
        options.baseOptions.modelAssetPath = modelPath
        options.runningMode = .liveStream
        options.minPoseDetectionConfidence = 0.5
        options.minTrackingConfidence = 0.5
        options.poseLandmarkerLiveStreamDelegate = self
        landmarker = try? PoseLandmarker(options: options)
    }
}

extension CameraView: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput,
                       didOutput sampleBuffer: CMSampleBuffer,
                       from connection: AVCaptureConnection) {
        guard let landmarker = landmarker,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        do {
            let mpImage = try MPImage(pixelBuffer: pixelBuffer)
            let timestampMs = Int(CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer)) * 1000)
            try landmarker.detectAsync(image: mpImage, timestampInMilliseconds: timestampMs)
        } catch {
            print("MediaPipe error:", error)
        }
    }
}

extension CameraView: PoseLandmarkerLiveStreamDelegate {
    func poseLandmarker(_ landmarker: PoseLandmarker,
                        didFinishDetection result: PoseLandmarkerResult?,
                        timestampInMilliseconds: Int,
                        error: Error?) {
        guard let result = result, let landmarks = result.landmarks.first else { return }
        // Flatten to [x,y,z,visibility, …] for the Kotlin side
        var flat: [Float] = []
        flat.reserveCapacity(landmarks.count * 4)
        for lm in landmarks {
            flat.append(Float(lm.x))
            flat.append(Float(lm.y))
            flat.append(Float(lm.z))
            flat.append(Float(lm.visibility?.floatValue ?? 1.0))
        }
        let array = KotlinFloatArray(size: Int32(flat.count))
        for (i, v) in flat.enumerated() { array.set(index: Int32(i), value: v) }
        detector?.submitFromSwift(timestampMs: Int64(timestampInMilliseconds), flat: array)
    }
}

/// Exported to Kotlin as `provideCameraView()` via Swift→Obj-C bridge.
@_cdecl("provideCameraView")
public func provideCameraView() -> UIView {
    return CameraView(frame: .zero)
}
