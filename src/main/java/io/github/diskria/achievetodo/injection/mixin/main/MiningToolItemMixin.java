package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.injection.extension.main.MiningToolItemExtension;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MiningToolItem.class)
public class MiningToolItemMixin implements MiningToolItemExtension {

    @Unique
    private ToolMaterial material;

    @Override
    public ToolMaterial achievetodo$getMaterial() {
        return material;
    }

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void saveMaterial(
        ToolMaterial material,
        TagKey<Block> effectiveBlocks,
        float attackDamage,
        float attackSpeed,
        Item.Settings settings,
        CallbackInfo ci
    ) {
        this.material = material;
    }
}
