package mackery.sablepatched.mixin.weather2.tornado_moves_sublevels;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import mackery.sablepatched.Config;
import mackery.sablepatched.mixin.weather2.TornadoIntensity;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.TornadoHelper;

@Mixin(TornadoHelper.class)
public class TornadoHelperMixin {

    @Shadow
    public StormObject storm;

    @Shadow
    public int grabDist;

    @Inject(method = "forceRotate(Lnet/minecraft/world/level/Level;Z)Z", at = @At("HEAD"))
    private void sablepatched$pullSubLevels(final Level parWorld, final boolean featherFallInstead,
                                             final CallbackInfoReturnable<Boolean> cir) {
        if (!Config.WEATHER2_TORNADOES_MOVE_SUBLEVELS.get() || parWorld.isClientSide()) {
            return;
        }

        final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(parWorld);
        final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(parWorld);

        if (physicsSystem == null || container == null) {
            return;
        }

        final double dist = this.storm.isPet() ? 3D : this.grabDist * 2D;
        final float intensityScale = Config.WEATHER2_SCALE_FORCE_BY_INTENSITY.get()
                ? TornadoIntensity.forceScale(this.storm)
                : 1F;

        for (final ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel.isRemoved()) {
                continue;
            }

            final BoundingBox3dc bounds = subLevel.boundingBox();
            final Vec3 center = new Vec3(
                    (bounds.minX() + bounds.maxX()) / 2.0,
                    (bounds.minY() + bounds.maxY()) / 2.0,
                    (bounds.minZ() + bounds.maxZ()) / 2.0
            );

            final double dx = this.storm.posBaseFormationPos.x - center.x;
            final double dz = this.storm.posBaseFormationPos.z - center.z;
            final double distXZ = Mth.sqrt((float) (dx * dx + dz * dz));

            if (distXZ >= dist) {
                continue;
            }

            final Vector3d currentVelocityJoml = subLevel.latestLinearVelocity;
            final Vec3 currentVelocity = new Vec3(currentVelocityJoml.x, currentVelocityJoml.y, currentVelocityJoml.z);

            final Vec3 targetVelocity = this.storm.spinObject(center, currentVelocity, false, 1F, 1F, true, 0F);
            final Vec3 deltaVelocity = targetVelocity.subtract(currentVelocity);

            if (deltaVelocity.lengthSqr() < 1.0E-6) {
                continue;
            }

            final double mass = subLevel.getMassTracker().getMass();
            if (mass <= 0.0) {
                continue;
            }

            final RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
            handle.applyImpulseAtPoint(
                    JOMLConversion.toJOML(center),
                    JOMLConversion.toJOML(deltaVelocity.scale(mass * intensityScale))
            );
        }
    }
}
