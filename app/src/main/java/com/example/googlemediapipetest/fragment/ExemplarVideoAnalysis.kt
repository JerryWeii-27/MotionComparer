package com.example.googlemediapipetest.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.googlemediapipetest.gles.GLRenderer
import com.example.googlemediapipetest.R
import com.example.googlemediapipetest.Vector3
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class ExemplarVideoAnalysis : Fragment(R.layout.fragment_exemplar_video_analysis)
{
    private var poseLandmarker : PoseLandmarker? = null
    val modelName = "pose_landmarker_heavy.task"
    lateinit var buttonPickVideo : Button
    lateinit var pvPlayerView : PlayerView
    lateinit var player : ExoPlayer
    lateinit var pbDetectionProgress : ProgressBar
    lateinit var glRenderer : GLRenderer
    lateinit var glSurfaceView : GLSurfaceView

    public var sampleIntervalMs : Long = 1000;
    var videoWidth : Int = -1;
    var videoHeight : Int = -1;

    private lateinit var backgroundExecutor : ScheduledExecutorService

    override fun onCreateView(
        inflater : LayoutInflater,
        container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View?
    {
        return inflater.inflate(R.layout.fragment_exemplar_video_analysis, container, false)
    }

    override fun onViewCreated(view : View, savedInstanceState : Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonPickVideo = view.findViewById(R.id.buttonExemplarPickVideo)

        pvPlayerView = view.findViewById(R.id.pvExemplarPlayerView)
        player = ExoPlayer.Builder(requireContext()).build()
        pvPlayerView.player = player

        buttonPickVideo.setOnClickListener() {
            openVideoPicker()
        }

        pbDetectionProgress = view.findViewById(R.id.pbExemplarDetectionProgress)
        pbDetectionProgress.setProgress(0, true)



        initPoseLandmarker()

        // OpenGL

        glSurfaceView = view.findViewById<GLSurfaceView>(R.id.glSurfaceView)
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.setZOrderOnTop(true);


        glRenderer = GLRenderer(requireContext())
        glRenderer.sampleInterval = sampleIntervalMs.toInt()

        glSurfaceView.setRenderer(glRenderer)

        @SuppressLint("ClickableViewAccessibility")
        glSurfaceView.setOnTouchListener() { _, event -> glRenderer.onTouchEvent(event) }
    }

    private fun initPoseLandmarker()
    {
        try
        {
            val baseOptionBuilder = BaseOptions.builder()
//            baseOptionBuilder.setDelegate(Delegate.GPU)
            baseOptionBuilder.setModelAssetPath(modelName)

            val baseOptions = baseOptionBuilder.build()

            val optionsBuilder =
                PoseLandmarker.PoseLandmarkerOptions.builder().setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(0.5f).setMinTrackingConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f).setRunningMode(RunningMode.VIDEO)
                    .setNumPoses(1)

            val options = optionsBuilder.build()
            poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options)
        } catch (e : RuntimeException)
        {
            Log.e("GPUDelegation", "GPU delegation error.")
        }
    }

    private fun openVideoPicker()
    {
        PictureSelector.create(this).openSystemGallery(SelectMimeType.ofVideo())
            .forSystemResult(object : OnResultCallbackListener<LocalMedia>
            {
                override fun onResult(result : java.util.ArrayList<LocalMedia>)
                {
                    handleVideoSelection(result)
                    Toast.makeText(requireContext(), "Selection made.", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onCancel()
                {
                    Toast.makeText(requireContext(), "Selection canceled.", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun handleVideoSelection(result : ArrayList<LocalMedia>)
    {
        if (result.isNotEmpty())
        {
            pbDetectionProgress.setProgress(0, true)
            val videoPath = result[0].availablePath.toString().toUri()
            Toast.makeText(requireContext(), "Selection made: $videoPath", Toast.LENGTH_LONG)
                .show()

            val mediaItem = MediaItem.fromUri(videoPath)

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
            backgroundExecutor.execute {
                runDetectOnVideo(videoPath)?.let { resultBundle ->
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Running UI thread code.",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i("MPDetectionProgress", "Running UI thread code.")
                        glSurfaceView.queueEvent { processResultBundle(resultBundle) }
                    }
                }
            }
        }
        else
        {
            Toast.makeText(requireContext(), "No video selected.", Toast.LENGTH_SHORT).show()
        }
    }

    // Run on background thread.
    private fun runDetectOnVideo(videoUri : Uri) : ResultBundle?
    {
        initPoseLandmarker()
        val startTime = SystemClock.uptimeMillis()
        Log.i("MPDetectionProgress", "runDetectOnVideo at $startTime")

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(requireContext(), videoUri)
        val videoLengthMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()

        val firstFrame = retriever.getFrameAtTime(0)

        if (firstFrame == null)
        {
            Log.e("MPDetectionProgress", "runDetectOnVideo: First frame is null.")
            return null
        }

        videoWidth = firstFrame.width
        videoHeight = firstFrame.height

        if (videoLengthMs == null)
        {
            Log.e("MPDetectionProgress", "runDetectOnVideo: Video length is null.")
            return null
        }

        val numberOfFramesToDetect = videoLengthMs / sampleIntervalMs
        val resultList = mutableListOf<PoseLandmarkerResult>()

        var currentProgressPercent : Int = 0

        for (i in 0..numberOfFramesToDetect)
        {
            val timeStampMs = i * sampleIntervalMs
            Log.i("MPDetectionProgress", "Detecting $timeStampMs out of $videoLengthMs.")

            retriever.getFrameAtTime(timeStampMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                ?.let { frame ->

                    // Convert the video frame to ARGB_8888 which is required by the MediaPipe
                    val argb8888Frame = if (frame.config == Bitmap.Config.ARGB_8888) frame
                    else frame.copy(Bitmap.Config.ARGB_8888, false)

                    // Convert the input Bitmap object to an MPImage object to run inference
                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    Log.i("MPDetectionProgress", "runDetectOnVideo: Calling poseLandmarker.")
                    poseLandmarker?.detectForVideo(mpImage, timeStampMs)?.let { detectionResult ->
                        resultList.add(detectionResult)
                    } ?: {
                        Log.e("MPDetectionProgress", "Frame could not be detected.")
                    }
                } ?: {
                Log.e("MPDetectionProgress", "Frame could not be retrieved.")
            }

            val newProgressPercent = (i * 100.0 / numberOfFramesToDetect).toInt()
            if (newProgressPercent >= currentProgressPercent + 5)
            {
                activity?.runOnUiThread {
                    pbDetectionProgress.setProgress(currentProgressPercent, true)
                }
                currentProgressPercent = newProgressPercent
            }
        }

        val endTime = SystemClock.uptimeMillis()
        val timeTaken = "%.1f".format((endTime - startTime) / 1000.0)
        Log.i(
            "MPDetectionProgress",
            "Detection finished at $endTime. Time Taken: $timeTaken seconds."
        )

        return ResultBundle(resultList, sampleIntervalMs, videoHeight, videoWidth)
    }

    private fun processResultBundle(resultBundle : ResultBundle)
    {
        Log.i("OpenGLThread", "OpenGL running on thread: ${Thread.currentThread().id}")
        // Go through every frame in result bundle.
        // For each frame:
        // - updateJoints(frame)
        // - Data of that frame saved to a mutableList
        // - List added to floatArray
        // -
        glRenderer.newHumanModel()
        glRenderer.humanModel.totalFrames = resultBundle.resultsList.size
        val s = resultBundle.resultsList.size
        Log.i("OpenGL", "processResultBundle: $s")

        for (i in 0 until resultBundle.resultsList.size)
        {

            val landmarksList = resultBundle.resultsList[i]
            Log.i("LandmarksList", landmarksList.toString())

            val jointsVec3Arr = landmarksToVec3Arr(landmarksList)

            glRenderer.humanModel.updateJointsForSingleFrame(i, jointsVec3Arr)
        }

        glRenderer.humanModel.bindVBO()
        glRenderer.humanModel.allFramesAdded = true
    }

    fun camSpaceLandmarksToVec3Arr(result : PoseLandmarkerResult) : Array<Vector3>
    {
        val landmarksList = result.landmarks()
        val jointsVec3Arr = Array(33) { Vector3() }

        var index : Int = 0
        for (sublist in landmarksList)
        {
            for (landmark in sublist)
            {
                val x = (landmark.x())
                val y = (-landmark.y())
                val z = (landmark.z())

                jointsVec3Arr[index++] = Vector3(x, y, z)

//                    Log.i("Landmarks", "Landmark at (x=$x, y=$y, z=$z).")
            }
        }

        Log.i("Landmarks", "")

        return jointsVec3Arr
    }

    fun landmarksToVec3Arr(result : PoseLandmarkerResult) : Array<Vector3>
    {
        val landmarksList = result.worldLandmarks()
        val jointsVec3Arr = Array(33) { Vector3() }

        var index : Int = 0
        for (sublist in landmarksList)
        {
            for (landmark in sublist)
            {
                val x = landmark.x()
                val y = -landmark.y()
                val z = landmark.z()

                jointsVec3Arr[index++] = Vector3(x, y, z)

//                    Log.i("Landmarks", "Landmark at (x=$x, y=$y, z=$z).")
            }
        }

        return jointsVec3Arr
    }

    data class ResultBundle(
        val resultsList : List<PoseLandmarkerResult>,
        val inferenceTime : Long,
        val inputImageHeight : Int,
        val inputImageWidth : Int,
    )
}