package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.ability.Landmark
import io.github.diskria.achievetodo.payloads.payloadTypeOf
import io.github.diskria.achievetodo.payloads.setCodecOf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

class LandmarksLockedStatusChangedPayload private constructor(
    private val landmarks: Set<Landmark>,
    private val isLocked: Boolean,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object Client {
        fun listen(action: ClientPlayNetworking.Context.(landmarks: Set<Landmark>, isLocked: Boolean) -> Unit) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute { context.action(payload.landmarks, payload.isLocked) }
            }
        }
    }

    object Server {
        context(player: ServerPlayer)
        fun send(landmarks: Set<Landmark>, isLocked: Boolean) {
            ServerPlayNetworking.send(player, LandmarksLockedStatusChangedPayload(landmarks, isLocked))
        }
    }

    companion object {
        private typealias Payload = LandmarksLockedStatusChangedPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            setCodecOf(Landmark.STREAM_CODEC), Payload::landmarks,
            ByteBufCodecs.BOOL, Payload::isLocked,
            ::Payload
        )

        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
