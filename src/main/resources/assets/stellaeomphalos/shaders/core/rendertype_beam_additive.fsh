#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in vec4 vertexColor;
in vec2 texCoord;
in float fogDistance;
out vec4 fragColor;
void main() {
    vec4 color = texture(Sampler0, texCoord) * vertexColor * ColorModulator;
    if (color.a < 0.003) discard;
    float fog = smoothstep(FogStart, max(FogStart + 0.001, FogEnd), fogDistance);
    fragColor = vec4(mix(color.rgb, FogColor.rgb, fog), color.a * (1.0 - fog));
}
