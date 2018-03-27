precision mediump float;
attribute vec4 aPosition;
uniform mat4 MVP;
attribute vec4 aColor;
varying vec4 vColor;
varying vec4 vShadowCoord;
uniform mat4 bias;
uniform mat4 lightbias;
varying vec4 vlightCoord;
void main() {
    vec4 pos = aPosition;
    gl_Position = MVP * pos;
    vShadowCoord = bias * pos;
    vColor = aColor;
}