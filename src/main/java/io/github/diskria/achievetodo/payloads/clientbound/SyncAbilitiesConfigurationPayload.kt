package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.ability.AbilityType
import io.github.diskria.achievetodo.payloads.mapCodecOf
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

class SyncAbilitiesConfigurationPayload private constructor(
    private val configuration: Map<AbilityType, Int>,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object ClientBound {
        @JvmStatic
        fun listen(action: context(ClientPlayNetworking.Context) (configuration: Map<AbilityType, Int>) -> Unit) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute {
                    context(context) { action(payload.configuration) }
                }
            }
        }
    }

    object ServerBound {
        @JvmStatic
        context(player: ServerPlayer)
        fun send(configuration: Map<AbilityType, Int>) {
            ServerPlayNetworking.send(player, SyncAbilitiesConfigurationPayload(configuration))
        }
    }

    companion object {
        private typealias Payload = SyncAbilitiesConfigurationPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            mapCodecOf(AbilityType.STREAM_CODEC to ByteBufCodecs.VAR_INT), Payload::configuration,
            ::Payload
        )

        @JvmStatic
        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
