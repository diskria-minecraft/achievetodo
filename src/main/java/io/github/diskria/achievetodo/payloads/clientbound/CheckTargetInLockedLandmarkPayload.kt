package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.ability.DimensionType
import io.github.diskria.achievetodo.payloads.payloadTypeOf
import io.netty.buffer.ByteBuf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.AABB

class CheckTargetInLockedLandmarkPayload private constructor(
    private val dimension: DimensionType,
    private val aabb: AABB,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object Client {
        fun listen(action: ClientPlayNetworking.Context.(dimension: DimensionType, aabb: AABB) -> Unit) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute { context.action(payload.dimension, payload.aabb) }
            }
        }
    }

    object Server {
        context(player: ServerPlayer)
        fun send(dimension: DimensionType, aabb: AABB) {
            ServerPlayNetworking.send(player, CheckTargetInLockedLandmarkPayload(dimension, aabb))
        }
    }

    companion object {
        private typealias Payload = CheckTargetInLockedLandmarkPayload

        private val AABB_STREAM_CODEC = object : StreamCodec<ByteBuf, AABB> {

            override fun decode(input: ByteBuf) = AABB(
                input.readDouble(), input.readDouble(), input.readDouble(),
                input.readDouble(), input.readDouble(), input.readDouble()
            )

            override fun encode(output: ByteBuf, box: AABB) {
                output.writeDouble(box.minX)
                output.writeDouble(box.minY)
                output.writeDouble(box.minZ)
                output.writeDouble(box.maxX)
                output.writeDouble(box.maxY)
                output.writeDouble(box.maxZ)
            }
        }

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            DimensionType.STREAM_CODEC, Payload::dimension,
            AABB_STREAM_CODEC, Payload::aabb,
            ::Payload
        )

        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
