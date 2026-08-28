package mackery.sablepatched.mixin.skip_unobserved_sublevel_ticking;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import mackery.sablepatched.Config;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SubLevel.class)
public class SubLevelMixin {

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/plot/LevelPlot;tick()V"))
    private void sablepatched$skipUnobservedPlotTick(final LevelPlot plot) {
        if (!Config.SKIP_UNOBSERVED_SUBLEVEL_TICKING.get() || this.sablepatched$mustTick()) {
            plot.tick();
        }
    }

    @Unique
    private boolean sablepatched$mustTick() {
        if (!((Object) this instanceof ServerSubLevel serverSubLevel)) {
            return true;
        }

        if (!serverSubLevel.getTrackingPlayers().isEmpty()) {
            return true;
        }

        final ServerLevel level = serverSubLevel.getLevel();
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        return container != null && container.collectForceLoadTickets().containsKey(serverSubLevel);
    }
}
