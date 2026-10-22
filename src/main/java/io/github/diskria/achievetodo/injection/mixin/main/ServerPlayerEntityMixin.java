package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(
        method = "jump",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockJump(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (!player.isTouchingWater() && AchieveToDoMod.isAbilityLocked(player, AbilityType.JUMP)) {
            ci.cancel();
        }
    }

    @ModifyReturnValue(
        method = "getWorldSpawnPos",
        at = @At("RETURN")
    )
    public BlockPos setSafeWorldSpawn(
        @NotNull BlockPos original,
        @Local(argsOnly = true) @NotNull ServerWorld world
    ) {
        int maxY = world.getBottomY() - 1;
        BlockPos highestBlockPos = original;

        int centerX = original.getX();
        int centerZ = original.getZ();
        int startX = centerX - 50;
        int endX = centerX + 50;
        int startZ = centerZ - 50;
        int endZ = centerZ + 50;

        int chunkStartX = startX >> 4;
        int chunkEndX = endX >> 4;
        int chunkStartZ = startZ >> 4;
        int chunkEndZ = endZ >> 4;

        for (int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++) {
            for (int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }
                Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);
                int chunkMaxY;
                if (heightmap == null) {
                    chunkMaxY = world.getBottomY();
                } else {
                    int tmpMax = world.getBottomY();
                    for (int localX = 0; localX < 16; localX++) {
                        for (int localZ = 0; localZ < 16; localZ++) {
                            int y = heightmap.get(localX, localZ);
                            if (y > tmpMax) {
                                tmpMax = y;
                            }
                        }
                    }
                    chunkMaxY = tmpMax;
                }
                if (chunkMaxY <= maxY) {
                    continue;
                }
                int blockMinX = chunkX << 4;
                int blockMaxX = blockMinX + 15;
                int blockMinZ = chunkZ << 4;
                int blockMaxZ = blockMinZ + 15;

                int realStartX = Math.max(blockMinX, startX);
                int realEndX = Math.min(blockMaxX, endX);
                int realStartZ = Math.max(blockMinZ, startZ);
                int realEndZ = Math.min(blockMaxZ, endZ);

                for (int x = realStartX; x <= realEndX; x++) {
                    for (int z = realStartZ; z <= realEndZ; z++) {
                        int topY = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z);
                        if (topY > maxY) {
                            maxY = topY;
                            BlockPos topPos = new BlockPos(x, topY, z);
                            BlockState state = world.getBlockState(topPos);
                            if (!state.getFluidState().isEmpty()) {
                                continue;
                            }
                            VoxelShape shape = state.getCollisionShape(world, topPos);
                            if (shape.isEmpty() || shape.getMax(Direction.Axis.Y) < 1.0D) {
                                continue;
                            }
                            double maxCollisionY = shape.getMax(Direction.Axis.Y);
                            if (maxCollisionY >= 1.0D) {
                                topY++;
                            }
                            highestBlockPos = new BlockPos(x, topY, z);
                        }
                    }
                }
            }
        }
        return highestBlockPos;
    }
}
