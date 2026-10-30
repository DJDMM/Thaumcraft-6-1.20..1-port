#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4 InverseProjection;
uniform float Density;
uniform vec3 FogColor;
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec4 source=texture(DiffuseSampler,texCoord);
    float depth=texture(DepthSampler,texCoord).r;
    vec4 point=InverseProjection*vec4(texCoord*2.0-1.0,depth*2.0-1.0,1.0);
    float distance=length(point.xyz/point.w);
    float transmission=clamp(exp(-Density*distance),0.0,1.0);
    fragColor=vec4(mix(FogColor,source.rgb,transmission),source.a);
}
