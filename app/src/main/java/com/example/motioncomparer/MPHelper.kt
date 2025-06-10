package com.example.motioncomparer

import android.content.Context
import android.util.Log
import com.example.motioncomparer.gles.GLRenderer2D
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class MPHelper
{
    companion object
    {
        fun initPoseLandmarker(modelName : String, fragmentContext : Context) : PoseLandmarker?
        {
            Log.i("MPInit", "initPoseLandmarker: Starting MP init.")
            val poseLandmarker : PoseLandmarker
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
                poseLandmarker = PoseLandmarker.createFromOptions(fragmentContext, options)
                return poseLandmarker
            } catch (e : RuntimeException)
            {
                Log.e("GPUDelegation", "GPU delegation error. $e")
            }
            return null
        }

        fun processResultBundle2D(resultBundle : ResultBundle, glRenderer2D : GLRenderer2D) : Int
        {
            Log.i("OpenGLThread", "OpenGL running on thread: ${Thread.currentThread().id}")
            // Go through every frame in result bundle.
            // For each frame:
            // - updateJoints(frame)
            // - Data of that frame saved to a mutableList
            // - List added to floatArray
            val resultListSize = resultBundle.resultsList.size
            glRenderer2D.newFlatSkeleton(resultListSize)

            Log.i("OpenGL", "processResultBundle: $resultListSize")

            for (i in 0 until resultListSize)
            {
                val landmarksList = resultBundle.resultsList[i]
                Log.i("LandmarksList", landmarksList.toString())

                val jointsVec3Arr = MPHelper.camSpaceLandmarksToVec3Arr(landmarksList)

                glRenderer2D.flatSkeleton.updateJointsForSingleFrame(i, jointsVec3Arr)
            }

            glRenderer2D.flatSkeleton.bindVBO()
            glRenderer2D.flatSkeleton.allFramesAdded = true

            // Returns the index of the flatSkeleton in glObject list.
            return glRenderer2D.glObjects.size - 1
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
    }

    data class ResultBundle(
        val resultsList : List<PoseLandmarkerResult>,
        val inferenceTime : Long,
        val inputImageHeight : Int,
        val inputImageWidth : Int,
    )
}

