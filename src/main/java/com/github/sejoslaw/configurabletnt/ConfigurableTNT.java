package com.github.sejoslaw.configurabletnt;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
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

        if (power < 0) {
            power = 1;
        }

        BlockSnapshot snapshot = event.getBlockSnapshot();
        World world = snapshot.getWorld().getWorld();
        BlockPos tntPosition = snapshot.getPos();

        EXPLOSIONS.add(new ConfiguredExplosion(world, entity, tntPosition, power, true, Explosion.Mode.DESTROY));
    }

    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        World world = event.getWorld();
        BlockPos position = new BlockPos(event.getExplosion().getPosition());

        ConfiguredExplosion configuredExplosion = EXPLOSIONS
                .stream()
                .filter(x -> x.world == world && this.isTheSamePosition(new BlockPos(x.getPosition()), position))
                .findAny()
                .orElse(new ConfiguredExplosion(world, event.getExplosion().getExplosivePlacedBy(), position, 1.0f, true, Explosion.Mode.DESTROY));

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
        try {
            return Float.parseFloat(stack.getDisplayName().getString());
        } catch (Exception ex) {
            return 1;
        }
    }

    private boolean isTheSamePosition(BlockPos position1, BlockPos position2) {
        return true; // TODO: Add appropriate logic
    }
}
