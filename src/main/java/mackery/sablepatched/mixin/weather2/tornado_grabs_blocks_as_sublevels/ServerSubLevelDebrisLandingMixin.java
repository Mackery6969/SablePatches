package mackery.sablepatched.mixin.weather2.tornado_grabs_blocks_as_sublevels;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import mackery.sablepatched.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSubLevel.class)
public class ServerSubLevelDebrisLandingMixin {

    private static final int STABLE_TICKS_TO_SETTLE = 10;
    private static final double AT_REST_VELOCITY_SQ = 1.0E-4;

    @Unique
    private int sablepatched$stableTicksAtRest = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void sablepatched$settleDebrisOnLanding(final CallbackInfo ci) {
        if (!Config.WEATHER2_TORNADOES_GRAB_BLOCKS_AS_PHYSICS_OBJECTS.get()) {
            return;
        }

        final ServerSubLevel self = (ServerSubLevel) (Object) this;

        if (self.isRemoved()) {
            return;
        }

        if (!TornadoDebris.isDebris(self)) {
            return;
        }

        final boolean atRest = self.latestLinearVelocity.lengthSquared() < AT_REST_VELOCITY_SQ
                && self.latestAngularVelocity.lengthSquared() < AT_REST_VELOCITY_SQ;

        if (!atRest) {
            this.sablepatched$stableTicksAtRest = 0;
            return;
        }

        if (++this.sablepatched$stableTicksAtRest < STABLE_TICKS_TO_SETTLE) {
            return;
        }

        final ServerLevel level = self.getLevel();
        final BoundingBox3dc bounds = self.boundingBox();
        final BlockPos landingPos = BlockPos.containing(
                (bounds.minX() + bounds.maxX()) / 2.0,
                (bounds.minY() + bounds.maxY()) / 2.0,
                (bounds.minZ() + bounds.maxZ()) / 2.0
        );

        if (!level.getBlockState(landingPos).isAir()) {
            this.sablepatched$stableTicksAtRest = 0;
            return;
        }

        final BlockPos originalBlockPos = self.getPlot().getCenterBlock();
        final BlockState originalState = level.getBlockState(originalBlockPos);

        if (originalState.isAir()) {
            return;
        }

        if (Config.WEATHER2_FRAGILE_BLOCKS_BREAK_ON_LANDING.get() && isFragile(originalState)) {
            TornadoDebris.destroyWithEffects(level, landingPos, originalState);
        } else {
            level.setBlockAndUpdate(landingPos, originalState);
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null) {
            container.removeSubLevel(self, SubLevelRemovalReason.REMOVED);
        }
    }

    private static boolean isFragile(final BlockState state) {
        return ((BlockStateExtension) state).sable$getProperty(PhysicsBlockPropertyTypes.FRAGILE.get());
    }
}
