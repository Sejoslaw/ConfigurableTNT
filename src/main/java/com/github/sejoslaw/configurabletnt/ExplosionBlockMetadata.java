package com.github.sejoslaw.configurabletnt;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ExplosionBlockMetadata {
    public final World world;
    public final Entity entity;
    public final BlockPos position;
    public final float power;

    public ExplosionBlockMetadata(World world, Entity entity, BlockPos position, float power) {
        this.world = world;
        this.entity = entity;
        this.position = position;
        this.power = power;
    }
}
