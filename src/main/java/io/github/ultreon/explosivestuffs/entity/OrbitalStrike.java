package io.github.ultreon.explosivestuffs.entity;

import io.github.ultreon.explosivestuffs.Config;
import io.github.ultreon.explosivestuffs.ExplosiveStuffs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import team.lodestar.lodestone.handlers.ScreenshakeHandler;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.SimpleParticleOptions;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.screenshake.PositionedScreenshakeInstance;

import java.awt.*;

public class OrbitalStrike extends Entity {
    public static final EntityDataAccessor<Integer> DATA_BEAM_Y = SynchedEntityData.defineId(OrbitalStrike.class, EntityDataSerializers.INT);
    private Player trigger = null;
    private int remainingTicks = 10;
    private int radius = Config.IMPACT_RADIUS.get(); // Set the desired radius of the spherical area. DANGEROUS ON HIGH RADIUS
    private final boolean tunneling = Config.TUNNELING.get();
    private Color startingColor = new Color(100, 130, 255);
    private Color endingColor = new Color(60, 100, 200);
    private float beamSize = Config.RELATIVE_BEAM_SIZE.get().floatValue();
    private boolean stopMoving = false;
    private int soundTick;

    public OrbitalStrike(EntityType<OrbitalStrike> orbitalStrikeEntityType, Level level) {
        super(orbitalStrikeEntityType, level);
    }

    public OrbitalStrike(Level level, double x, double z, Player trigger) {
        this(ExplosiveStuffs.ORBITAL_STRIKE.get(), level);

        double y = level.getMaxBuildHeight();

        this.entityData.set(DATA_BEAM_Y, (int) y);

        this.trigger = trigger;

        this.setPos(x, y, z);
        this.setNoGravity(true);

        setBoundingBox(new AABB(
                -radius * Math.PI, -radius * Math.PI, -radius * Math.PI,
                radius * Math.PI, radius * Math.PI, radius * Math.PI));
    }

    @Override
    public void tick() {
        super.tick();

        int beamY = this.entityData.get(DATA_BEAM_Y);
        if (level().isClientSide) {
            this.spawnBeam(this.level(), this.position(), beamY);
            if (beamY <= position().y + radius * 2) {
                PositionedScreenshakeInstance instance = new PositionedScreenshakeInstance(Math.max(radius / 3, 2), new Vec3(position().x, beamY, position().z), radius * 2, radius * 10);
                ScreenshakeHandler.addScreenshake(instance);
            }
        }

        if (soundTick++ % 10 == 0) {
            for (int i = (int) getY(); i < level().getMaxBuildHeight(); i += 50) {
                this.level().playSound(null, this.position().x, i, this.position().z, ExplosiveStuffs.BEAM_SOUND.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
            }
        }

        if (!stopMoving)
            this.move(MoverType.SELF, new Vec3(0, -radius, 0));
        if (!onGround()) {
            return;
        }

        if (!level().isClientSide) {
            if (remainingTicks-- <= 0) this.discard();
            if (remainingTicks % 4 == 0 && tunneling) this.explode();
            if (remainingTicks == 5 && !tunneling) this.explode();
        } else {
            if (remainingTicks-- <= 0) this.discard();
            if (remainingTicks % 4 == 0 && tunneling) this.explodeClient();
            if (remainingTicks == 5 && !tunneling) this.explodeClient();
        }

        if (level().isClientSide) {
            spawnExplosion(this.level(), this.position());
        }
    }

    private void explodeClient() {
        PositionedScreenshakeInstance instance = new PositionedScreenshakeInstance(radius * 5, position(), radius * 2, radius * 10);
        ScreenshakeHandler.addScreenshake(instance);

        this.stopMoving = true;
    }

    private void explode() {
        // Remove blocks in a spherical area
        BlockPos centerPos = this.blockPosition(); // Get the center position of the explosion

        PositionedScreenshakeInstance instance = new PositionedScreenshakeInstance(radius * 5, position(), radius * 2, radius * 10);
        ScreenshakeHandler.addScreenshake(instance);

        this.stopMoving = true;

        // Iterate over the blocks within the spherical area
        for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-radius - 4, -radius - 4, -radius - 4), centerPos.offset(radius + 4, radius + 4, radius + 4))) {
            BlockState blockState = level().getBlockState(pos);
            if (blockState.getBlock().getSpeedFactor() < 0) continue;
            if (pos.distSqr(centerPos) <= radius * radius) {
                level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); // Destroy the block at the current position
            } else if (pos.distSqr(centerPos) <= (radius * radius) + 24 * radius / 10f) {
                transformBlocks(pos, blockState);
            }
        }

        level().getEntities(EntityTypeTest.forClass(Entity.class), new AABB(centerPos.offset(-radius * 2, -radius * 2, -radius * 2), centerPos.offset(radius * 2, radius * 2, radius * 2)), entity -> true).forEach(entity -> {
            double damage = 50f * radius * 2 * (radius * 2 - entity.position().distanceTo(centerPos.getCenter()));
            if (damage <= 0f) return;

            Registry<DamageType> damageTypes = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            ResourceKey<DamageType> damageKey = ExplosiveStuffs.ATOMIZED_DAMAGE;
            Holder.Reference<DamageType> damageType = damageTypes.getHolderOrThrow(damageKey);

            if (entity.position().distanceTo(centerPos.getCenter()) < radius) {
                DamageSource damageSource = new DamageSource(damageType, trigger);
                entity.hurt(damageSource, (float) damage);
            } else {
                DamageSource damageSource = new DamageSource(damageType, trigger);
                entity.hurt(damageSource, (float) damage / 2f);
            }
        });
    }

    private void transformBlocks(BlockPos pos, BlockState blockState) {
        if (blockState.is(BlockTags.SMELTS_TO_GLASS) || blockState.is(BlockTags.SAND) || blockState.is(Tags.Blocks.SANDSTONE))
            level().setBlockAndUpdate(pos, Blocks.GLASS.defaultBlockState());
        else if (blockState.isFlammable(level(), pos, Direction.UP))
            catchFire(pos, blockState);
        else
            annihilateSurroudingBlocks(pos);
    }

    private void annihilateSurroudingBlocks(BlockPos pos) {
        boolean b = random.nextInt(3) == 0;
        BlockState state = level().getBlockState(pos);
        if (state.isAir() || !level().getFluidState(pos).isEmpty()) return;
        level().setBlockAndUpdate(pos, b ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
    }

    private void catchFire(BlockPos pos, BlockState blockState) {
        blockState.getBlock().onCaughtFire(blockState, level(), pos, null, null);
        if (level().getBlockState(pos).equals(blockState)) {
            if (level().getBlockState(pos.above()).isAir())
                level().setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState());
            else level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    public void spawnExplosion(Level level, Vec3 pos) {
        WorldParticleBuilder.create(ExplosiveStuffs.CIRCLE_PARTICLE)
                .setScaleData(GenericParticleData.create(radius, radius, (float) (3 * radius) / 4).setEasing(Easing.LINEAR, Easing.SINE_OUT).setCoefficient(1f).build())
                .setTransparencyData(GenericParticleData.create(0.18f, 0.18f, 0f).setEasing(Easing.LINEAR, Easing.CIRC_OUT).build())
                .setColorData(ColorParticleData.create(startingColor, endingColor).setCoefficient(5f).setEasing(Easing.CIRC_IN_OUT).build())
                .setSpinData(SpinParticleData.create(0.0f, 0.0f).setSpinOffset((level.getGameTime() * 0.1f)).setEasing(Easing.QUARTIC_IN).build())
                .setRandomOffset(radius * 1.2f)
                .setLifetime(30)
                .addMotion(0, 0f, 0)
                .enableNoClip()
                .enableForcedSpawn()
                .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.NONE)
                .disableCull()
                .repeat(level, pos.x, pos.y, pos.z, (int) (5 * radius * 1.2f));
    }

    private void spawnBeam(Level level, Vec3 pos, int y) {
        WorldParticleBuilder builder = WorldParticleBuilder.create(ExplosiveStuffs.CIRCLE_PARTICLE)
                .setScaleData(GenericParticleData.create(radius * beamSize, radius * beamSize, 1 * (radius * beamSize) / 4).setEasing(Easing.LINEAR, Easing.SINE_OUT).setCoefficient(1f).build())
                .setTransparencyData(GenericParticleData.create(0.18f, 0.18f, 0f).setEasing(Easing.LINEAR, Easing.CIRC_OUT).build())
                .setColorData(ColorParticleData.create(startingColor, endingColor).setCoefficient(5f).setEasing(Easing.CIRC_IN_OUT).build())
                .setSpinData(SpinParticleData.create(0.0f, 0.0f).setSpinOffset((level.getGameTime() * 0.1f)).setEasing(Easing.QUARTIC_IN).build())
                .setRandomOffset(radius * beamSize, radius * beamSize * 3, radius * beamSize)
                .setLifetime(30)
                .addMotion(0, 0f, 0)
                .enableNoClip()
                .enableForcedSpawn()
                .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.NONE)
                .disableCull();

        for (int i = y; i > getY(); i--) {
            builder.repeat(level, pos.x, i, pos.z, 5 * radius);
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BEAM_Y, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        remainingTicks = compoundTag.getInt("ticks");
        this.entityData.set(DATA_BEAM_Y, compoundTag.getInt("beamY"));
        if (compoundTag.contains("stopMoving", Tag.TAG_BYTE)) {
            this.stopMoving = compoundTag.getBoolean("stopMoving");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("ticks", remainingTicks);
        compoundTag.putInt("beamY", this.entityData.get(DATA_BEAM_Y));
        compoundTag.putBoolean("stopMoving", stopMoving);
    }

    public void setRemainingTicks(int ticks) {
        remainingTicks = ticks;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public Player getTrigger() {
        return trigger;
    }

    public void setTrigger(Player trigger) {
        this.trigger = trigger;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public Color getStartingColor() {
        return startingColor;
    }

    public void setStartingColor(Color startingColor) {
        this.startingColor = startingColor;
    }

    public Color getEndingColor() {
        return endingColor;
    }

    public void setEndingColor(Color endingColor) {
        this.endingColor = endingColor;
    }

    public float getBeamSize() {
        return beamSize;
    }

    public void setBeamSize(float beamSize) {
        this.beamSize = beamSize;
    }

    public int getBeamY() {
        return this.entityData.get(DATA_BEAM_Y);
    }

    public void setBeamY(int beamY) {
        this.entityData.set(DATA_BEAM_Y, beamY);
    }
}
