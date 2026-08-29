package mackery.sablepatched.mixin.weather2.tornado_grabs_blocks_as_sublevels;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import mackery.sablepatched.Config;
import mackery.sablepatched.mixin.weather2.TornadoIntensity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.TornadoHelper;
import weather2.weathersystem.storm.WeatherObject;

import java.util.Set;

@Mixin(TornadoHelper.class)
public class TornadoHelperBlockGrabMixin {

    @Shadow
    public StormObject storm;

    @Redirect(
            method = "tryRip",
            at = @At(value = "INVOKE",
                    target = "Lweather2/weathersystem/WeatherManagerServer;syncBlockParticleNew(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lweather2/weathersystem/storm/WeatherObject;)V"))
    private void sablepatched$grabAsSubLevelInstead(final WeatherManagerServer manager, final BlockPos pos, final BlockState state, final WeatherObject owner,
                                                      final Level parWorld, final int tryX, final int tryY, final int tryZ) {
        if (!Config.WEATHER2_TORNADOES_GRAB_BLOCKS_AS_PHYSICS_OBJECTS.get()
                || !(parWorld instanceof final ServerLevel serverLevel)) {
            manager.syncBlockParticleNew(pos, state, owner);
            return;
        }

        final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(serverLevel);
        final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(serverLevel);

        if (physicsSystem == null || container == null) {
            manager.syncBlockParticleNew(pos, state, owner);
            return;
        }

        final double effectiveMaxMass = Config.WEATHER2_SCALE_FORCE_BY_INTENSITY.get()
                ? TornadoIntensity.lerp(this.storm,
                        Config.WEATHER2_GRABBED_BLOCK_MAX_MASS_AT_MIN_INTENSITY.get(),
                        Config.WEATHER2_GRABBED_BLOCK_MAX_MASS_AT_MAX_INTENSITY.get())
                : Config.WEATHER2_GRABBED_BLOCK_MAX_MASS_AT_MAX_INTENSITY.get();

        final SubLevelAssemblyHelper.GatherResult gathered = SubLevelAssemblyHelper.gatherConnectedBlocks(
                pos, serverLevel, Config.WEATHER2_MAX_GLUED_CLUSTER_SIZE.get(),
                (originPos, originState, candidatePos, candidateState, directionFrom) -> directionFrom != null
                        && GlueDetection.isGlued(serverLevel, originPos, directionFrom));

        final Set<BlockPos> gluedGroup = gathered.assemblyState() == SubLevelAssemblyHelper.GatherResult.State.SUCCESS
                ? gathered.blocks()
                : Set.of(pos);

        for (final BlockPos p : gluedGroup) {
            if (serverLevel.getBlockEntity(p) != null) {
                manager.syncBlockParticleNew(pos, state, owner);
                return;
            }
        }

        double groupMass = 0.0;
        for (final BlockPos p : gluedGroup) {
            groupMass += PhysicsBlockPropertyHelper.getMass(serverLevel, p, serverLevel.getBlockState(p));
        }

        Set<BlockPos> blocksToGrab = gluedGroup;
        double grabbedMass = groupMass;

        if (groupMass <= 0.0 || groupMass > effectiveMaxMass) {
            if (gluedGroup.size() == 1) {
                // plain ineligible block, not glued to anything - unchanged from before
                manager.syncBlockParticleNew(pos, state, owner);
                return;
            }

            if (!Config.WEATHER2_ALLOW_SEPARATING_GLUED_BLOCKS.get()) {
                manager.syncBlockParticleNew(pos, state, owner);
                return;
            }

            final double soloMass = PhysicsBlockPropertyHelper.getMass(serverLevel, pos, state);
            final double resistedMass = soloMass * Config.WEATHER2_GLUED_SEPARATION_RESISTANCE.get();

            if (soloMass <= 0.0 || resistedMass > effectiveMaxMass) {
                manager.syncBlockParticleNew(pos, state, owner);
                return;
            }

            blocksToGrab = Set.of(pos);
            grabbedMass = soloMass;
        }

        if (TornadoDebris.countActive(container) >= Config.WEATHER2_MAX_CONCURRENT_TORNADO_DEBRIS.get()) {
            TornadoDebris.destroyWithEffects(serverLevel, pos, state);
            return;
        }

        final BoundingBox3i bounds = java.util.Objects.requireNonNull(BoundingBox3i.from(blocksToGrab)).expand(1, 1, 1);
        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(serverLevel, pos, blocksToGrab, bounds);

        if (subLevel.isRemoved()) {
            return;
        }

        final CompoundTag marker = new CompoundTag();
        marker.putBoolean(TornadoDebris.TAG, true);
        subLevel.setUserDataTag(marker);

        final Vec3 center = new Vec3(
                (bounds.minX() + bounds.maxX()) / 2.0,
                (bounds.minY() + bounds.maxY()) / 2.0,
                (bounds.minZ() + bounds.maxZ()) / 2.0);
        final Vec3 initialVelocity = this.storm.spinObject(center, Vec3.ZERO, false, 1F, 1F, true, 0F);

        if (initialVelocity.lengthSqr() > 1.0E-6) {
            final float intensityScale = Config.WEATHER2_SCALE_FORCE_BY_INTENSITY.get()
                    ? TornadoIntensity.forceScale(this.storm)
                    : 1F;

            physicsSystem.getPhysicsHandle(subLevel).applyImpulseAtPoint(
                    JOMLConversion.toJOML(center),
                    JOMLConversion.toJOML(initialVelocity.scale(grabbedMass * intensityScale))
            );
        }
    }
}
