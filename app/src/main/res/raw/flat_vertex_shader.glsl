attribute vec4 vPosition;
uniform float zPosition;
attribute vec4 vColor;
varying vec3 fColor;

void main() {
    gl_Position = vPosition;
    gl_Position.z = zPosition;
    fColor = vColor.rgb;
}