package mackery.sablepatched.mixin.uncapped_sublevel_light_drain;

import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import mackery.sablepatched.Config;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevelPlot.class)
public class ServerLevelPlotMixin {

    @Unique
    private int sablepatched$lightDrainPassesThisTick = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void sablepatched$resetLightDrainCounter(final CallbackInfo ci) {
        this.sablepatched$lightDrainPassesThisTick = 0;
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;hasLightWork()Z"))
    private boolean sablepatched$capLightDrainPerTick(final LevelLightEngine lightEngine) {
        final boolean actuallyHasWork = lightEngine.hasLightWork();
        if (!Config.FIX_UNCAPPED_SUBLEVEL_LIGHT_DRAIN.get()) {
            return actuallyHasWork;
        }

        this.sablepatched$lightDrainPassesThisTick++;
        if (this.sablepatched$lightDrainPassesThisTick >= Config.MAX_LIGHT_DRAIN_PASSES_PER_TICK.get()) {
            return false;
        }

        return actuallyHasWork;
    }
}
