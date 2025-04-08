precision mediump float;
varying vec3 fNormal;

void main() {
    // Base green color (RGB values)
    vec3 baseColor = vec3(0.0, 0.5, 0.0); // Medium green

    // 45-degree directional light (normalized)
    vec3 lightDir = normalize(vec3(1.0, 1.0, 0.0)); // X and Y components equal for 45°

    // Ensure normal is normalized
    vec3 normal = normalize(fNormal);

    // Calculate diffuse lighting (Lambertian)
    float diffuse = max(dot(normal, lightDir), 0.0);

    // Apply lighting to base color
    vec3 finalColor = baseColor * diffuse;

    // Add some ambient light so unlit areas aren't completely black
    float ambient = 0.7;
    finalColor += baseColor * ambient;

    gl_FragColor = vec4(finalColor, 1.0);
//    gl_FragColor = vec4(fNormal * 0.5 + 0.5, 1.0);
}