package pitheguy.countycolor.render.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.IntBuffer;

public class ComputeShaderWrapper {
    private final int programId;

    public ComputeShaderWrapper(String computeShaderSource) {
        int computeShader = Gdx.gl31.glCreateShader(GL31.GL_COMPUTE_SHADER);
        Gdx.gl31.glShaderSource(computeShader, computeShaderSource);
        Gdx.gl31.glCompileShader(computeShader);
        int[] compileStatus = new int[1];
        IntBuffer compileStatusBuffer = BufferUtils.newIntBuffer(1).put(compileStatus).flip();
        Gdx.gl31.glGetShaderiv(computeShader, GL31.GL_COMPILE_STATUS, compileStatusBuffer);
        if (compileStatus[0] == 0) {
            String log = Gdx.gl31.glGetShaderInfoLog(computeShader);
            throw new GdxRuntimeException("Compute shader compilation failed: " + log);
        }
        programId = Gdx.gl31.glCreateProgram();
        Gdx.gl31.glAttachShader(programId, computeShader);
        Gdx.gl31.glLinkProgram(programId);
        int[] linkStatus = new int[1];
        IntBuffer linkStatusBuffer = BufferUtils.newIntBuffer(1).put(linkStatus).flip();
        Gdx.gl31.glGetProgramiv(programId, GL31.GL_LINK_STATUS, linkStatusBuffer);
        if (linkStatus[0] == 0) {
            String log = Gdx.gl31.glGetProgramInfoLog(programId);
            throw new GdxRuntimeException("Compute shader program linking failed: " + log);
        }
        Gdx.gl31.glDetachShader(programId, computeShader);
        Gdx.gl31.glDeleteShader(computeShader);
    }

    public int getProgramId() {
        return programId;
    }

    public void bind() {
        Gdx.gl31.glUseProgram(programId);
    }

    public void unbind() {
        Gdx.gl31.glUseProgram(0);
    }

    public void dispose() {
        Gdx.gl31.glDeleteProgram(programId);
    }

    public void setUniformi(String uniformName, int value) {
        int location = Gdx.gl31.glGetUniformLocation(programId, uniformName);
        if (location == -1) throw new GdxRuntimeException("Uniform '" + uniformName + "' not found.");
        Gdx.gl31.glUniform1i(location, value);
    }
}

