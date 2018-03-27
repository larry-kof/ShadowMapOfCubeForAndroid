precision mediump float;
attribute vec4 aPosition;
uniform mat4 depthMVP;
attribute vec4 aColor;
varying vec4 vColor;
void main() {
    gl_Position = depthMVP * aPosition;
    vColor = aColor;
}