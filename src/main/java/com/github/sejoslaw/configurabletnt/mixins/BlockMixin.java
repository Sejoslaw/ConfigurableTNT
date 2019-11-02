package com.github.sejoslaw.configurabletnt.mixins;

import com.github.sejoslaw.configurabletnt.ConfigurableTNT;
import com.github.sejoslaw.configurabletnt.ConfiguredExplosion;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.stream.Collectors;

@Mixin(Block.class)
public class BlockMixin { // onBlockPlaced
    @Inject(method = "onPlaced", at = @At("HEAD"))
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack, CallbackInfo info) {
        if (stack.getItem() == Items.TNT && !world.isClient) {
            float power = this.getPowerFromName(stack);
            ConfiguredExplosion configuredExplosion = new ConfiguredExplosion(world, entity, pos, power, true, Explosion.DestructionType.DESTROY);

            this.removePreviouslyPlacedBlock(configuredExplosion);

            ConfigurableTNT.EXPLOSIONS.add(configuredExplosion);
        }
    }

    private float getPowerFromName(ItemStack stack) {
        float power = 1;

        try {
            power = Float.parseFloat(stack.getName().getString());
        } finally {
            return power;
        }
    }

    private void removePreviouslyPlacedBlock(ConfiguredExplosion configuredExplosion) {
        Set<ConfiguredExplosion> copies = ConfigurableTNT.EXPLOSIONS.stream()
                .filter(x -> x.equals(configuredExplosion))
                .collect(Collectors.toSet());

        copies.forEach(x -> ConfigurableTNT.EXPLOSIONS.remove(x));
    }
}
