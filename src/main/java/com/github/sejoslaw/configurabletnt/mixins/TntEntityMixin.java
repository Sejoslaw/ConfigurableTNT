package com.github.sejoslaw.configurabletnt.mixins;

import com.github.sejoslaw.configurabletnt.ConfigurableTNT;
import com.github.sejoslaw.configurabletnt.ConfiguredExplosion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntEntity.class)
public abstract class TntEntityMixin extends Entity { // onExplosionOccurred
    public TntEntityMixin(EntityType<? extends TntEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "explode", at = @At("HEAD"))
    private void explode(CallbackInfo info) {
        ConfiguredExplosion configuredExplosion = ConfigurableTNT.EXPLOSIONS
                .stream()
                .filter(x -> x.world == this.world && new BlockPos(x.getPosition()).equals(this.getBlockPos()))
                .findAny()
                .orElse(new ConfiguredExplosion(this.world, null, this.getBlockPos(), 1.0f, true, Explosion.DestructionType.DESTROY));

        configuredExplosion.setNewExplosionPosition(this.getX(), this.getY(), this.getZ());
        configuredExplosion.doExplosion();

        ConfigurableTNT.EXPLOSIONS.remove(configuredExplosion);
    }
}