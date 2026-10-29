package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.ability.LandmarkArea
import io.github.diskria.achievetodo.ability.LandmarkType
import io.github.diskria.achievetodo.payloads.payloadTypeOf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

class LockedLandmarkResizedPayload private constructor(
    private val landmark: LandmarkType,
    private val oldArea: LandmarkArea,
    private val newArea: LandmarkArea,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object Client {
        fun listen(
            action: ClientPlayNetworking.Context.(
                landmark: LandmarkType,
                oldArea: LandmarkArea,
                newArea: LandmarkArea,
            ) -> Unit
        ) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute { context.action(payload.landmark, payload.oldArea, payload.newArea) }
            }
        }
    }

    object Server {
        context(player: ServerPlayer)
        fun send(landmark: LandmarkType, oldArea: LandmarkArea, newArea: LandmarkArea) {
            ServerPlayNetworking.send(player, LockedLandmarkResizedPayload(landmark, oldArea, newArea))
        }
    }

    companion object {
        private typealias Payload = LockedLandmarkResizedPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            LandmarkType.STREAM_CODEC, Payload::landmark,
            LandmarkArea.STREAM_CODEC, Payload::oldArea,
            LandmarkArea.STREAM_CODEC, Payload::newArea,
            ::Payload
        )

        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
