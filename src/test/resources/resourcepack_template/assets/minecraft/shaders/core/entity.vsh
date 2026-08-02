#version 330

#if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)
#moj_import <minecraft:light.glsl>
#endif
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler0;
#ifndef NO_OVERLAY
uniform sampler2D Sampler1;
#endif
#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
out vec4 vertexPerFaceColorBack;
out vec4 vertexPerFaceColorFront;
#else
out vec4 vertexColor;
#endif
#ifndef EMISSIVE
out vec4 lightMapColor;
#endif
#ifndef NO_OVERLAY
out vec4 overlayColor;
#endif
out vec2 texCoord0;
out vec2 texCoord1;
flat out int playerPart;

#define SPACING 1024.0
#define MAXRANGE (0.5 * SPACING)

const vec4[] subuvs = vec4[](
    vec4(4,0,8,4), vec4(8,0,12,4), vec4(0,4,4,16), vec4(4,4,8,16), vec4(8,4,12,16), vec4(12,4,16,16),
    vec4(4,0,7,4), vec4(7,0,10,4), vec4(0,4,4,16), vec4(4,4,7,16), vec4(7,4,11,16), vec4(11,4,14,16),
    vec4(4,0,12,4), vec4(12,0,20,4), vec4(0,4,4,16), vec4(4,4,12,16), vec4(12,4,16,16), vec4(16,4,24,16)
);
const vec2[] origins = vec2[](
    vec2(40,16), vec2(40,32), vec2(32,48), vec2(48,48), vec2(16,16),
    vec2(16,32), vec2(0,16), vec2(0,32), vec2(16,48), vec2(0,48)
);

void main() {
    vec3 position = Position;
    texCoord0 = UV0;
    texCoord1 = vec2(0);
    playerPart = 0;

    ivec2 dim = textureSize(Sampler0, 0);
    if (ProjMat[2][3] != 0.0 && dim == ivec2(64)) {
        int partId = -int((Position.y - MAXRANGE) / SPACING);
        playerPart = partId;
        if (partId > 0 && partId <= 5) {
            vec4 samp1 = texture(Sampler0, vec2(54.0/64.0, 20.0/64.0));
            vec4 samp2 = texture(Sampler0, vec2(55.0/64.0, 20.0/64.0));
            bool slim = samp1.a == 0.0 || (all(equal(samp1.rgb, vec3(0))) && all(equal(samp2.rgb, vec3(0))) && samp1.a == 1.0 && samp2.a == 1.0);
            int outerLayer = (gl_VertexID / 24) % 2;
            int faceId = (gl_VertexID % 24) / 4;
            int vertexId = gl_VertexID % 4;
            int subuvIndex = faceId + ((slim && partId <= 2) ? 6 : (partId == 3 ? 12 : 0));
            position.y += SPACING * partId;
            vec2 uv = origins[2 * (partId - 1) + outerLayer];
            vec2 uv2 = origins[2 * (partId - 1)];
            vec4 s = subuvs[subuvIndex];
            vec2 offset;
            if (faceId == 1) {
                offset = vertexId == 0 ? s.zw : vertexId == 1 ? s.xw : vertexId == 2 ? s.xy : s.zy;
            } else {
                offset = vertexId == 0 ? s.zy : vertexId == 1 ? s.xy : vertexId == 2 ? s.xw : s.zw;
            }
            texCoord0 = (uv + offset) / 64.0;
            texCoord1 = (uv2 + offset) / 64.0;
        }
    }

    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
    sphericalVertexDistance = fog_spherical_distance(position);
    cylindricalVertexDistance = fog_cylindrical_distance(position);
#ifdef PER_FACE_LIGHTING
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, Normal);
    vertexPerFaceColorBack = minecraft_mix_light_separate(-light, Color);
    vertexPerFaceColorFront = minecraft_mix_light_separate(light, Color);
#elif defined(NO_CARDINAL_LIGHTING)
    vertexColor = Color;
#else
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
#endif
#ifndef EMISSIVE
    lightMapColor = sample_lightmap(Sampler2, UV2);
#endif
#ifndef NO_OVERLAY
    overlayColor = texelFetch(Sampler1, UV1, 0);
#endif
#ifdef APPLY_TEXTURE_MATRIX
    texCoord0 = (TextureMat * vec4(texCoord0, 0.0, 1.0)).xy;
#endif
}
