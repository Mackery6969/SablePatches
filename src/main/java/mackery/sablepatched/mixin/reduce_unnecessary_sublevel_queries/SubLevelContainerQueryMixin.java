package mackery.sablepatched.mixin.reduce_unnecessary_sublevel_queries;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mackery.sablepatched.Config;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SubLevelContainer.class)
public class SubLevelContainerQueryMixin {

    private static final double UNION_BOUNDS_PADDING = 4.0;

    @Shadow
    @Final
    private List<SubLevel> allSubLevels;

    @Unique
    private BoundingBox3d sablepatched$unionBounds;

    @Unique
    private boolean sablepatched$unionBoundsEmpty = true;

    @Inject(method = "tick", at = @At("TAIL"))
    private void sablepatched$refreshUnionBounds(final CallbackInfo ci) {
        if (!Config.FIX_UNNECESSARY_SUBLEVEL_QUERIES.get()) {
            return;
        }

        this.sablepatched$unionBoundsEmpty = true;

        for (final SubLevel subLevel : this.allSubLevels) {
            final BoundingBox3dc bounds = subLevel.boundingBox();

            if (this.sablepatched$unionBoundsEmpty) {
                if (this.sablepatched$unionBounds == null) {
                    this.sablepatched$unionBounds = new BoundingBox3d(bounds);
                } else {
                    this.sablepatched$unionBounds.set(bounds);
                }
                this.sablepatched$unionBoundsEmpty = false;
            } else {
                this.sablepatched$unionBounds.expandTo(bounds);
            }
        }

        if (!this.sablepatched$unionBoundsEmpty) {
            this.sablepatched$unionBounds.expand(UNION_BOUNDS_PADDING);
        }
    }

    @Inject(method = "queryIntersecting", at = @At("HEAD"), cancellable = true)
    private void sablepatched$skipQueryIfNoPossibleOverlap(final BoundingBox3dc bounds, final CallbackInfoReturnable<Iterable<SubLevel>> cir) {
        if (!Config.FIX_UNNECESSARY_SUBLEVEL_QUERIES.get()) {
            return;
        }

        if (this.sablepatched$unionBoundsEmpty || !bounds.intersects(this.sablepatched$unionBounds)) {
            cir.setReturnValue(List.of());
        }
    }
}
