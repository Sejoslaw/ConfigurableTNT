package com.github.sejoslaw.configurabletnt;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
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

    /**
     * Id: World Id
     * Value:
     *      Id: TNT Block Position
     *      Value: Radius
     */
    private static Set<ExplosionBlockMetadata> EXPLOSION_BLOCKS = new HashSet<>();

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
        float power = this.getPowerFromName(tntStack);

//        if (power < 0) {
//            power = 1;
//        }

        BlockSnapshot snapshot = event.getBlockSnapshot();
        World world = snapshot.getWorld().getWorld();
        BlockPos tntPosition = snapshot.getPos();

        EXPLOSION_BLOCKS.add(new ExplosionBlockMetadata(world, entity, tntPosition, power));
    }

    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        World world = event.getWorld();
        BlockPos position = new BlockPos(event.getExplosion().getPosition());

        ExplosionBlockMetadata meta = EXPLOSION_BLOCKS.stream().filter(m -> m.world == world && m.position == position).findFirst().get();

        new ConfiguredExplosion(meta).doExplosion();
    }

//    @SubscribeEvent
//    public void onWorldSave(WorldEvent.Save event) {
//        // TODO: 3. Save placed TNT positions
//    }
//
//    @SubscribeEvent
//    public void onWorldLoad(WorldEvent.Load event) {
//        // TODO: 4. Load placed TNT positions
//    }

    private ItemStack getTntStack(PlayerEntity player) {
        if (player.getHeldItem(Hand.MAIN_HAND).getItem() == Items.TNT) {
            return player.getHeldItem(Hand.MAIN_HAND);
        }

        if (player.getHeldItem(Hand.OFF_HAND).getItem() == Items.TNT) {
            return player.getHeldItem(Hand.OFF_HAND);
        }

        return null;
    }

    private float getPowerFromName(ItemStack stack) {
        try {
            return Float.parseFloat(stack.getDisplayName().getString());
        } catch (Exception ex) {
            return 1;
        }
    }
}
