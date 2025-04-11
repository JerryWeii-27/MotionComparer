package com.example.googlemediapipetest

import android.util.Log
import com.example.googlemediapipetest.gles.GLRenderer
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class MPHelper
{
    companion object
    {
        private fun processResultBundle3D(resultBundle : ResultBundle, glRenderer : GLRenderer)
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