uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
//attribute vec4 vColor;
//varying vec4 fColor;

void main() {
    gl_Position = uMVPMatrix * vPosition;
//    fColor = vColor;
}