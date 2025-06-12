precision mediump float;
varying vec3 fColor;
uniform float uAlpha;  // Add this uniform for controlling opacity.
uniform int useSecondColor;

void main() {
    if(useSecondColor == 1)
    {
        gl_FragColor = vec4(fColor.y, fColor.z, fColor.x, uAlpha);
    }
    else
    {
        gl_FragColor = vec4(fColor, uAlpha);
    }
}