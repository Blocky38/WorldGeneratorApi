package nl.rutgerkok.worldgeneratorapi.decoration;

public enum BaseDecorationType {

    /**
     * Ran directly after the terrain shape has been generated. Generally, there
     * will only be stone, air and water in the world.
     */
    RAW_GENERATION,

    /**
     * The surface is generated.
     */
    SURFACE,

    /**
     * The bedrock layers are generated.
     */
    BEDROCK,

    /**
     * Used to generate caves and ravines.
     */
    CARVING_AIR,

    /**
     * Used to generate underwater caves and ravines.
     */
    CARVING_LIQUID
}
