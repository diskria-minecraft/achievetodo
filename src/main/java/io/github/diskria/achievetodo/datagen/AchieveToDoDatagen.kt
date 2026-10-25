package io.github.diskria.achievetodo.datagen

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class AchieveToDoDatagen : DataGeneratorEntrypoint {

    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()
        pack.addProvider { output, registryLookup ->
            AbilityAdvancementsTabGenerator(output, registryLookup)
        }
    }
}
