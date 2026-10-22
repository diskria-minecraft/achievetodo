package io.github.diskria.achievetodo.ability;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public enum DimensionType {

    OVERWORLD,
    NETHER,
    END;

    public static @Nullable DimensionType findByWorld(RegistryKey<World> world) {
        if (world == World.OVERWORLD) {
            return OVERWORLD;
        }
        if (world == World.NETHER) {
            return NETHER;
        }
        if (world == World.END) {
            return END;
        }
        return null;
    }
}
