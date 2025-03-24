#version 310 es
precision highp float;
precision highp int;

layout (local_size_x = 16, local_size_y = 16) in;

// Uniform for the 2D integer texture holding your grid data.
uniform highp isampler2D gridDataTexture;

// Write-only output image.
layout (rgba8, binding = 1) uniform highp writeonly image2D outputImage;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
    ivec2 imgSize = imageSize(outputImage);

    // Compute the 1D index corresponding to this output pixel.
    int index = pos.y * imgSize.x + pos.x;

    // Determine which integer holds the desired bit and which bit inside that integer.
    int texelIndex = index / 32;
    int bitIndex = index % 32;

    // Get the dimensions of the gridDataTexture.
    ivec2 gridTexSize = textureSize(gridDataTexture, 0);
    // Convert the 1D index (texelIndex) into a 2D coordinate.
    ivec2 texelPos = ivec2(texelIndex % gridTexSize.x, texelIndex / gridTexSize.x);

    // Fetch the integer value from the texture using texelFetch.
    int value = texelFetch(gridDataTexture, texelPos, 0).r;

    // Check if the desired bit is set.
    bool isSet = (value & (1 << bitIndex)) != 0;

    // Write the result: white if the bit is set, black otherwise.
    vec4 color = isSet ? vec4(1.0) : vec4(0.0);
    imageStore(outputImage, pos, color);
}
