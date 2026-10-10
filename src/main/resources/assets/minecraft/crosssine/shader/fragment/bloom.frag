#version 120

uniform sampler2D u_diffuse_sampler, u_other_sampler;
uniform vec2 u_texel_size, u_direction;
uniform float u_radius, u_kernel[24];

void main(void)
{
    vec2 uv = gl_TexCoord[0].st;

    if (u_direction.x == 0.0) {

        if (texture2D(u_other_sampler, uv).a > 0.0)
        {
            discard;
        }
    }

    vec4 kernel = texture2D(u_diffuse_sampler, uv);
    vec4 pixel_color = kernel * u_kernel[0];

    pixel_color.rgb *= pixel_color.a;

    for (int f = 0; f <= u_radius; f++)
    {
        // calculate offset
        vec2 offset = (u_texel_size * u_direction) * f;

        // create vertical and horizontal samples
        vec4 left = texture2D(u_diffuse_sampler, uv - offset);
        vec4 right = texture2D(u_diffuse_sampler, uv + offset);

        // multiply by alpha to remove dark edges
        left.rgb *= left.a;
        right.rgb *= right.a;

        // linearly interpolate between the two samples
        pixel_color = pixel_color + (left + right) * u_kernel[f];
    }

    pixel_color.rgb /= pixel_color.a;

    gl_FragColor = pixel_color;
}