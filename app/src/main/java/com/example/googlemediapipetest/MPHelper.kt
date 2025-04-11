package com.example.googlemediapipetest

import android.util.Log
import com.example.googlemediapipetest.gles.GLRenderer
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class MPHelper
{
    companion object
    {
        fun processResultBundle2D(resultBundle : ResultBundle, glRenderer : GLRenderer)
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

                val jointsVec3Arr = MPHelper.camSpaceLandmarksToVec3Arr(landmarksList)

                glRenderer.flatSkeleton.updateJointsForSingleFrame(i, jointsVec3Arr)
            }

            glRenderer.flatSkeleton.bindVBO()
            glRenderer.flatSkeleton.allFramesAdded = true
        }

        fun processResultBundle3D(resultBundle : ResultBundle, glRenderer : GLRenderer)
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
}

data class ResultBundle(
    val resultsList : List<PoseLandmarkerResult>,
    val inferenceTime : Long,
    val inputImageHeight : Int,
    val inputImageWidth : Int,
)