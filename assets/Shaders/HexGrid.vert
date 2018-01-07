uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
varying vec3 worldPosition;

attribute vec3 inTexCoord2;
varying vec3 terrain;

// attribute vec3 inTexCoord;
// varying vec3 texCoord1;

attribute vec4 inColor;
varying vec4 vertColor;

void main(){
    // texCoord1 = inTexCoord;

    vertColor = inColor;

    terrain = inTexCoord2;
    worldPosition = inPosition;

    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}

