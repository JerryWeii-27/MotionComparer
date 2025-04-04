layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

layout (std430, binding = 0) buffer landmarkPositionsBuffer {
    vec3 landmarkPositionsArr[];
};

layout (std430, binding = 1) buffer verticesBuffer {
    float landmarkPositionsArr[];
};

layout (std430, binding = 2) buffer testBuffer {
    float testArr[];
};

in uvec3 gl_NumWorkGroups;        // Check how many work groups there are. Provided for convenience.
in uvec3 gl_WorkGroupID;          // Check which work group the thread belongs to.
in uvec3 gl_LocalInvocationID;    // Within the work group, get a unique identifier for the thread.
in uvec3 gl_GlobalInvocationID;   // Globally unique value across the entire compute dispatch. Short-hand for gl_WorkGroupID * gl_WorkGroupSize + gl_LocalInvocationID;
in uint gl_LocalInvocationIndex; // 1D version of gl_LocalInvocationID. Provided for convenience.

void main()
{
    int currentLandmark = int(gl_localinvocationindex);
    testArr[currentLandmark] += currentLandmark;
}

