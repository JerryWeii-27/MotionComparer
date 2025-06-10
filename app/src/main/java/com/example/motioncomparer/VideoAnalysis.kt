package com.example.motioncomparer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.example.motioncomparer.gles.GLRenderer2D
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class VideoAnalysis(
    val fragmentContext : Context,
    val fragmentActivity : FragmentActivity,

    // Pass UI elements explicitly.
    var ivCurrentFrame : ImageView,
    var etFrameStep : EditText,
    var pbDetectionProgress : ProgressBar,
    var glSurfaceView : GLSurfaceView,
)
{
    // Mediapipe pose landmarker.
    private var poseLandmarker : PoseLandmarker? = null
    val modelName = MainActivity.modelName

    // Image frames.
    var frameBitmapList : MutableList<Bitmap> = mutableListOf()
    lateinit var frameBitmapArray : Array<Bitmap>

    // Pose overlay.
    lateinit var glRenderer2D : GLRenderer2D
    var flatSkeletonIndex : Int = -1

    // Frame navigation.
    var frameStep : Int = 0
    var currentFrame : Int = 0
    var sampleIntervalFrames : Int = MainActivity.sampleIntervalFrames

    // Video metadata.
    var frameDurationMS : Long? = null
    var totalDurationMS : Long? = null
    var totalFrames : Int = -1
    var videoWidth : Int = -1
    var videoHeight : Int = -1

    private lateinit var backgroundExecutor : ScheduledExecutorService

    fun updateFrame(direction : Int)
    {
        if (!glRenderer2D.flatSkeleton.allFramesAdded)
        {
            Log.e("VideoPlayer", "updateFrame: Not all frames added.")
            return
        }

        if (flatSkeletonIndex == -1)
        {
            Log.e("VideoPlayer", "updateFrame: FlatSkeleton index not assigned.")
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

        // Change this to a FlatSkeleton object specific currentFrame.
        glRenderer2D.currentFrame = currentFrame / sampleIntervalFrames
        glRenderer2D.glObjects[flatSkeletonIndex].currentFrame = currentFrame / sampleIntervalFrames


        val bitmapIndex = currentFrame / sampleIntervalFrames
        ivCurrentFrame.setImageBitmap(frameBitmapArray[bitmapIndex])
    }

    fun openVideoPicker()
    {
        PictureSelector.create(fragmentContext).openSystemGallery(SelectMimeType.ofVideo())
            .forSystemResult(object : OnResultCallbackListener<LocalMedia>
            {
                override fun onResult(result : ArrayList<LocalMedia>)
                {
                    handleVideoSelection(result)
                    Toast.makeText(fragmentContext, "Selection made.", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onCancel()
                {
                    Toast.makeText(fragmentContext, "Selection canceled.", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun handleVideoSelection(result : ArrayList<LocalMedia>)
    {
        if (result.isNotEmpty())
        {
            ivCurrentFrame.visibility = View.INVISIBLE

            pbDetectionProgress.setProgress(0, true)
            frameBitmapList.clear()
            glRenderer2D.flatSkeleton.allFramesAdded = false

            val videoPath = result[0].availablePath.toString().toUri()
            Log.i("MPDetectionProgress", "Selection made: $videoPath")

            backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
            backgroundExecutor.execute {
                Log.i("MPDetectionProgress", "Starting background executor.")
                val resultBundle = try
                {
                    runDetectOnVideo(videoPath) ?: run {
                        Log.e("MPDetectionProgress", "Detection returned null results")
                        return@execute
                    }
                }
                catch (e : Exception)
                {
                    Log.e("MPDetectionProgress", "Error during video detection", e)
                    fragmentActivity.runOnUiThread {
                        Log.e("MPDetectionProgress", "")
                    }
                    return@execute
                }

                Log.i("MPDetectionProgress", resultBundle.resultsList[0].landmarks().toString())

                fragmentActivity.runOnUiThread {
                    Log.i("MPDetectionProgress", "Running UI thread code.")
                    glSurfaceView.queueEvent {
                        flatSkeletonIndex = MPHelper.processResultBundle2D(
                            resultBundle,
                            glRenderer2D
                        )
                    }
                }
            }
        }
        else
        {
            Toast.makeText(fragmentContext, "No video selected.", Toast.LENGTH_SHORT).show()
        }
    }

    // Run on background thread.
    private fun runDetectOnVideo(videoUri : Uri) : MPHelper.ResultBundle?
    {
        poseLandmarker = MPHelper.initPoseLandmarker(modelName, fragmentContext)
        val startTime = SystemClock.uptimeMillis()
        Log.i("MPDetectionProgress", "runDetectOnVideo at $startTime")

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(fragmentContext, videoUri)

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
        glRenderer2D.sampleInterval = sampleIntervalFrames * frameDurationMS!!.toInt()

        val firstFrame = retriever.getFrameAtTime(0)

        if (firstFrame == null)
        {
            Log.e("MPDetectionProgress", "runDetectOnVideo: First frame is null.")
            return null
        }

        videoWidth = firstFrame.width
        videoHeight = firstFrame.height
        Log.i("SelectedVideo", "Width: $videoWidth, height: $videoHeight.")

        try
        {
            var noError : Boolean = true
            val latch = CountDownLatch(1)

            fragmentActivity.runOnUiThread {
                val videoAspect : Float = videoWidth.toFloat() / videoHeight.toFloat()
                val viewAspect : Float =
                    ivCurrentFrame.width.toFloat() / ivCurrentFrame.height.toFloat()

                Log.i("SelectedVideo", "Old view aspect: $viewAspect")

                var newWidth : Int = 0
                var newHeight : Int = 0

                if (videoAspect > viewAspect)
                {
                    // Width too large.
                    newWidth = ivCurrentFrame.width
                    newHeight = (ivCurrentFrame.width / videoAspect).roundToInt()
                }
                else
                {
                    // Height too large.
                    newWidth = (ivCurrentFrame.height * videoAspect).roundToInt()
                    newHeight = ivCurrentFrame.height
                }

                if ((MainActivity.glSurfaceViewWidth != 0 && MainActivity.glSurfaceViewWidth != newWidth)
                    || (MainActivity.glSurfaceViewHeight != 0 && MainActivity.glSurfaceViewHeight != newHeight)
                )
                {
                    Log.e("SelectedVideo", "Video has different aspect ratio.")
                    noError = false

                    if (MainActivity.forceSameAspectRatio)
                    {
                        latch.countDown()
                        return@runOnUiThread
                    }
                }

                glSurfaceView.layoutParams = glSurfaceView.layoutParams.apply {
                    width = newWidth
                    height = newHeight
                }

                MainActivity.glSurfaceViewWidth = newWidth
                MainActivity.glSurfaceViewHeight = newHeight

                glSurfaceView.requestLayout()

                glSurfaceView.post {
                    val widthAfterLayout = glSurfaceView.width
                    val heightAfterLayout = glSurfaceView.height
                    val playerWidth = ivCurrentFrame.width
                    val playerHeight = ivCurrentFrame.height

                    Log.i(
                        "SelectedVideo",
                        "New size of glSurfaceView: $widthAfterLayout x $heightAfterLayout. \nSize of entire video player: $playerWidth x $playerHeight."
                    )
                    Log.i(
                        "SelectedVideo",
                        "Video aspect: $videoAspect. New view aspect: ${glSurfaceView.width.toFloat() / glSurfaceView.height.toFloat()}."
                    )
                }
                latch.countDown()
            }

            latch.await() // Wait for the UI thread to finish.
            if (!noError)
            {
                throw RuntimeException("Video has different aspect ratio.")
            }
        }
        catch (e : RuntimeException)
        {
            fragmentActivity.runOnUiThread {
                Toast.makeText(
                    fragmentContext,
                    "Select a video with the same aspect ratio as the other video!",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (MainActivity.forceSameAspectRatio)
            {
                Log.e("SelectedVideo", "runDetectOnVideo: Returning null.")
                return null
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

                    // Convert to ARGB_8888.
                    val argb8888Frame =
                        if (scaledFrame.config == Bitmap.Config.ARGB_8888) scaledFrame
                        else scaledFrame.copy(Bitmap.Config.ARGB_8888, false)

                    frameBitmapList.add(argb8888Frame)

                    // Convert to MPImage for MediaPipe.
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
                fragmentActivity.runOnUiThread {
                    pbDetectionProgress.setProgress(currentProgressPercent, true)
                }
                currentProgressPercent = newProgressPercent
            }
        }
        retriever.release()

        fragmentActivity.runOnUiThread {
            pbDetectionProgress.setProgress(100, true)
        }

        frameBitmapArray = frameBitmapList.toTypedArray()
        frameBitmapList.clear()
        currentFrame = 0
        glRenderer2D.currentFrame = 0
        fragmentActivity.runOnUiThread {
            ivCurrentFrame.setImageBitmap(frameBitmapArray[0])
            ivCurrentFrame.visibility = View.VISIBLE
        }

        val endTime = SystemClock.uptimeMillis()
        val timeTaken = "%.1f".format((endTime - startTime) / 1000.0)
        Log.i(
            "MPDetectionProgress",
            "Detection finished at $endTime. Time Taken: $timeTaken seconds."
        )

        fragmentActivity.runOnUiThread {
            ivCurrentFrame.setImageBitmap(frameBitmapArray[0])
        }

        return MPHelper.ResultBundle(
            resultList,
            sampleIntervalFrames * frameDurationMS!!,
            videoHeight,
            videoWidth
        )
    }
}