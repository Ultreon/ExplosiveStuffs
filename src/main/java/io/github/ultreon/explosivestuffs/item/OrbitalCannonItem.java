package io.github.ultreon.explosivestuffs.item;

import io.github.ultreon.explosivestuffs.Config;
import io.github.ultreon.explosivestuffs.ExplosiveStuffs;
import io.github.ultreon.explosivestuffs.entity.OrbitalStrike;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.handlers.FireEffectHandler;
import team.lodestar.lodestone.systems.fireeffect.FireEffectInstance;

public class OrbitalCannonItem extends Item {
    public OrbitalCannonItem() {
        super(new Properties().durability(20));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        player.getCooldowns().addCooldown(this, Config.COOLDOWN.get());

        BlockHitResult clip = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(Config.STRIKE_RANGE.get())), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos location = clip.getBlockPos();

        ItemStack itemInHand = player.getItemInHand(hand);
        if (!level.isClientSide) {
            InteractionResultHolder<ItemStack> itemInHand1 = OrbitalCannonItem.handleDamage(player, itemInHand);
            if (itemInHand1 != null) return itemInHand1;

            OrbitalCannonItem.target(level, player, location);
        } else {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), ExplosiveStuffs.ORBITAL_CANNON_TARGET_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
        }

        return InteractionResultHolder.sidedSuccess(itemInHand, level.isClientSide());
    }

    private static void target(@NotNull Level level, Player player, BlockPos location) {
        OrbitalStrike beamStrike = new OrbitalStrike(level, location.getX(), location.getZ(), player);
        FireEffectHandler.setCustomFireInstance(beamStrike, new FireEffectInstance(ExplosiveStuffs.ORBITAL_STRIKE_EFFECT_TYPE));

        level.addFreshEntity(beamStrike);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), ExplosiveStuffs.ORBITAL_CANNON_TARGET_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Nullable
    private static InteractionResultHolder<ItemStack> handleDamage(Player player, ItemStack itemInHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (itemInHand.getDamageValue() == itemInHand.getMaxDamage()) {
                return InteractionResultHolder.fail(itemInHand);
            }
            itemInHand.hurt(1, player.getRandom(), serverPlayer);
        } return null;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack p_41453_) {
        return true;
    }
}
