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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.OptIn
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
import com.google.mediapipe.tasks.core.Delegate
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
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.core.graphics.scale
import androidx.media3.exoplayer.SeekParameters
import com.example.googlemediapipetest.BitmapHelper
import com.example.googlemediapipetest.ResultBundle
import com.example.googlemediapipetest.gles.FlatSkeleton

class ExemplarVideoAnalysis : Fragment(R.layout.fragment_exemplar_video_analysis)
{
    private var poseLandmarker : PoseLandmarker? = null
    val modelName = "pose_landmarker_heavy.task"

    lateinit var buttonPickVideo : Button
    var useExoPlayer : Boolean = false

    // Change player button.
    lateinit var buttonChangePlayer : Button

    // Video player.
    lateinit var player : ExoPlayer
    lateinit var pvPlayerView : PlayerView

    // Image view frame shower.
    lateinit var ivCurrentFrame : ImageView
    var frameBitmapList : MutableList<Bitmap> = mutableListOf<Bitmap>()
    lateinit var frameBitmapArray : Array<Bitmap>

    lateinit var buttonNextFrame : ImageButton
    lateinit var buttonPrevFrame : ImageButton
    lateinit var etFrameStep : EditText
    var frameStep : Int = 0;
    public var currentFrame : Int = 0;


    lateinit var pbDetectionProgress : ProgressBar
    lateinit var glRenderer : GLRenderer
    lateinit var glSurfaceView : GLSurfaceView

    public var sampleIntervalFrames : Int = 30
    var frameDurationMS : Long? = null
    var totalDurationMS : Long? = null
    var totalFrames : Int = -1;
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

        // Exoplayer.
        pvPlayerView = view.findViewById(R.id.pvExemplarPlayerView)
        player = ExoPlayer.Builder(requireContext()).build()
//        player.setSeekParameters(SeekParameters.EXACT)
        pvPlayerView.player = player
        pvPlayerView.useController = false

        // Frame display with image view.
        ivCurrentFrame = view.findViewById(R.id.ivCurrentFrame)

        // Change player button.
        buttonChangePlayer = view.findViewById(R.id.buttonChangePlayer)
        buttonChangePlayer.setOnClickListener {
            if (!glRenderer.flatSkeleton.allFramesAdded)
            {
                Log.i("ChangePlayer", "buttonChangePlayer.setOnClickListener: Not ready yet.")
                return@setOnClickListener
            }

            useExoPlayer = !useExoPlayer
            Log.i("ChangePlayer", "useExoPlayer: $useExoPlayer.")

            if (useExoPlayer)
            {
                pvPlayerView.isEnabled = true
                pvPlayerView.visibility = View.VISIBLE

                ivCurrentFrame.isEnabled = false
                ivCurrentFrame.visibility = View.INVISIBLE

                player.seekTo(currentFrame * frameDurationMS!!)
                buttonChangePlayer.text = "Using Exo"
            }
            else
            {
                pvPlayerView.isEnabled = false
                pvPlayerView.visibility = View.INVISIBLE

                ivCurrentFrame.isEnabled = true
                ivCurrentFrame.visibility = View.VISIBLE

                val bitmapIndex = currentFrame / sampleIntervalFrames
                ivCurrentFrame.setImageBitmap(frameBitmapList[bitmapIndex])
                buttonChangePlayer.text = "Not using Exo"
            }
        }

        // Set default player.
        pvPlayerView.isEnabled = false
        pvPlayerView.visibility = View.INVISIBLE

        ivCurrentFrame.isEnabled = true
        ivCurrentFrame.visibility = View.VISIBLE

        buttonChangePlayer.text = "Not using Exo"

        // Update frame.
        etFrameStep = view.findViewById(R.id.etFrameStep)
        buttonNextFrame = view.findViewById(R.id.buttonNextFrame)
        buttonPrevFrame = view.findViewById(R.id.buttonPrevFrame)

        buttonNextFrame.setOnClickListener {
            updateFrame(1)
        }
        buttonPrevFrame.setOnClickListener {
            updateFrame(-1)
        }

        // Pick video.
        buttonPickVideo = view.findViewById(R.id.buttonExemplarPickVideo)
        buttonPickVideo.setOnClickListener() {
            openVideoPicker()
        }

        // Progress bar.
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

        glSurfaceView.setRenderer(glRenderer)

        @SuppressLint("ClickableViewAccessibility")
        glSurfaceView.setOnTouchListener() { _, event -> glRenderer.onTouchEvent(event) }
    }

    private fun updateFrame(direction : Int)
    {
        if (!glRenderer.flatSkeleton.allFramesAdded)
        {
            Log.e("VideoPlayer", "updateFrame: Not all frames added.")
            return
        }

        val player = pvPlayerView.player ?: run {
            Log.e("VideoPlayer", "updateFrame: pvPlayerView.player is null.")
            return
        }

        frameStep = etFrameStep.text.toString().toIntOrNull() ?: frameStep

        // Calculate and seek to the new position.
        val newFrame = currentFrame + direction * sampleIntervalFrames * frameStep
        currentFrame = if (newFrame > totalFrames)
        {
            0
        }
        else if (newFrame < 0)
        {
            (totalFrames / sampleIntervalFrames) * sampleIntervalFrames
        }
        else
        {
            newFrame
        }

        if (frameDurationMS == null || totalDurationMS == null)
        {
            Log.e("VideoPlayer", "updateFrame: No frame duration or total duration.")
            return
        }

        Log.i(
            "VideoPlayer",
            "updateFrame: \nNew frame:$currentFrame. \nNew landmarks list index: ${currentFrame / sampleIntervalFrames} \nNew position in MS: ${currentFrame * frameDurationMS!!}. \nTotal frames: $totalFrames."
        )
        glRenderer.currentFrame = currentFrame / sampleIntervalFrames

        if (useExoPlayer)
        {
            player.seekTo(currentFrame * frameDurationMS!!)
            player.prepare()
        }
        else
        {
            val bitmapIndex = currentFrame / sampleIntervalFrames
            ivCurrentFrame.setImageBitmap(frameBitmapArray[bitmapIndex])
        }
    }

    private fun initPoseLandmarker()
    {
        try
        {
            val baseOptionBuilder = BaseOptions.builder()
            baseOptionBuilder.setDelegate(Delegate.CPU)
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
            Log.e("GPUDelegation", "GPU delegation error. $e")
        }
    }

    private fun openVideoPicker()
    {
        PictureSelector.create(this).openSystemGallery(SelectMimeType.ofVideo())
            .forSystemResult(object : OnResultCallbackListener<LocalMedia>
            {
                override fun onResult(result : ArrayList<LocalMedia>)
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
            pvPlayerView.visibility = View.INVISIBLE
            ivCurrentFrame.visibility = View.INVISIBLE

            pbDetectionProgress.setProgress(0, true)
            frameBitmapList.clear()
            glRenderer.flatSkeleton.allFramesAdded = false

            val videoPath = result[0].availablePath.toString().toUri()
            Toast.makeText(requireContext(), "Selection made: $videoPath", Toast.LENGTH_LONG)
                .show()

            val mediaItem = MediaItem.fromUri(videoPath)

            player.setMediaItem(mediaItem)
            player.prepare()

            if (useExoPlayer)
            {
                pvPlayerView.visibility = View.VISIBLE
            }

//            player.play()

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
                        glSurfaceView.queueEvent { processResultBundle2D(resultBundle) }
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
        val videoLengthMS =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        val newTotalFrames =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                ?.toInt()
        if (videoLengthMS != null && newTotalFrames != null)
        {
            totalFrames = newTotalFrames
            frameDurationMS = (videoLengthMS.toDouble() / totalFrames.toDouble()).roundToLong()
            totalDurationMS = videoLengthMS
        }
        glRenderer.sampleInterval = sampleIntervalFrames * frameDurationMS!!.toInt()

        val firstFrame = retriever.getFrameAtTime(0)

        if (firstFrame == null)
        {
            Log.e("MPDetectionProgress", "runDetectOnVideo: First frame is null.")
            return null
        }

        videoWidth = firstFrame.width
        videoHeight = firstFrame.height
        Log.i("SelectedVideo", "Width: $videoWidth, height: $videoHeight.")

        activity?.runOnUiThread {
            val videoAspect : Float = videoWidth.toFloat() / videoHeight.toFloat()
            val viewAspect : Float = pvPlayerView.width.toFloat() / pvPlayerView.height.toFloat()

            Log.i("SelectedVideo", "Old view aspect: $viewAspect")

            if (videoAspect > viewAspect)
            {
                // Width too large.
                val newWidth = pvPlayerView.width
                val newHeight = (pvPlayerView.width / videoAspect).roundToInt()
                glSurfaceView.layoutParams = glSurfaceView.layoutParams.apply {
                    width = newWidth
                    height = newHeight
                }
                glSurfaceView.requestLayout()
            }
            else
            {
                // Height too large.
                val newWidth = (pvPlayerView.height * videoAspect).roundToInt()
                val newHeight = pvPlayerView.height
                glSurfaceView.layoutParams = glSurfaceView.layoutParams.apply {
                    width = newWidth
                    height = newHeight
                }
                glSurfaceView.requestLayout()
            }

            glSurfaceView.post {
                val widthAfterLayout = glSurfaceView.width
                val heightAfterLayout = glSurfaceView.height
                val playerWidth = pvPlayerView.width
                val playerHeight = pvPlayerView.height
                Log.i(
                    "SelectedVideo",
                    "New size of glSurfaceView: $widthAfterLayout x $heightAfterLayout. \nSize of entire video player: $playerWidth x $playerHeight."
                )
                Log.i(
                    "SelectedVideo",
                    "Video aspect: $videoAspect. New view aspect: ${glSurfaceView.width.toFloat() / glSurfaceView.height.toFloat()}."
                )
            }
        }

        if (videoLengthMS == null)
        {
            Log.e("MPDetectionProgress", "runDetectOnVideo: Video length is null.")
            return null
        }

        val numberOfFramesToDetect = videoLengthMS / (sampleIntervalFrames * frameDurationMS!!)
        val resultList = mutableListOf<PoseLandmarkerResult>()

        var currentProgressPercent : Int = 0

        for (i in 0..numberOfFramesToDetect)
        {
            val timeStampMs = i * sampleIntervalFrames * frameDurationMS!!
            Log.i("MPDetectionProgress", "Detecting $timeStampMs out of $videoLengthMS.")

            retriever.getFrameAtTime(timeStampMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                ?.let { frame ->

                    val scaledFrame = BitmapHelper.scaleBitmap(frame, 1280, 1280)

                    // Convert to ARGB_8888 if needed
                    val argb8888Frame =
                        if (scaledFrame.config == Bitmap.Config.ARGB_8888) scaledFrame
                        else scaledFrame.copy(Bitmap.Config.ARGB_8888, false)

                    frameBitmapList.add(argb8888Frame)

                    // Convert to MPImage for MediaPipe
                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    Log.i("MPDetectionProgress", "runDetectOnVideo: Calling poseLandmarker.")
                    poseLandmarker?.detectForVideo(mpImage, timeStampMs)?.let { detectionResult ->
                        resultList.add(detectionResult)
                    } ?: run {
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
        retriever.release()

        frameBitmapArray = frameBitmapList.toTypedArray()
        currentFrame = 0
        glRenderer.currentFrame = 0
        activity?.runOnUiThread {
            ivCurrentFrame.setImageBitmap(frameBitmapArray[0])
            ivCurrentFrame.visibility = View.VISIBLE
            pvPlayerView.player?.seekTo(0)
        }

        val endTime = SystemClock.uptimeMillis()
        val timeTaken = "%.1f".format((endTime - startTime) / 1000.0)
        Log.i(
            "MPDetectionProgress",
            "Detection finished at $endTime. Time Taken: $timeTaken seconds."
        )

        activity?.runOnUiThread {
            ivCurrentFrame.setImageBitmap(frameBitmapList[0])
        }

        return ResultBundle(
            resultList,
            sampleIntervalFrames * frameDurationMS!!,
            videoHeight,
            videoWidth
        )
    }

    private fun processResultBundle3D(resultBundle : ResultBundle)
    {
        Log.i("OpenGLThread", "OpenGL running on thread: ${Thread.currentThread().id}")

        val resultListSize = resultBundle.resultsList.size
        glRenderer.newHumanModel(resultListSize)
        glRenderer.humanModel.totalFrames = resultListSize

        Log.i("OpenGL", "processResultBundle: $resultListSize")

        for (i in 0 until resultListSize)
        {
            val landmarksList = resultBundle.resultsList[i]
            Log.i("LandmarksList", landmarksList.toString())

            val jointsVec3Arr = landmarksToVec3Arr(landmarksList)
            glRenderer.humanModel.updateJointsForSingleFrame(i, jointsVec3Arr)
        }

        glRenderer.humanModel.bindVBO()
        glRenderer.humanModel.allFramesAdded = true
    }

    private fun processResultBundle2D(resultBundle : ResultBundle)
    {
        Log.i("OpenGLThread", "OpenGL running on thread: ${Thread.currentThread().id}")
        // Go through every frame in result bundle.
        // For each frame:
        // - updateJoints(frame)
        // - Data of that frame saved to a mutableList
        // - List added to floatArray
        val resultListSize = resultBundle.resultsList.size
        glRenderer.newFlatSkeleton(resultListSize)

        Log.i("OpenGL", "processResultBundle: $resultListSize")

        for (i in 0 until resultListSize)
        {
            val landmarksList = resultBundle.resultsList[i]
            Log.i("LandmarksList", landmarksList.toString())

            val jointsVec3Arr = camSpaceLandmarksToVec3Arr(landmarksList)

            glRenderer.flatSkeleton.updateJointsForSingleFrame(i, jointsVec3Arr)
        }

        glRenderer.flatSkeleton.bindVBO()
        glRenderer.flatSkeleton.allFramesAdded = true
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
                val x = landmark.x() * 2f - 1f
                val y = -landmark.y() * 2f + 1f
                val z = landmark.z()

                if (landmark.visibility().orElse(100f) < 0.5f)
                {
                    jointsVec3Arr[index++] = Vector3(114.514f, 114.514f, 114.514f)
                }
                else
                {
                    jointsVec3Arr[index++] = Vector3(x, y, z)
                }

                Log.i("Landmarks", "Landmark at ${Vector3(x, y, z)}.")
            }
        }

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
}