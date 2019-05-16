package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;

import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.Biomes;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.property.FloatProperty;
import nl.rutgerkok.worldgeneratorapi.property.Property;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

/**
 * Default implementation of {@link PropertyRegistry}. Use
 * {@link WorldGeneratorApi#getPropertyRegistry()} to get an instance.
 *
 */
public final class PropertyRegistryImpl implements PropertyRegistry {

    /**
     * Be careful with thread safety. For example, this is wrong:
     *
     * <pre>
     * if (map.containsKey("example")) {
     *     map.get("example").doSomething();
     * }
     * </pre>
     *
     * After all, another thread could delete the value in between the first and
     * second line, making your code throw an exception. Instead, use this:
     *
     * <pre>
     * var value = map.get("example");
     * if (value != null) {
     *     value.doSomething();
     * }
     * </pre>
     *
     * See for example the {@link Map#computeIfAbsent(Object, Function)} function
     * for safely putting a value if (and only if) there is no value yet.
     */
    private final Map<NamespacedKey, Keyed> properties = new ConcurrentHashMap<>();

    public PropertyRegistryImpl() {
        addMinecraftBiomeFloatProperty(TEMPERATURE, BiomeBase::getTemperature);
        addMinecraftBiomeFloatProperty(WETNESS, BiomeBase::getHumidity);
        addMinecraftBiomeFloatProperty(BASE_HEIGHT, BiomeBase::g);
        addMinecraftBiomeFloatProperty(HEIGHT_VARIATION, BiomeBase::k);
        addMinecraftWorldProperty(WORLD_SEED, world -> (Long) world.getSeed(), -1L);
        addSeaLevelProperty(SEA_LEVEL, world -> (float) world.getSeaLevel());
    }

    private void addMinecraftBiomeFloatProperty(NamespacedKey name, Function<BiomeBase, Float> value) {
        FloatProperty property = new FloatProperty(name, value.apply(Biomes.b)) {

            @Override
            public float getBiomeDefault(Biome biome) {
                BiomeBase base = CraftBlock.biomeToBiomeBase(biome);
                return value.apply(base);
            }

            @Override
            public void setBiomeDefault(Biome biome, float value) {
                throw new UnsupportedOperationException(
                        "Cannot change " + getKey().getKey() + " globally for biomes. Try"
                                + " setting it per-world instead.");
            }

        };
        this.properties.put(name, property);
    }

    private void addSeaLevelProperty(NamespacedKey name, Function<World, Float> value) {
        FloatProperty property = new FloatProperty(name, 0f) {

            @Override
            public float getWorldDefault(WorldRef worldRef) {
                World world = Bukkit.getWorld(worldRef.getName());
                if (world == null) {
                    return Float.NaN;
                }
                return value.apply(world);
            }

            @Override
            public void setWorldDefault(WorldRef worldRef, float value) {
                throw new UnsupportedOperationException("Setting the sea level is not possible"
                        + " in Minecraft >= 1.14 - world.getSeaLevel() is hardcoded to return 63.");
            }

        };
        this.properties.put(name, property);
    }

    private <T> void addMinecraftWorldProperty(NamespacedKey name, Function<World, T> value, T defaultValue) {
        Property<T> property = new Property<T>(name, defaultValue) {

            @Override
            public @Nullable T getWorldDefault(WorldRef worldRef) {
                World world = Bukkit.getWorld(worldRef.getName());
                if (world == null) {
                    return null;
                }
                return value.apply(world);
            }

            @Override
            public void setWorldDefault(WorldRef world, T value) {
                throw new UnsupportedOperationException(
                        "Cannot change " + getKey().getKey() + " for worlds.");
            }

        };
        this.properties.put(name, property);
    }

    @Override
    public FloatProperty getFloat(NamespacedKey name, float defaultValue) {
        return (FloatProperty) properties.computeIfAbsent(name, n -> new FloatProperty(name, defaultValue));
    }

    @Override
    public <T> Property<T> getProperty(NamespacedKey name, T defaultValue) {
        @SuppressWarnings("unchecked") // Will be checked on lines following
        Property<T> property = (Property<T>) properties.computeIfAbsent(name, n -> new Property<>(name, defaultValue));
        if (!property.getDefault().getClass().equals(defaultValue.getClass())) {
            throw new ClassCastException("Cannot cast Property<" + property.getDefault().getClass().getSimpleName()
                    + "> to Property<" + defaultValue.getClass().getSimpleName() + ">");
        }
        return property;
    }
}
