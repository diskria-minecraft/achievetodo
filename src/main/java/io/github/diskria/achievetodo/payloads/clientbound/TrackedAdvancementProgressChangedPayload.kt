package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.payloads.payloadTypeOf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

class TrackedAdvancementProgressChangedPayload private constructor(
    private val advancementId: String,
    private val progress: Int,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object Client {
        fun listen(action: ClientPlayNetworking.Context.(advancementId: String, progress: Int) -> Unit) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute {
                    context.action(payload.advancementId, payload.progress)
                }
            }
        }
    }

    object Server {
        context(player: ServerPlayer)
        fun send(advancementId: String, progress: Int) {
            ServerPlayNetworking.send(player, TrackedAdvancementProgressChangedPayload(advancementId, progress))
        }
    }

    companion object {
        private typealias Payload = TrackedAdvancementProgressChangedPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Payload::advancementId,
            ByteBufCodecs.VAR_INT, Payload::progress,
            ::Payload
        )

        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
