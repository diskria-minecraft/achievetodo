//package io.github.diskria.achievetodo.injection.mixin.client;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.BufferBuilder;
//import io.github.diskria.achievetodo.ability.DimensionType;
//import io.github.diskria.achievetodo.client.AchieveToDoClient;
//import io.github.diskria.achievetodo.client.gui.DesignCodePalette;
//import io.github.diskria.achievetodo.ability.Landmark;
//import net.minecraft.client.renderer.LevelRenderer;
//import net.minecraft.core.BlockBox;
//import net.minecraft.resources.Identifier;
//import net.minecraft.util.Util;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import java.util.concurrent.TimeUnit;
//
//@Mixin(LevelRenderer.class)
//public abstract class WorldRendererMixin {
//
//    @Unique
//    private static final long LOCKED_LANDMARK_BORDER_ANIMATION_DURATION = TimeUnit.SECONDS.toMillis(6);
//
//    @Unique
//    private static final float ENTER_LOCKED_LANDMARK_BORDER_FADE_ALPHA_SPEED = 0.003f;
//
//    @Unique
//    private static final Identifier LOCKED_LANDMARK_BORDER_TEXTURE =
//        Identifier.withDefaultNamespace("textures/misc/forcefield.png");
//
//    @Unique
//    private Landmark lastLandmark = null;
//
//    @Unique
//    private float fadeInsideLockedLandmarkAlpha;
//
//    @Unique
//    private void renderLockedLandmarkBorderFloor(
//        @NotNull BufferBuilder b,
//        float minY,
//        float x, float segmentX, float halfSegmentX,
//        float z, float segmentZ, float halfSegmentZ,
//        float textureU,
//        float animationTime
//    ) {
//        b.addVertex(x + segmentX, minY, z).setUv(animationTime - textureU, animationTime + halfSegmentZ);
//        b.addVertex(x + segmentX, minY, z + segmentZ).setUv(animationTime - textureU, animationTime);
//        b.addVertex(x, minY, z + segmentZ).setUv(animationTime - textureU + halfSegmentX, animationTime);
//        b.addVertex(x, minY, z).setUv(animationTime - textureU + halfSegmentX, animationTime + halfSegmentZ);
//    }
//
//    @Unique
//    private void renderLockedLandmarkBorderRoof(
//        @NotNull BufferBuilder b,
//        float maxY,
//        float x, float segmentX, float halfSegmentX,
//        float z, float segmentZ, float halfSegmentZ,
//        float textureU,
//        float animationTime
//    ) {
//        b.addVertex(x, maxY, z).setUv(animationTime - textureU + halfSegmentX, animationTime + halfSegmentZ);
//        b.addVertex(x, maxY, z + segmentZ).setUv(animationTime - textureU + halfSegmentX, animationTime);
//        b.addVertex(x + segmentX, maxY, z + segmentZ).setUv(animationTime - textureU, animationTime);
//        b.addVertex(x + segmentX, maxY, z).setUv(animationTime - textureU, animationTime + halfSegmentZ);
//    }
//
//    @Unique
//    private void renderLockedLandmarkBorderNorthWall(
//        @NotNull BufferBuilder b,
//        float minZ,
//        float x, float segmentX, float halfSegmentX,
//        float y, float segmentY, float halfSegmentY,
//        float textureU,
//        float animationTime
//    ) {
//        b.addVertex(x, y, minZ).setUv(animationTime - textureU + halfSegmentX, animationTime);
//        b.addVertex(x, y + segmentY, minZ).setUv(animationTime - textureU + halfSegmentX, animationTime + halfSegmentY);
//        b.addVertex(x + segmentX, y + segmentY, minZ).setUv(animationTime - textureU, animationTime + halfSegmentY);
//        b.addVertex(x + segmentX, y, minZ).setUv(animationTime - textureU, animationTime);
//    }
//
//    @Unique
//    private void renderLockedLandmarkBorderSouthWall(
//        @NotNull BufferBuilder b,
//        float maxZ,
//        float x, float segmentX, float halfSegmentX,
//        float y, float segmentY, float halfSegmentY,
//        float textureU,
//        float animationTime
//    ) {
//        b.addVertex(x, y, maxZ).setUv(animationTime - textureU + halfSegmentX, animationTime);
//        b.addVertex(x + segmentX, y, maxZ).setUv(animationTime - textureU, animationTime);
//        b.addVertex(x + segmentX, y + segmentY, maxZ).setUv(animationTime - textureU, animationTime + halfSegmentY);
//        b.addVertex(x, y + segmentY, maxZ).setUv(animationTime - textureU + halfSegmentX, animationTime + halfSegmentY);
//    }
//
//    @Unique
//    private void renderLockedLandmarkBorderWestWall(
//        @NotNull BufferBuilder b,
//        float minX,
//        float y, float segmentY, float halfSegmentY,
//        float z, float segmentZ, float halfSegmentZ,
//        float textureU,
//        float animationTime
//    ) {
//        b.addVertex(minX, y, z).setUv(animationTime - textureU + halfSegmentZ, animationTime);
//        b.addVertex(minX, y, z + segmentZ).setUv(animationTime - textureU, animationTime);
//        b.addVertex(minX, y + segmentY, z + segmentZ).setUv(animationTime - textureU, animationTime + halfSegmentY);
//        b.addVertex(minX, y + segmentY, z).setUv(animationTime - textureU + halfSegmentZ, animationTime + halfSegmentY);
//    }
//
//    @Unique
//    private void renderLockedLandmarkBorderEastWall(
//        @NotNull BufferBuilder b,
//        float maxX,
//        float y, float segmentY, float halfSegmentY,
//        float z, float segmentZ, float halfSegmentZ,
//        float textureU,
//        float animationTime
//    ) {
//        b.addVertex(maxX, y + segmentY, z).setUv(animationTime - textureU + halfSegmentZ, animationTime + halfSegmentY);
//        b.addVertex(maxX, y + segmentY, z + segmentZ).setUv(animationTime - textureU + halfSegmentZ, animationTime);
//        b.addVertex(maxX, y, z + segmentZ).setUv(animationTime - textureU, animationTime);
//        b.addVertex(maxX, y, z).setUv(animationTime - textureU, animationTime + halfSegmentY);
//    }
//
//    @Inject(
//        method = "method_62216",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V",
//            shift = At.Shift.AFTER
//        )
//    )
//    public void renderLockedLandmarkBorder(
//        Fog fog,
//        float tickDelta,
//        Vec3d cameraPos,
//        int viewDistance,
//        float farPlaneDistance,
//        CallbackInfo ci
//    ) {
//        if (world == null) {
//            return;
//        }
//        DimensionType dimensionType = DimensionType.findByLevel(world.getRegistryKey());
//        if (dimensionType == null) {
//            return;
//        }
//        Landmark foundBox = null;
//        for (Landmark landmark : AchieveToDoClient.Companion.getLockedLandmarksSet()) {
//            if (landmark.getDimensionType() != dimensionType) {
//                continue;
//            }
//            BlockBox box = landmark.getArea();
//
//            float minX = (float) box.minX;
//            float minY = (float) box.minY;
//            float minZ = (float) box.minZ;
//            float maxX = (float) box.maxX;
//            float maxY = (float) box.maxY;
//            float maxZ = (float) box.maxZ;
//
//            float boxPadding;
//            if (box.getLengthX() > 1.0f && box.getLengthY() > 1.0f && box.getLengthZ() > 1.0f) {
//                boxPadding = 0.5f;
//            } else {
//                boxPadding = 0.001f;
//            }
//
//            minX += boxPadding;
//            maxX -= boxPadding;
//            minY += boxPadding;
//            maxY -= boxPadding;
//            minZ += boxPadding;
//            maxZ -= boxPadding;
//
//            double cameraX = cameraPos.x;
//            double cameraY = cameraPos.y;
//            double cameraZ = cameraPos.z;
//            if (cameraX < minX || cameraX >= maxX ||
//                cameraY < minY || cameraY >= maxY ||
//                cameraZ < minZ || cameraZ >= maxZ
//            ) {
//                continue;
//            }
//
//            boolean isFloorVisible = frustum.intersectAab(minX, minY, minZ, maxX, minY, maxZ) < 0;
//            boolean isRoofVisible = frustum.intersectAab(minX, maxY, minZ, maxX, maxY, maxZ) < 0;
//            boolean isNorthWallVisible = frustum.intersectAab(minX, minY, minZ, maxX, maxY, minZ) < 0;
//            boolean isSouthWallVisible = frustum.intersectAab(minX, minY, maxZ, maxX, maxY, maxZ) < 0;
//            boolean isWestWallVisible = frustum.intersectAab(minX, minY, minZ, minX, maxY, maxZ) < 0;
//            boolean isEastWallVisible = frustum.intersectAab(maxX, minY, minZ, maxX, maxY, maxZ) < 0;
//
//            if (!isFloorVisible &&
//                !isRoofVisible &&
//                !isNorthWallVisible &&
//                !isSouthWallVisible &&
//                !isWestWallVisible &&
//                !isEastWallVisible
//            ) {
//                continue;
//            }
//
//            minX -= (float) cameraX;
//            maxX -= (float) cameraX;
//            minY -= (float) cameraY;
//            maxY -= (float) cameraY;
//            minZ -= (float) cameraZ;
//            maxZ -= (float) cameraZ;
//
//            foundBox = landmark;
//            if (foundBox.equals(lastLandmark)) {
//                fadeInsideLockedLandmarkAlpha += ENTER_LOCKED_LANDMARK_BORDER_FADE_ALPHA_SPEED * tickDelta;
//                if (fadeInsideLockedLandmarkAlpha > 1.0f) {
//                    fadeInsideLockedLandmarkAlpha = 1.0f;
//                }
//            } else {
//                fadeInsideLockedLandmarkAlpha = 0.0f;
//            }
//
//            RenderSystem.setShaderTexture(0, LOCKED_LANDMARK_BORDER_TEXTURE);
//            RenderLayer renderLayer = RenderLayer.getWorldBorder(MinecraftClient.isFabulousGraphicsOrBetter());
//            renderLayer.startDrawing();
//            float animationTime = (float) (Util.getMeasuringTimeMs() % LOCKED_LANDMARK_BORDER_ANIMATION_DURATION) /
//                LOCKED_LANDMARK_BORDER_ANIMATION_DURATION;
//
//            RenderSystem.setShaderColor(
//                ColorHelper.getRedFloat(DesignCodePalette.IN_WORLD_RGB),
//                ColorHelper.getGreenFloat(DesignCodePalette.IN_WORLD_RGB),
//                ColorHelper.getBlueFloat(DesignCodePalette.IN_WORLD_RGB),
//                fadeInsideLockedLandmarkAlpha
//            );
//
//            BufferBuilder bufferBuilder = Tessellator.getInstance()
//                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
//
//            if (isFloorVisible || isRoofVisible) {
//                for (float z = minZ; z < maxZ; ) {
//                    float segmentZ = Math.min(1.0f, maxZ - z);
//                    float textureU = 0f;
//                    for (float x = minX; x < maxX; ) {
//                        float segmentX = Math.min(1.0f, maxX - x);
//                        float halfSegmentX = segmentX * 0.5f;
//                        float halfSegmentZ = segmentZ * 0.5f;
//                        if (isFloorVisible) {
//                            renderLockedLandmarkBorderFloor(
//                                bufferBuilder,
//                                minY,
//                                x, segmentX, halfSegmentX,
//                                z, segmentZ, halfSegmentZ,
//                                textureU,
//                                animationTime
//                            );
//                        }
//                        if (isRoofVisible) {
//                            renderLockedLandmarkBorderRoof(
//                                bufferBuilder,
//                                maxY,
//                                x, segmentX, halfSegmentX,
//                                z, segmentZ, halfSegmentZ,
//                                textureU,
//                                animationTime
//                            );
//                        }
//                        x += segmentX;
//                        textureU += 0.5f;
//                    }
//                    z += segmentZ;
//                }
//            }
//            if (isNorthWallVisible || isSouthWallVisible || isWestWallVisible || isEastWallVisible) {
//                for (float y = minY; y < maxY; ) {
//                    float segmentY = Math.min(1.0f, maxY - y);
//                    float textureU = 0f;
//                    if (isNorthWallVisible || isSouthWallVisible) {
//                        for (float x = minX; x < maxX; ) {
//                            float segmentX = Math.min(1.0f, maxX - x);
//                            float halfSegmentX = segmentX * 0.5f;
//                            float halfSegmentY = segmentY * 0.5f;
//                            if (isNorthWallVisible) {
//                                renderLockedLandmarkBorderNorthWall(
//                                    bufferBuilder,
//                                    minZ,
//                                    x, segmentX, halfSegmentX,
//                                    y, segmentY, halfSegmentY,
//                                    textureU,
//                                    animationTime
//                                );
//                            }
//                            if (isSouthWallVisible) {
//                                renderLockedLandmarkBorderSouthWall(
//                                    bufferBuilder,
//                                    maxZ,
//                                    x, segmentX, halfSegmentX,
//                                    y, segmentY, halfSegmentY,
//                                    textureU,
//                                    animationTime
//                                );
//                            }
//                            x += segmentX;
//                            textureU += 0.5f;
//                        }
//                    }
//                    if (isWestWallVisible || isEastWallVisible) {
//                        for (float z = minZ; z < maxZ; ) {
//                            float segmentZ = Math.min(1.0f, maxZ - z);
//                            float halfSegmentZ = segmentZ * 0.5f;
//                            float halfSegmentY = segmentY * 0.5f;
//                            if (isWestWallVisible) {
//                                renderLockedLandmarkBorderWestWall(
//                                    bufferBuilder,
//                                    minX,
//                                    y, segmentY, halfSegmentY,
//                                    z, segmentZ, halfSegmentZ,
//                                    textureU,
//                                    animationTime
//                                );
//                            }
//                            if (isEastWallVisible) {
//                                renderLockedLandmarkBorderEastWall(
//                                    bufferBuilder,
//                                    maxX,
//                                    y, segmentY, halfSegmentY,
//                                    z, segmentZ, halfSegmentZ,
//                                    textureU,
//                                    animationTime
//                                );
//                            }
//                            z += segmentZ;
//                            textureU += 0.5f;
//                        }
//                    }
//                    y += segmentY;
//                }
//            }
//
//            BuiltBuffer buffer = bufferBuilder.endNullable();
//            if (buffer != null) {
//                BufferRenderer.drawWithGlobalProgram(buffer);
//            }
//            renderLayer.endDrawing();
//            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//            break;
//        }
//        if (foundBox == null) {
//            fadeInsideLockedLandmarkAlpha = 0.0f;
//        }
//        lastLandmark = foundBox;
//    }
//}
