#version 150

uniform vec4 ColorModulator;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    // Original untextured smooth-color fans disable alpha testing, including transparent tips.
    fragColor = vertexColor * ColorModulator;
}
