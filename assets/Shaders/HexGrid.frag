#extension GL_EXT_texture_array : enable

varying vec3 worldPosition;
varying vec3 terrain;

uniform sampler2DArray m_ColorMap;

varying vec4 vertColor;

vec4 getTerrainColor (int index) {
    vec3 sample = vec3(worldPosition.xz * 0.05, terrain[index]);
    vec4 c = texture2DArray(m_ColorMap, sample);
    return c * vertColor[index];
}

void main() {
    vec4 color = vec4(1.0);
    vec4 c = getTerrainColor(0) + getTerrainColor(1) + getTerrainColor(2);

    color *= c;

    // color *= texture2DArray(m_ColorMap, texCoord1);

    // color *= vertColor;

    gl_FragColor = color;
}