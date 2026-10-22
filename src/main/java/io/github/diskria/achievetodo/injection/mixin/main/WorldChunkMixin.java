package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.injection.extension.main.ChunkExtension;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {

    @Inject(
        method = "<init>(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/ProtoChunk;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/WorldChunk;setStructureReferences(Ljava/util/Map;)V",
            shift = At.Shift.AFTER
        )
    )
    public void set(ServerWorld world, ProtoChunk protoChunk, WorldChunk.EntityLoader entityLoader, CallbackInfo ci) {
        WorldChunk worldChunk = (WorldChunk) (Object) this;
        if (worldChunk instanceof ChunkExtension worldChunkExtension &&
            protoChunk instanceof ChunkExtension protoChunkExtension
        ) {
            worldChunkExtension.achievetodo$setFeatureLandmarks(
                world,
                protoChunkExtension.achievetodo$getFeatureLandmarks()
            );
        }
    }
}
