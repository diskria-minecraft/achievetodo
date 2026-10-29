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

class SyncObtainedAdvancementsCountPayload private constructor(
    private val count: Int,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object Client {
        fun listen(action: ClientPlayNetworking.Context.(count: Int) -> Unit) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute { context.action(payload.count) }
            }
        }
    }

    object Server {
        context(player: ServerPlayer)
        fun send(count: Int) {
            ServerPlayNetworking.send(player, SyncObtainedAdvancementsCountPayload(count))
        }
    }

    companion object {
        private typealias Payload = SyncObtainedAdvancementsCountPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Payload::count,
            ::Payload
        )

        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
