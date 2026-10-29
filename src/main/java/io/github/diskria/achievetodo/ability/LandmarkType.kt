package io.github.diskria.achievetodo.ability

import net.minecraft.data.worldgen.features.CaveFeatures
import net.minecraft.data.worldgen.features.MiscOverworldFeatures
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ByIdMap
import net.minecraft.util.StringRepresentable
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.structure.BuiltinStructures
import net.minecraft.world.level.levelgen.structure.Structure

enum class LandmarkType(
    vararg val structureKeys: ResourceKey<Structure>,
    val featureKey: ResourceKey<Feature>? = null,
) : StringRepresentable {

    DESERT_PYRAMID(BuiltinStructures.DESERT_PYRAMID),
    DESERT_WELL(featureKey = MiscOverworldFeatures.DESERT_WELL),
    JUNGLE_PYRAMID(BuiltinStructures.JUNGLE_TEMPLE),
    PILLAGER_OUTPOST(BuiltinStructures.PILLAGER_OUTPOST),
    IGLOO(BuiltinStructures.IGLOO),
    SWAMP_HUT(BuiltinStructures.SWAMP_HUT),
    MANSION(BuiltinStructures.WOODLAND_MANSION),
    VILLAGE(
        BuiltinStructures.VILLAGE_PLAINS,
        BuiltinStructures.VILLAGE_DESERT,
        BuiltinStructures.VILLAGE_SAVANNA,
        BuiltinStructures.VILLAGE_SNOWY,
        BuiltinStructures.VILLAGE_TAIGA,
    ),
    RUINED_PORTAL(
        BuiltinStructures.RUINED_PORTAL_STANDARD,
        BuiltinStructures.RUINED_PORTAL_DESERT,
        BuiltinStructures.RUINED_PORTAL_JUNGLE,
        BuiltinStructures.RUINED_PORTAL_SWAMP,
        BuiltinStructures.RUINED_PORTAL_MOUNTAIN,
        BuiltinStructures.RUINED_PORTAL_OCEAN,
    ),
    BURIED_TREASURE(BuiltinStructures.BURIED_TREASURE),
    SHIPWRECK(
        BuiltinStructures.SHIPWRECK,
        BuiltinStructures.SHIPWRECK_BEACHED,
    ),
    OCEAN_RUIN(
        BuiltinStructures.OCEAN_RUIN_COLD,
        BuiltinStructures.OCEAN_RUIN_WARM,
    ),
    MONUMENT(BuiltinStructures.OCEAN_MONUMENT),
    MONSTER_ROOM(featureKey = CaveFeatures.MONSTER_ROOM),
    MINESHAFT(
        BuiltinStructures.MINESHAFT,
        BuiltinStructures.MINESHAFT_MESA,
    ),
    TRAIL_RUINS(BuiltinStructures.TRAIL_RUINS),
    ANCIENT_CITY(BuiltinStructures.ANCIENT_CITY),
    TRIAL_CHAMBERS(BuiltinStructures.TRIAL_CHAMBERS),
    STRONGHOLD(BuiltinStructures.STRONGHOLD),
    FORTRESS(BuiltinStructures.FORTRESS),
    BASTION_REMNANT(BuiltinStructures.BASTION_REMNANT),
    END_CITY(BuiltinStructures.END_CITY);

    val id = name.lowercase()
    val isFeature = featureKey != null
    val isStructure = !isFeature

    override fun getSerializedName(): String = id

    companion object {
        private val BY_STRUCTURE_KEY = entries.flatMap { it.structureKeys.map { key -> key to it } }.toMap()
        private val BY_FEATURE_KEY = entries.filter { it.isFeature }.associateBy { it.featureKey }

        fun fromId(id: String) = entries.find { it.id == id }
        fun fromFeature(key: ResourceKey<Feature>) = BY_FEATURE_KEY[key]
        fun fromStructure(key: ResourceKey<Structure>) = BY_STRUCTURE_KEY[key]

        private val BY_ORDINAL = ByIdMap.continuous(
            LandmarkType::ordinal,
            entries.toTypedArray(),
            ByIdMap.OutOfBoundsStrategy.ZERO
        )

        val STREAM_CODEC = ByteBufCodecs.idMapper(BY_ORDINAL, LandmarkType::ordinal)
        val CODEC = StringRepresentable.fromEnum { LandmarkType.entries.toTypedArray() }
    }
}
