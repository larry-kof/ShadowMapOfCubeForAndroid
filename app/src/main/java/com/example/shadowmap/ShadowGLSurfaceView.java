package com.example.shadowmap;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by apple on 2018/3/7.
 */

public class ShadowGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private int shadow_program;
    private int shadow_fbo;
    private int shadow_texture;

    private int shadow_pos;
    private int shadow_col;
    private int shadow_mvp;
    private int shadow_cube;

    private FloatBuffer vertexBuffer, colorBuffer;
    private ShortBuffer indexBuffer;
    FloatBuffer floorbuffer;

    private int usual_program;
    private int usual_pos;
    private int usual_mvp;
    private int usual_bias;
    private int usual_tex;
    private int usual_col;

    public ShadowGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    private boolean genShadowResource(int width, int height) {

        IntBuffer i = IntBuffer.allocate(1);
        GLES20.glGenFramebuffers(1, i);
        i.position(0);
        shadow_fbo = i.get();

        IntBuffer j = IntBuffer.allocate(1);
        GLES20.glGenTextures(1, j);
        shadow_texture = j.get();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadow_fbo);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadow_texture);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_DEPTH_COMPONENT, width, height, 0,
                GLES20.GL_DEPTH_COMPONENT, GLES20.GL_UNSIGNED_INT, null);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_TEXTURE_2D, shadow_texture, 0);

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("GLES2.0", "framebuffer error");
            return false;
        }

        return true;
    }

    private int loadShader(int shaderType, String source) {

        //创建一个新shader
        int shader = GLES20.glCreateShader(shaderType);

        //若创建成功则加载shader
        if (shader != 0) {

            //加载shader的源代码
            GLES20.glShaderSource(shader, source);

            //编译shader
            GLES20.glCompileShader(shader);

            //存放编译成功shader数量的数组
            int[] compiled = new int[1];

            //获取Shader的编译情况
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

            if (compiled[0] == 0) {
                //若编译失败则显示错误日志并删除此shader
                Log.e("GLES2.0", "Could not compile shader " + shaderType + ":");
                Log.e("GLES2.0", GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {

        //加载顶点着色器

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);

        if (vertexShader == 0) {

            return 0;

        }

        //加载片元着色器

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        if (pixelShader == 0) {

            return 0;

        }

        //创建着色器程序

        int program = GLES20.glCreateProgram();

        //若程序创建成功则向程序中加入顶点着色器与片元着色器

        if (program != 0) {

            //向程序中加入顶点着色器

            GLES20.glAttachShader(program, vertexShader);

            //向程序中加入片元着色器

            GLES20.glAttachShader(program, pixelShader);

            //链接程序

            GLES20.glLinkProgram(program);

            //存放链接成功program数量的数组

            int[] linkStatus = new int[1];

            //获取program的链接情况

            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

            //若链接失败则报错并删除程序

            if (linkStatus[0] != GLES20.GL_TRUE) {

                Log.e("GLES2.0", "Could not link program: ");

                Log.e("GLES2.0", GLES20.glGetProgramInfoLog(program));

                GLES20.glDeleteProgram(program);

                program = 0;

            }

        }

        GLES20.glDeleteShader(pixelShader);
        GLES20.glDeleteShader(vertexShader);

        return program;

    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        shadow_x = 0.0f;
        Log.e("GLES", "onSurfaceCreate");
        InputStream inputStream_vs = getContext().getResources().openRawResource(R.raw.shadow_vs);
        InputStream inputStream_fs = getContext().getResources().openRawResource(R.raw.shadow_fs);

        InputStream inputStream_uvs = getContext().getResources().openRawResource(R.raw.usual_vs);
        InputStream inputStream_ufs = getContext().getResources().openRawResource(R.raw.usual_fs);
        try {
            byte[] vs_buffer = new byte[inputStream_vs.available()];
            byte[] fs_buffer = new byte[inputStream_fs.available()];

            byte[] uvs_buffer = new byte[inputStream_uvs.available()];
            byte[] ufs_buffer = new byte[inputStream_ufs.available()];

            inputStream_vs.read(vs_buffer, 0, vs_buffer.length);
            inputStream_fs.read(fs_buffer, 0, fs_buffer.length);

            inputStream_uvs.read(uvs_buffer, 0, uvs_buffer.length);
            inputStream_ufs.read(ufs_buffer, 0, ufs_buffer.length);

            String vertex_glsl = new String(vs_buffer);
            String fragment_glsl = new String(fs_buffer);

            String uv_glsl = new String(uvs_buffer);
            String uf_glsl = new String(ufs_buffer);

            shadow_program = createProgram(vertex_glsl, fragment_glsl);

            GLES20.glLinkProgram(shadow_program);

            shadow_pos = GLES20.glGetAttribLocation(shadow_program, "aPosition");
            shadow_col = GLES20.glGetAttribLocation(shadow_program, "aColor");

            shadow_mvp = GLES20.glGetUniformLocation(shadow_program, "depthMVP");
            shadow_cube = GLES20.glGetUniformLocation(shadow_program,"cube");

            usual_program = createProgram(uv_glsl, uf_glsl);
            GLES20.glLinkProgram(usual_program);
            usual_pos = GLES20.glGetAttribLocation(usual_program, "aPosition");
            usual_mvp = GLES20.glGetUniformLocation(usual_program, "MVP");
            usual_bias = GLES20.glGetUniformLocation(usual_program, "bias");
            usual_tex = GLES20.glGetUniformLocation(usual_program, "uShadow");
            usual_col = GLES20.glGetAttribLocation(usual_program, "aColor");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream_vs.close();
                inputStream_fs.close();

                inputStream_ufs.close();
                inputStream_uvs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    float[] shadow_projection = new float[16];
    float[] shadow_view = new float[16];
    float[] shadow_result = new float[16];
    float[] usual_projection = new float[16];
    float[] usual_view = new float[16];
    float[] usual_depth_mvp = new float[16];
    float[] usual_result = new float[16];

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.e("GLES", "onSurfaceChanged");
        genShadowResource(width, height);

        GLES20.glViewport(0, 0, width, height);


        float cube[] =
                {
                        -0.5f, 0.5f, 0.5f,    //正面左上0
                        -0.5f, -0.5f, 0.5f,   //正面左下1
                        0.5f, -0.5f, 0.5f,    //正面右下2
                        0.5f, 0.5f, 0.5f,     //正面右上3
                        -0.5f, 0.5f, -0.5f,    //反面左上4
                        -0.5f, -0.5f, -0.5f,   //反面左下5
                        0.5f, -0.5f, -0.5f,    //反面右下6
                        0.5f, 0.5f, -0.5f,     //反面右上7
                };

        ByteBuffer bb = ByteBuffer.allocateDirect(cube.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cube);
        vertexBuffer.position(0);

        short cube_index[] =
                {
                        0, 3, 2, 0, 2, 1,    //正面
                        0, 1, 5, 0, 5, 4,    //左面
                        0, 7, 3, 0, 4, 7,    //上面
                        6, 7, 4, 6, 4, 5,    //后面
                        6, 3, 7, 6, 2, 3,    //右面
                        6, 5, 1, 6, 1, 2     //下面
                };
        ByteBuffer bb2 = ByteBuffer.allocateDirect(cube_index.length * 2);
        bb2.order(ByteOrder.nativeOrder());
        indexBuffer = bb2.asShortBuffer();
        indexBuffer.put(cube_index);
        indexBuffer.position(0);

        float color[] = {
                0f, 1f, 0f, 1f,
                0f, 1f, 0f, 1f,
                0f, 1f, 0f, 1f,
                0f, 1f, 0f, 1f,
                1f, 0f, 0f, 1f,
                1f, 0f, 0f, 1f,
                1f, 0f, 0f, 1f,
                1f, 0f, 0f, 1f,
        };

        ByteBuffer bb3 = ByteBuffer.allocateDirect(color.length * 4);
        bb3.order(ByteOrder.nativeOrder());
        colorBuffer = bb3.asFloatBuffer();
        colorBuffer.put(color);
        colorBuffer.position(0);

        float floor_vertex[] = {
                -5.0f, -0.5f, 5.0f,
                -5.0f, -0.5f, -5.0f,
                5.0f, -0.5f, 5.0f,
                5.0f, -0.5f, -5.0f
        };

        ByteBuffer bb4 = ByteBuffer.allocateDirect(floor_vertex.length * 4);
        bb4.order(ByteOrder.nativeOrder());
        floorbuffer = bb4.asFloatBuffer();
        floorbuffer.put(floor_vertex);
        floorbuffer.position(0);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        int fbo[] = new int[1];
        fbo[0] = shadow_fbo;
        GLES20.glDeleteFramebuffers(GLES20.GL_FRAMEBUFFER, fbo, 1);

        int texture[] = new int[1];
        texture[0] = shadow_texture;
        GLES20.glDeleteTextures(GLES20.GL_TEXTURE_2D, texture, 1);

        GLES20.glDeleteProgram(shadow_program);
        GLES20.glDeleteProgram(usual_program);
    }

    private float x;
    private float shadow_x;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e("GLES ", "" + event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            x = event.getX();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float tempx = event.getX();
            shadow_x = shadow_x + (tempx - x) * 0.001f;
            this.requestRender();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadow_fbo);

        GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glClearDepthf(1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(shadow_program);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        Matrix.perspectiveM(shadow_projection, 0, 45.0f, (float) getWidth() / (float) getHeight(), 1.0f, 1000.f);
        Matrix.setLookAtM(shadow_view, 0, shadow_x, 4.0f, 4.0f, 0, 0, 0, 0, 1, 0);
        Matrix.multiplyMM(shadow_result, 0, shadow_projection, 0, shadow_view, 0);

        GLES20.glEnableVertexAttribArray(shadow_pos);
        GLES20.glVertexAttribPointer(shadow_pos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glUniformMatrix4fv(shadow_mvp, 1, false, shadow_result, 0);

        GLES20.glUniform1i(shadow_cube,1);
        GLES20.glEnableVertexAttribArray(shadow_col);
        GLES20.glVertexAttribPointer(shadow_col, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(shadow_pos);
        GLES20.glDisableVertexAttribArray(shadow_col);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClearDepthf(1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glCullFace(GLES20.GL_FRONT);

        GLES20.glUseProgram(usual_program);
        Matrix.perspectiveM(usual_projection, 0, 45.0f, (float) getWidth() / (float) getHeight(), 0.01f, 30.0f);

        Matrix.setLookAtM(usual_view, 0, 6.0f, 10.0f, 10.0f, 0, 0, 0, 0, 1, 0);

        Matrix.multiplyMM(usual_result, 0, usual_projection, 0, usual_view, 0);

        GLES20.glUniformMatrix4fv(usual_mvp, 1, false, usual_result, 0);

        float[] bias = {
                0.5f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.5f, 0.0f,
                0.5f, 0.5f, 0.5f, 1.0f
        };

        Matrix.multiplyMM(usual_depth_mvp, 0, bias, 0, shadow_result, 0);

        GLES20.glUniformMatrix4fv(usual_bias, 1, false, usual_depth_mvp, 0);

        GLES20.glEnableVertexAttribArray(usual_pos);
        GLES20.glVertexAttribPointer(usual_pos, 3, GLES20.GL_FLOAT, false, 0, floorbuffer);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadow_texture);
        GLES20.glUniform1i(usual_tex, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(usual_pos);

        GLES20.glUseProgram(shadow_program);
        GLES20.glUniform1i(shadow_cube,1);
        GLES20.glEnableVertexAttribArray(shadow_pos);
        GLES20.glVertexAttribPointer(shadow_pos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glUniformMatrix4fv(shadow_mvp, 1, false, usual_result, 0);
        GLES20.glEnableVertexAttribArray(shadow_col);
        GLES20.glVertexAttribPointer(shadow_col, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(shadow_pos);
        GLES20.glDisableVertexAttribArray(shadow_col);

        GLES20.glUseProgram(shadow_program);
        GLES20.glUniform1i(shadow_cube,0);
        GLES20.glEnableVertexAttribArray(shadow_pos);
        float light_pos[] = {
            shadow_x,4.0f,4.0f,
                shadow_x+1.0f,4.0f,3.0f,
                shadow_x+1.0f,4.0f,5.0f
        };

        ByteBuffer bb4 = ByteBuffer.allocateDirect(light_pos.length*4);
        bb4.order(ByteOrder.nativeOrder());
        FloatBuffer lightbuffer = bb4.asFloatBuffer();
        lightbuffer.put(light_pos);
        lightbuffer.position(0);

        GLES20.glVertexAttribPointer(shadow_pos, 3, GLES20.GL_FLOAT, false, 0, lightbuffer);
        GLES20.glUniformMatrix4fv(shadow_mvp, 1, false, usual_result, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,3);

        GLES20.glFinish();
    }
}
