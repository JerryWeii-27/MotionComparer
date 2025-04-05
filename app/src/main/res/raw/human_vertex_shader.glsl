uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec4 vNormal;
varying vec3 fNormal;

void main() {
    gl_Position = uMVPMatrix * vPosition;
    fNormal = vec3(vNormal.x, vNormal.y, vNormal.z);
}