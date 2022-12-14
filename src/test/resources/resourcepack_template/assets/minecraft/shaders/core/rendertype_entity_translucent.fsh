#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 normal;

in vec3 a, b;

out vec4 fragColor;

vec2 getSize() {
    vec2 uv1 = a.xy / a.z;
    vec2 uv2 = b.xy / b.z;
    return round((max(uv1, uv2) - min(uv1, uv2)) * 64);
}

void main() {

    if(texCoord0.x < 0) {
        discard;
    }

    vec2 uv = texCoord0;
    if(getSize() != vec2(8, 8))
        uv = texCoord1;

    vec4 color = texture(Sampler0, uv);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
