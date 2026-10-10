#version 120

uniform vec2 u_size;
uniform float u_radius;
uniform vec4 u_color;
uniform float u_shadow;

void main(void)
{
    float adjustedRadius = u_radius * 2.0;
    gl_FragColor = vec4(
        u_color.rgb,
        u_color.a * smoothstep(u_shadow, -u_shadow,
            length(max((abs(gl_TexCoord[0].st - 0.5) + 0.5) * u_size - u_size + adjustedRadius, 0.0)) - adjustedRadius + (u_shadow - 0.5))
    );
}
