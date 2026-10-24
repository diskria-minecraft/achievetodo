package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.ability.DimensionType
import io.github.diskria.achievetodo.ability.LandmarkType
import io.github.diskria.achievetodo.payloads.payloadTypeOf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockBox
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

class LockedLandmarkResizedPayload private constructor(
    private val landmark: LandmarkType,
    private val dimension: DimensionType,
    private val oldBox: BlockBox,
    private val newBox: BlockBox,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object ClientBound {
        @JvmStatic
        fun listen(
            action: context(ClientPlayNetworking.Context) (
                landmark: LandmarkType,
                dimension: DimensionType,
                oldBox: BlockBox,
                newBox: BlockBox,
            ) -> Unit
        ) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute {
                    context(context) { action(payload.landmark, payload.dimension, payload.oldBox, payload.newBox) }
                }
            }
        }
    }

    object ServerBound {
        @JvmStatic
        context(player: ServerPlayer)
        fun send(landmark: LandmarkType, dimension: DimensionType, oldBox: BlockBox, newBox: BlockBox) {
            ServerPlayNetworking.send(player, LockedLandmarkResizedPayload(landmark, dimension, oldBox, newBox))
        }
    }

    companion object {
        private typealias Payload = LockedLandmarkResizedPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            LandmarkType.STREAM_CODEC, Payload::landmark,
            DimensionType.STREAM_CODEC, Payload::dimension,
            BlockBox.STREAM_CODEC, Payload::oldBox,
            BlockBox.STREAM_CODEC, Payload::newBox,
            ::Payload
        )

        @JvmStatic
        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
