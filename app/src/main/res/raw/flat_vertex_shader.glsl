attribute vec4 vPosition;
attribute vec4 vColor;
varying vec3 fColor;

void main() {
    gl_Position = vPosition;
    fColor = vec3(vColor.x, vColor.y, vColor.z);
}