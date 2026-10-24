package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.ability.DimensionType
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

class CheckTargetInLockedLandmarkPayload private constructor(
    private val dimension: DimensionType,
    private val box: BlockBox,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object ClientBound {
        @JvmStatic
        fun listen(action: context(ClientPlayNetworking.Context) (dimension: DimensionType, box: BlockBox) -> Unit) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute {
                    context(context) { action(payload.dimension, payload.box) }
                }
            }
        }
    }

    object ServerBound {
        @JvmStatic
        context(player: ServerPlayer)
        fun send(dimension: DimensionType, box: BlockBox) {
            ServerPlayNetworking.send(player, CheckTargetInLockedLandmarkPayload(dimension, box))
        }
    }

    companion object {
        private typealias Payload = CheckTargetInLockedLandmarkPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            DimensionType.STREAM_CODEC, Payload::dimension,
            BlockBox.STREAM_CODEC, Payload::box,
            ::Payload
        )

        @JvmStatic
        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
