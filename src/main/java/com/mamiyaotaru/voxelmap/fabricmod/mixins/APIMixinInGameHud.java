package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class APIMixinInGameHud {
    @Inject(method = "renderMainHud", at = @At("RETURN"))
    private void onRenderGameOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        FabricModVoxelMap.instance.renderOverlay(context);
    }
}
