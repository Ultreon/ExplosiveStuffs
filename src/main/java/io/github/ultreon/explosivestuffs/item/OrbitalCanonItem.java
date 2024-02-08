package io.github.ultreon.explosivestuffs.item;

import io.github.ultreon.explosivestuffs.ExplosiveStuffs;
import io.github.ultreon.explosivestuffs.entity.OrbitalStrike;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import team.lodestar.lodestone.handlers.FireEffectHandler;
import team.lodestar.lodestone.systems.fireeffect.FireEffectInstance;

public class OrbitalCanonItem extends Item {
    public OrbitalCanonItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        player.getCooldowns().addCooldown(this, 5);

        BlockHitResult clip = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(128)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos location = clip.getBlockPos();

        if (!level.isClientSide) {
            OrbitalStrike beamStrike = new OrbitalStrike(level, location.getX(), location.getY(), location.getZ(), player);
            FireEffectHandler.setCustomFireInstance(beamStrike, new FireEffectInstance(ExplosiveStuffs.ORBITAL_STRIKE_EFFECT_TYPE));

            level.addFreshEntity(beamStrike);
        }

        return super.use(level, player, hand);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack p_41453_) {
        return true;
    }
}
