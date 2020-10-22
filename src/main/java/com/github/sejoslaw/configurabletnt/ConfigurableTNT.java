package com.github.sejoslaw.configurabletnt;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sejoslaw - https://github.com/Sejoslaw
 */
@Mod(ConfigurableTNT.MODID)
public class ConfigurableTNT {
    public static final String MODID = "configurabletnt";

    private static Set<ConfiguredExplosion> EXPLOSIONS = new HashSet<>();

    public ConfigurableTNT() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) entity;
        ItemStack tntStack = this.getTntStack(player);

        if (tntStack.getItem() != Items.TNT) {
            return;
        }

        float power = this.getPowerFromName(tntStack);
        BlockSnapshot snapshot = event.getBlockSnapshot();
        IWorld world = snapshot.getWorld();
        BlockPos tntPosition = snapshot.getPos();
        ConfiguredExplosion configuredExplosion = new ConfiguredExplosion((World)world, entity, tntPosition, power, true, Explosion.Mode.DESTROY);

        Set<ConfiguredExplosion> copies = EXPLOSIONS
                .stream()
                .filter(x -> x.equals(configuredExplosion))
                .collect(Collectors.toSet());

        copies.forEach(x -> EXPLOSIONS.remove(x));

        EXPLOSIONS.add(configuredExplosion);
    }

    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        World world = event.getWorld();
        Explosion explosion = event.getExplosion();
        Vector3d position = explosion.getPosition();

        ConfiguredExplosion configuredExplosion = this.findExplosion(world, explosion, position);
        configuredExplosion.doExplosion();

        EXPLOSIONS.remove(configuredExplosion);

        event.setCanceled(true);
    }

    private ConfiguredExplosion findExplosion(World world, Explosion explosion, Vector3d position) {
        ConfiguredExplosion configuredExplosion;

        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    BlockPos configuredExplosionPotentialPosition = new BlockPos(position.getX() + x, position.getY() + y, position.getZ() + z);

                    configuredExplosion = EXPLOSIONS
                            .stream()
                            .filter(exp -> exp.world == world &&
                                    new BlockPos(exp.getPosition()).equals(configuredExplosionPotentialPosition) &&
                                    world.getBlockState(configuredExplosionPotentialPosition).getBlock() == Blocks.AIR)
                            .findAny()
                            .orElse(null);

                    if (configuredExplosion != null) {
                        return configuredExplosion;
                    }
                }
            }
        }

        return new ConfiguredExplosion(world, explosion.getExplosivePlacedBy(), new BlockPos(position), 1.0f, true, Explosion.Mode.DESTROY);
    }

    private ItemStack getTntStack(PlayerEntity player) {
        if (player.getHeldItem(Hand.MAIN_HAND).getItem() == Items.TNT) {
            return player.getHeldItem(Hand.MAIN_HAND);
        }

        if (player.getHeldItem(Hand.OFF_HAND).getItem() == Items.TNT) {
            return player.getHeldItem(Hand.OFF_HAND);
        }

        return ItemStack.EMPTY;
    }

    private float getPowerFromName(ItemStack stack) {
        float power = 1;

        try {
            power = Float.parseFloat(stack.getDisplayName().getString());
        } finally {
            return power;
        }
    }
}
