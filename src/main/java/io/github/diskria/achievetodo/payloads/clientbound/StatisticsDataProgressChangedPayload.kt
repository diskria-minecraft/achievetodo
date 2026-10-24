package io.github.diskria.achievetodo.payloads.clientbound

import io.github.diskria.achievetodo.payloads.payloadTypeOf
import io.github.diskria.achievetodo.tracking.TrackedStatisticsDataType
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

class StatisticsDataProgressChangedPayload private constructor(
    private val statisticsData: TrackedStatisticsDataType,
    private val progress: Int,
) : CustomPacketPayload {

    override fun type() = TYPE

    @Environment(EnvType.CLIENT)
    object ClientBound {
        @JvmStatic
        fun listen(
            action: context(ClientPlayNetworking.Context) (
                statisticsData: TrackedStatisticsDataType,
                progress: Int,
            ) -> Unit
        ) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.client().execute {
                    context(context) { action(payload.statisticsData, payload.progress) }
                }
            }
        }
    }

    object ServerBound {
        @JvmStatic
        context(player: ServerPlayer)
        fun send(statisticsData: TrackedStatisticsDataType, progress: Int) {
            ServerPlayNetworking.send(player, StatisticsDataProgressChangedPayload(statisticsData, progress))
        }
    }

    companion object {
        private typealias Payload = StatisticsDataProgressChangedPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            TrackedStatisticsDataType.STREAM_CODEC, Payload::statisticsData,
            ByteBufCodecs.VAR_INT, Payload::progress,
            ::Payload
        )

        @JvmStatic
        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}
