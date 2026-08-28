package mackery.sablepatched.mixin.redundant_mass_merge;

import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.api.physics.mass.MergedMassTracker;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import mackery.sablepatched.Config;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MergedMassTracker.class)
public class MergedMassTrackerMixin {

    @Shadow
    @Final
    private MassTracker selfTracker;

    @Shadow
    @Final
    private ServerSubLevel subLevel;

    @Unique
    private double sablepatched$lastSelfMass = Double.NaN;

    @Unique
    private double sablepatched$lastSelfComX = Double.NaN;

    @Unique
    private double sablepatched$lastSelfComY = Double.NaN;

    @Unique
    private double sablepatched$lastSelfComZ = Double.NaN;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void sablepatched$skipRedundantMassMerge(final float partialPhysicsTick, final CallbackInfo ci) {
        if (!Config.FIX_REDUNDANT_MASS_MERGE.get()) {
            return;
        }

        final Vector3dc selfCenterOfMass = this.selfTracker.getCenterOfMass();

        if (selfCenterOfMass == null) {
            return;
        }

        final double mass = this.selfTracker.getMass();

        final boolean unchanged = this.subLevel.getPlot().getContraptions().isEmpty()
                && mass == this.sablepatched$lastSelfMass
                && selfCenterOfMass.x() == this.sablepatched$lastSelfComX
                && selfCenterOfMass.y() == this.sablepatched$lastSelfComY
                && selfCenterOfMass.z() == this.sablepatched$lastSelfComZ;

        this.sablepatched$lastSelfMass = mass;
        this.sablepatched$lastSelfComX = selfCenterOfMass.x();
        this.sablepatched$lastSelfComY = selfCenterOfMass.y();
        this.sablepatched$lastSelfComZ = selfCenterOfMass.z();

        if (unchanged) {
            ci.cancel();
        }
    }
}
