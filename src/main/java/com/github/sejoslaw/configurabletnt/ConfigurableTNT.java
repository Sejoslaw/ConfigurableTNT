package com.github.sejoslaw.configurabletnt;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
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
        World world = snapshot.getWorld().getWorld();
        BlockPos tntPosition = snapshot.getPos();
        ConfiguredExplosion configuredExplosion = new ConfiguredExplosion(world, entity, tntPosition, power, true, Explosion.Mode.DESTROY);

        this.removePreviouslyPlacedBlock(configuredExplosion);

        EXPLOSIONS.add(configuredExplosion);
    }

    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        World world = event.getWorld();
        Explosion explosion = event.getExplosion();
        Vec3d position = explosion.getPosition();
        BlockPos explosionPosition = new BlockPos((double)((float)position.getX() + 0.5F), (double)position.getY(), (double)((float)position.getZ() + 0.5F));

        ConfiguredExplosion configuredExplosion = EXPLOSIONS
                .stream()
                .filter(x -> x.world == world && new BlockPos(x.getPosition()).equals(explosionPosition))
                .findAny()
                .orElse(new ConfiguredExplosion(world, explosion.getExplosivePlacedBy(), explosionPosition, 1.0f, true, Explosion.Mode.DESTROY));

        configuredExplosion.setNewExplosionPosition(explosionPosition.getX(), explosionPosition.getY(), explosionPosition.getZ());
        configuredExplosion.doExplosion();
        EXPLOSIONS.remove(configuredExplosion);

        event.setCanceled(true);
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

    private void removePreviouslyPlacedBlock(ConfiguredExplosion configuredExplosion) {
        Set<ConfiguredExplosion> copies = EXPLOSIONS.stream()
                .filter(x -> x.equals(configuredExplosion))
                .collect(Collectors.toSet());

        copies.forEach(x -> EXPLOSIONS.remove(x));
    }
}
