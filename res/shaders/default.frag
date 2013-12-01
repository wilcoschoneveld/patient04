#version 120

uniform mat4 matModel;
uniform mat4 matNormal;
uniform vec3 colorDiffuse;

uniform sampler2D texLocation0;
uniform int useTexture;

varying vec3 vPosition;
varying vec2 vTexCoord;
varying vec3 vNormal;

void main() {
    vec3 lightPosition = vec3(4, 8, 4);
    vec3 fragPosition = (matModel * vec4(vPosition, 1)).xyz;

    float dist = length(lightPosition - fragPosition);

    vec3 fragNormal = normalize((matNormal * vec4(vNormal,1)).xyz);
    vec3 lightDir = normalize(lightPosition - fragPosition);

    float diffuseI = 0.05 + 500 * max(0, dot(fragNormal, lightDir)) / pow(dist, 3);

    // Set initial color to diffuse color
    vec4 outColor = vec4(colorDiffuse, 1) * diffuseI;

    // Set diffuse texture
    if(useTexture == 1) {
        outColor *= texture2D(texLocation0, vTexCoord);
    }

    // Set fragment color
    gl_FragData[0] = outColor;
}