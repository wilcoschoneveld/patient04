#version 120

uniform mat4 matProj;
uniform mat4 matView;
uniform mat4 matModel;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec3 inTexCoord;

void main() {
    gl_Position = matProj * matView * matModel * vec4(inPosition, 1);
}