package com.github.sejoslaw.configurabletnt;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.*;

public class ConfiguredExplosion extends Explosion {
    public final World world;
    public final Entity exploder;
    public final Random random = new Random();
    public final DamageSource damageSource;
    public final List<EntityDamage> entitiesInRange = new ArrayList<>();
    public final long[][] destroyedBlockPositions;
    public final double maxDistance;

    public final int mapHeight;
    public final int areaSize;
    public final int areaX;
    public final int areaZ;

    private float power;
    private double explosionX;
    private double explosionY;
    private double explosionZ;

    private static class EntityDamage {
        final Entity entity;
        final int distance;

        double health;
        double damage;

        double motionX;
        double motionY;
        double motionZ;

        EntityDamage(Entity entity, int distance, double health) {
            this.entity = entity;
            this.distance = distance;
            this.health = health;
        }
    }

    public ConfiguredExplosion(World world, Entity entity, BlockPos position, float power, boolean flaming, DestructionType mode) {
        this(world, entity, position.getX(), position.getY(), position.getZ(), power, flaming, mode);
    }

    public ConfiguredExplosion(World world, Entity entity, double x, double y, double z, float power, boolean flaming, DestructionType mode) {
        super(world, entity, x, y, z, power, flaming, mode);

        this.world = world;
        this.exploder = entity;

        this.setNewExplosionPosition(x, y, z);

        this.mapHeight = world.getHeight();
        this.power = power;

        this.maxDistance = (this.power / 0.4D);
        int maxDistanceInt = (int) Math.ceil(this.maxDistance);
        this.areaSize = (maxDistanceInt * 2);

        this.areaX = (roundToNegInf(x) - maxDistanceInt);
        this.areaZ = (roundToNegInf(z) - maxDistanceInt);

        this.damageSource = DamageSource.explosion(this);
        this.destroyedBlockPositions = new long[this.mapHeight][];
    }

    public void setNewExplosionPosition(double x, double y, double z) {
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;
    }

    public BlockPos getPosition() {
        return new BlockPos(this.explosionX, this.explosionY, this.explosionZ);
    }

    public boolean equals(ConfiguredExplosion explosion) {
        return this.explosionX == explosion.explosionX &&
                this.explosionY == explosion.explosionY &&
                this.explosionZ == explosion.explosionZ &&
                this.world.getDimension().getType().getRawId() == explosion.world.getDimension().getType().getRawId();
    }

    public void doExplosion() {
        if (this.power <= 0.0F) {
            this.power = 1;
        }

        int range = this.areaSize / 2;

        BlockPos pos = this.getPosition();
        BlockPos start = pos.add(-range, -range, -range);
        BlockPos end = pos.add(range, range, range);

        List<Entity> entities = this.world.getEntities(null, new Box(start, end));

        for (Entity entity : entities) {
            if (((entity instanceof LivingEntity)) || ((entity instanceof ItemEntity))) {
                int distance = (int) (square(entity.getX() - this.explosionX)
                        + square(entity.getY() - this.explosionY)
                        + square(entity.getZ() - this.explosionZ));
                double health = getEntityHealth(entity);

                this.entitiesInRange.add(new EntityDamage(entity, distance, health));
            }
        }

        boolean entitiesAreInRange = !this.entitiesInRange.isEmpty();

        if (entitiesAreInRange) {
            Collections.sort(this.entitiesInRange, new Comparator<Object>() {
                public int compare(Object a, Object b) {
                    return ((EntityDamage) a).distance - ((EntityDamage) b).distance;
                }
            });
        }

        int steps = (int) Math.ceil(Math.PI / Math.atan(1.0D / this.maxDistance));
        BlockPos.Mutable tmpPos = new BlockPos.Mutable();

        for (int phiN = 0; phiN < 2 * steps; ++phiN) {
            for (int thetaN = 0; thetaN < steps; ++thetaN) {
                double phi = 6.283185307179586D / steps * phiN;
                double theta = Math.PI / steps * thetaN;

                this.shootRay(this.explosionX, this.explosionY, this.explosionZ, phi, theta, this.power,
                        (entitiesAreInRange) && (phiN % 8 == 0) && (thetaN % 8 == 0), tmpPos);
            }
        }

        for (EntityDamage entityDamage : this.entitiesInRange) {
            Entity entity = entityDamage.entity;

            entity.damage(this.damageSource, (float) entityDamage.damage);

            double motionSquare = square(entity.getVelocity().getX()) + square(entity.getVelocity().getY()) + square(entity.getVelocity().getZ());
            double reduction = motionSquare > 3600.0D ? Math.sqrt(3600.0D / motionSquare) : 1.0D;
            Vec3d newMotion = entity.getVelocity().add(entityDamage.motionX * reduction, entityDamage.motionY * reduction, entityDamage.motionZ * reduction);

            entity.setVelocity(newMotion);
        }

        Random worldRandom = this.world.random;
        boolean doDrops = this.world.getGameRules().getBoolean(new GameRules.RuleKey<>("doTileDrops"));

        this.world.playSound(null, this.explosionX, this.explosionY, this.explosionZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F,
                (1.0F + (worldRandom.nextFloat() - worldRandom.nextFloat()) * 0.2F) * 0.7F);

        int realIndex;

        for (int y = 0; y < this.destroyedBlockPositions.length; ++y) {
            long[] bitSet = this.destroyedBlockPositions[y];

            if (bitSet != null) {
                int index = -2;

                while ((index = nextSetIndex(index + 2, bitSet, 2)) != -1) {
                    realIndex = index / 2;

                    int z = realIndex / this.areaSize;
                    int x = realIndex - z * this.areaSize;

                    x += this.areaX;
                    z += this.areaZ;

                    tmpPos.set(x, y, z);

                    BlockState state = this.world.getBlockState(tmpPos);
                    Block block = state.getBlock();

                    if ((this.power >= 20.0F) || ((doDrops) && (block.shouldDropItemsOnExplosion(this)) && (getAtIndex(index, bitSet, 2) == 1))) {
                        BlockEntity blockEntity = block.hasBlockEntity() ? this.world.getBlockEntity(tmpPos) : null;

                        LootContext.Builder lootContextBuilder = (new LootContext.Builder((ServerWorld)this.world))
                                .setRandom(this.world.random)
                                .put(LootContextParameters.POSITION, tmpPos)
                                .put(LootContextParameters.TOOL, ItemStack.EMPTY)
                                .putNullable(LootContextParameters.BLOCK_ENTITY, blockEntity)
                                .putNullable(LootContextParameters.THIS_ENTITY, this.exploder)
                                .put(LootContextParameters.EXPLOSION_RADIUS, this.power);

                        Block.dropStacks(state, lootContextBuilder);
                    }

                    this.world.setBlockState(tmpPos, Blocks.AIR.getDefaultState(), 3);
                    block.onDestroyedByExplosion(this.world, tmpPos, this);
                }
            }
        }
    }

    private void destroy(int x, int y, int z, boolean noDrop) {
        destroyUnchecked(x, y, z, noDrop);
    }

    private void destroyUnchecked(int x, int y, int z, boolean noDrop) {
        int index = ((z - this.areaZ) * this.areaSize + (x - this.areaX)) * 2;
        long[] array = this.destroyedBlockPositions[y];

        if (array == null) {
            array = makeArray(square(this.areaSize), 2);
            this.destroyedBlockPositions[y] = array;
        }

        if (noDrop) {
            setAtIndex(index, array, 3);
        } else {
            setAtIndex(index, array, 1);
        }
    }

    private void shootRay(double x, double y, double z, double phi, double theta, double power, boolean killEntities, BlockPos.Mutable tmpPos) {
        double deltaX = Math.sin(theta) * Math.cos(phi);
        double deltaY = Math.cos(theta);
        double deltaZ = Math.sin(theta) * Math.sin(phi);

        for (int step = 0;; step++) {
            int blockY = roundToNegInf(y);

            if ((blockY < 0) || (blockY >= this.mapHeight)) {
                break;
            }

            int blockX = roundToNegInf(x);
            int blockZ = roundToNegInf(z);

            tmpPos.set(blockX, blockY, blockZ);

            BlockState state = this.world.getBlockState(tmpPos);
            Block block = state.getBlock();
            double absorption = getAbsorption(block, tmpPos);

            if (absorption < 0.0D) {
                break;
            }

            if (absorption > 1000.0D) {
                absorption = 0.5D;
            } else {
                if (absorption > power) {
                    break;
                }

                if ((block == Blocks.STONE) || ((block != Blocks.AIR) && (!block.isAir(state)))) {
                    this.destroyUnchecked(blockX, blockY, blockZ, power > 8.0D);
                }
            }

            if (killEntities && ((step + 4) % 8 == 0) && !this.entitiesInRange.isEmpty() && (power >= 0.25D)) {
                this.damageEntities(x, y, z, step, power);
            }

            if (absorption > 10.0D) {
                for (int i = 0; i < 5; ++i) {
                    this.shootRay(x, y, z, this.random.nextDouble() * 2.0D * Math.PI,
                            this.random.nextDouble() * Math.PI, absorption * 0.4D, false, tmpPos);
                }
            }

            power -= absorption;

            x += deltaX;
            y += deltaY;
            z += deltaZ;
        }
    }

    private double getAbsorption(Block block, BlockPos pos) {
        double ret = 0.5D;

        if ((block == Blocks.AIR) || (block.isAir(block.getDefaultState()))) {
            return ret;
        }

        if (block == Blocks.BEDROCK) {
            ret += 800D;
            return ret;
        }

        if (block == Blocks.WATER) {
            ret += 1.0D;
        } else {
            float resistance = block.getBlastResistance();

            if (resistance < 0.0F) {
                return resistance;
            }
        }

        return ret;
    }

    private void damageEntities(double x, double y, double z, int step, double power) {
        int index;

        if (step != 4) {
            int distanceMin = square(step - 5);
            int indexStart = 0;
            int indexEnd = this.entitiesInRange.size() - 1;

            do {
                index = (indexStart + indexEnd) / 2;
                int distance = this.entitiesInRange.get(index).distance;

                if (distance < distanceMin) {
                    indexStart = index + 1;
                } else if (distance > distanceMin) {
                    indexEnd = index - 1;
                } else {
                    indexEnd = index;
                }
            } while (indexStart < indexEnd);
        } else {
            index = 0;
        }

        int distanceMax = square(step + 5);

        for (int i = index; i < this.entitiesInRange.size(); i++) {
            EntityDamage entityDamage = this.entitiesInRange.get(i);

            if (entityDamage.distance >= distanceMax) {
                continue;
            }

            Entity entity = entityDamage.entity;

            if (square(entity.getX() - x) + square(entity.getY() - y) + square(entity.getZ() - z) > 25.0D) {
                continue;
            }

            double damage = 4.0D * power;

            entityDamage.damage += damage;
            entityDamage.health -= damage;

            double deltaX = entity.getX() - this.explosionX;
            double deltaY = entity.getY() - this.explosionY;
            double deltaZ = entity.getZ() - this.explosionZ;

            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            double offset = 0.08749999999999999D;

            entityDamage.motionX += deltaX / distance * offset * power;
            entityDamage.motionY += deltaY / distance * offset * power;
            entityDamage.motionZ += deltaZ / distance * offset * power;

            if (entityDamage.health > 0.0D) {
                continue;
            }

            entity.damage(this.damageSource, (float) entityDamage.damage);

            if (entity.isAlive()) {
                continue;
            }

            this.entitiesInRange.remove(i);
            i--;
        }
    }

    private static double getEntityHealth(Entity entity) {
        if (entity instanceof ItemEntity) {
            return 5.0D;
        }

        return (1.0D / 0.0D);
    }

    private static long[] makeArray(int size, int step) {
        return new long[(size * step + 8 - step) / 8];
    }

    private static int nextSetIndex(int start, long[] array, int step) {
        int offset = start % 8;

        for (int i = start / 8; i < array.length; i++) {
            long l = array[i];

            for (int j = offset; j < 8; j += step) {
                int val = (int) (l >> j & (1 << step) - 1);

                if (val != 0) {
                    return i * 8 + j;
                }
            }

            offset = 0;
        }

        return -1;
    }

    private static int getAtIndex(int index, long[] array, int step) {
        return (int) (array[(index / 8)] >>> index % 8 & (1 << step) - 1);
    }

    private static void setAtIndex(int index, long[] array, int value) {
        array[(index / 8)] |= value << index % 8;
    }

    public static int roundToNegInf(double x) {
        int ret = (int) x;

        if (ret > x) {
            ret--;
        }

        return ret;
    }

    public static int square(int x) {
        return x * x;
    }

    public static double square(double x) {
        return x * x;
    }
}
