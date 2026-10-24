package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.ability.LandmarkType
import io.github.diskria.achievetodo.payloads.payloadTypeOf
import io.github.diskria.achievetodo.payloads.setCodecOf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

class LandmarkTypesUnlockedPayload private constructor(
    private val landmarks: Set<LandmarkType>,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object ClientBound {
        @JvmStatic
        fun listen(action: context(ClientPlayNetworking.Context) (landmarks: Set<LandmarkType>) -> Unit) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute {
                    context(context) { action(payload.landmarks) }
                }
            }
        }
    }

    object ServerBound {
        @JvmStatic
        context(player: ServerPlayer)
        fun send(landmarks: Set<LandmarkType>) {
            ServerPlayNetworking.send(player, LandmarkTypesUnlockedPayload(landmarks))
        }
    }

    companion object {
        private typealias Payload = LandmarkTypesUnlockedPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            setCodecOf(LandmarkType.STREAM_CODEC), Payload::landmarks,
            ::Payload
        )

        @JvmStatic
        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
