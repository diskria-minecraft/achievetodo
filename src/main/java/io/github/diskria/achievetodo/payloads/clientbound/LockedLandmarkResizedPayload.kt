package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.extensions.payloadTypeOf
import io.github.diskria.achievetodo.landmarks.LandmarkType
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.levelgen.structure.BoundingBox

class LockedLandmarkResizedPayload private constructor(
    private val type: LandmarkType,
    private val oldBB: BoundingBox,
    private val newBB: BoundingBox,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object Client {
        fun listen(
            action: ClientPlayNetworking.Context.(type: LandmarkType, oldBB: BoundingBox, newBB: BoundingBox) -> Unit,
        ) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute { context.action(payload.type, payload.oldBB, payload.newBB) }
            }
        }
    }

    object Server {
        context(player: ServerPlayer)
        fun send(type: LandmarkType, oldBB: BoundingBox, newBB: BoundingBox) {
            ServerPlayNetworking.send(player, LockedLandmarkResizedPayload(type, oldBB, newBB))
        }
    }

    companion object {
        private typealias Payload = LockedLandmarkResizedPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            LandmarkType.STREAM_CODEC, Payload::type,
            BoundingBox.STREAM_CODEC, Payload::oldBB,
            BoundingBox.STREAM_CODEC, Payload::newBB,
            ::Payload
        )

        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
