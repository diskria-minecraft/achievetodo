package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.payloads.payloadTypeOf
import io.github.diskria.achievetodo.tracking.TrackedScoreType
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

class ScoreProgressChangedPayload private constructor(
    private val score: TrackedScoreType,
    private val progress: Int,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object ClientBound {
        @JvmStatic
        fun listen(action: context(ClientPlayNetworking.Context) (score: TrackedScoreType, progress: Int) -> Unit) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute {
                    context(context) { action(payload.score, payload.progress) }
                }
            }
        }
    }

    object ServerBound {
        @JvmStatic
        context(player: ServerPlayer)
        fun send(score: TrackedScoreType, progress: Int) {
            ServerPlayNetworking.send(player, ScoreProgressChangedPayload(score, progress))
        }
    }

    companion object {
        private typealias Payload = ScoreProgressChangedPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            TrackedScoreType.STREAM_CODEC, Payload::score,
            ByteBufCodecs.VAR_INT, Payload::progress,
            ::Payload
        )

        @JvmStatic
        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
