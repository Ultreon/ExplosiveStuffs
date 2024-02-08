package io.github.ultreon.explosivestuffs.client.renderer;

import io.github.ultreon.explosivestuffs.entity.OrbitalStrike;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class OrbitalStrikeRenderer extends EntityRenderer<OrbitalStrike> {
    public OrbitalStrikeRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    @Override
    public boolean shouldRender(@NotNull OrbitalStrike p_114491_, @NotNull Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }

    @Override
    protected boolean shouldShowName(@NotNull OrbitalStrike p_114504_) {
        return false;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull OrbitalStrike beamStrike) {
        return new ResourceLocation("explosivestuffs", "textures/entity/orbital_strike.png");
    }
}
