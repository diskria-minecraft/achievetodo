package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.injection.extension.main.LandmarkGenerationTracker;
import net.minecraft.block.BlockState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements LandmarkGenerationTracker {

    @Unique
    private boolean isLandmarkGenerationTrackingEnabled;

    @Unique
    private BlockBox landmarkBlockBox;

    protected ServerWorldMixin(
        MutableWorldProperties properties,
        RegistryKey<World> registryRef,
        DynamicRegistryManager registryManager,
        RegistryEntry<DimensionType> dimensionEntry,
        boolean isClient,
        boolean debugWorld,
        long seed,
        int maxChainedNeighborUpdates
    ) {
        super(
            properties,
            registryRef,
            registryManager,
            dimensionEntry,
            isClient,
            debugWorld,
            seed,
            maxChainedNeighborUpdates
        );
    }

    @Override
    public void achievetodo$setLandmarkGenerationTrackingEnabled(boolean isLandmarkGenerationTrackingEnabled) {
        this.isLandmarkGenerationTrackingEnabled = isLandmarkGenerationTrackingEnabled;
    }

    @Override
    public void achievetodo$setLandmarkBlockBox(BlockBox landmarkBlockBox) {
        this.landmarkBlockBox = landmarkBlockBox;
    }

    @Override
    public BlockBox achievetodo$getLandmarkBlockBox() {
        BlockBox temp = landmarkBlockBox;
        landmarkBlockBox = null;
        return temp;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        boolean result = super.setBlockState(pos, state, flags, maxUpdateDepth);
        if (result && isLandmarkGenerationTrackingEnabled) {
            if (landmarkBlockBox == null) {
                landmarkBlockBox = new BlockBox(pos);
            }
            landmarkBlockBox.encompass(pos);
        }
        return result;
    }
}
