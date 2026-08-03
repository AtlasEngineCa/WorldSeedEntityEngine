#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
#ifdef DISSOLVE
uniform sampler2D DissolveMaskSampler;
#endif

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif
#ifndef EMISSIVE
in vec4 lightMapColor;
#endif
#ifndef NO_OVERLAY
in vec4 overlayColor;
#endif
in vec2 texCoord0;
in vec2 texCoord1;
flat in int playerPart;
flat in int playerSlim;
out vec4 fragColor;

const vec4[] playerSubUvs = vec4[](
    vec4(4,0,8,4), vec4(8,0,12,4), vec4(0,4,4,16), vec4(4,4,8,16), vec4(8,4,12,16), vec4(12,4,16,16),
    vec4(4,0,7,4), vec4(7,0,10,4), vec4(0,4,4,16), vec4(4,4,7,16), vec4(7,4,11,16), vec4(11,4,14,16),
    vec4(4,0,12,4), vec4(12,0,20,4), vec4(0,4,4,16), vec4(4,4,12,16), vec4(12,4,16,16), vec4(16,4,24,16)
);
const vec2[] playerOrigins = vec2[](
    vec2(40,16), vec2(40,32), vec2(32,48), vec2(48,48), vec2(16,16),
    vec2(16,32), vec2(0,16), vec2(0,32), vec2(16,48), vec2(0,48)
);

vec2 remapPlayerUv(vec2 sourceUv) {
    vec2 sourcePixel = sourceUv * 64.0;
    int outerLayer = sourcePixel.x >= 32.0 ? 1 : 0;
    sourcePixel.x -= float(outerLayer) * 32.0;
    int faceId;
    vec2 sourceMin;
    if (sourcePixel.y < 8.0) {
        faceId = sourcePixel.x < 16.0 ? 0 : 1;
        sourceMin = vec2(faceId == 0 ? 8.0 : 16.0, 0.0);
    } else {
        faceId = 2 + int(clamp(floor(sourcePixel.x / 8.0), 0.0, 3.0));
        sourceMin = vec2(float(faceId - 2) * 8.0, 8.0);
    }
    int shapeOffset = (playerSlim != 0 && playerPart <= 2) ? 6 : (playerPart == 3 ? 12 : 0);
    vec4 target = playerSubUvs[shapeOffset + faceId];
    vec2 targetOffset = mix(target.xy, target.zw, (sourcePixel - sourceMin) / 8.0);
    vec2 targetOrigin = playerOrigins[2 * (playerPart - 1) + outerLayer];
    return (targetOrigin + targetOffset) / 64.0;
}

void main() {
    vec2 sampleUv = playerPart > 0 ? remapPlayerUv(texCoord0) : texCoord0;
    vec4 color = texture(Sampler0, sampleUv);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) discard;
#endif
#ifdef PER_FACE_LIGHTING
    vec4 faceVertexColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
#else
    vec4 faceVertexColor = vertexColor;
#endif
#ifdef DISSOLVE
    if (faceVertexColor.a < texture(DissolveMaskSampler, texCoord0).a) discard;
    faceVertexColor.a = 1.0;
#endif
    color *= faceVertexColor * ColorModulator;
#ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
#endif
#ifndef EMISSIVE
    color *= lightMapColor;
#endif
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
