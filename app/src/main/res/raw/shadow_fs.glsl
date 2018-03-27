precision mediump float;
varying vec4 vColor;
uniform int cube;
void main() {
//    gl_FragColor = vec4(gl_FragCoord.z);
    if (cube == 1)
        gl_FragColor = vColor;
    else gl_FragColor = vec4(1.0);
}