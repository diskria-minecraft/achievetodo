package io.github.diskria.achievetodo.payloads.serverbound

import io.github.diskria.achievetodo.ability.AbilityType
import io.github.diskria.achievetodo.payloads.payloadTypeOf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class DemystifyAbilityPayload private constructor(
    private val ability: AbilityType,
) : CustomPacketPayload {

    override fun type() = TYPE

    object ServerBound {
        @JvmStatic
        fun listen(action: context(ServerPlayNetworking.Context) (ability: AbilityType) -> Unit) {
            ServerPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.server().execute {
                    context(context) { action(payload.ability) }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    object ClientBound {
        @JvmStatic
        fun send(ability: AbilityType) {
            ClientPlayNetworking.send(DemystifyAbilityPayload(ability))
        }
    }

    companion object {
        private typealias Payload = DemystifyAbilityPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            AbilityType.STREAM_CODEC, Payload::ability,
            ::Payload
        )

        @JvmStatic
        fun register() = PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC)
    }
}
