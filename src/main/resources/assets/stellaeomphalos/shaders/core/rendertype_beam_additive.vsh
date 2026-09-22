#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 TextureMat;
out vec4 vertexColor;
out vec2 texCoord;
out float fogDistance;
void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    vertexColor = Color;
    texCoord = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    fogDistance = length(view.xyz);
}
