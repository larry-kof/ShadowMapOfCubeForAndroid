precision mediump float;
varying vec4 vColor;
varying vec4 vShadowCoord;
uniform sampler2D uShadow;
void main() {
//      gl_FragColor = vec4(0.2,0.2,0.2,1.0);
//    float visibility = 1.0;
    vec4 shadowMapPosition = vShadowCoord / vShadowCoord.w;
    if (texture2D(uShadow,shadowMapPosition.xy).r < shadowMapPosition.z)
        gl_FragColor = vec4(0.2,0.2,0.2,1.0);
    else  gl_FragColor = vec4(0.8,0.8,0.8,1.0);
//    float r = texture2DProj(uShadow,vShadowCoord).r;
//    gl_FragColor = vec4(r);

//    gl_FragColor = vColor;
}