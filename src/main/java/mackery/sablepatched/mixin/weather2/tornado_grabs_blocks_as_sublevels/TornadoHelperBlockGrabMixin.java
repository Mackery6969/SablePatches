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

import java.util.List;
import java.util.Objects;

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
                || !(parWorld instanceof final ServerLevel serverLevel)
                || serverLevel.getBlockEntity(pos) != null) {
            manager.syncBlockParticleNew(pos, state, owner);
            return;
        }

        final double mass = PhysicsBlockPropertyHelper.getMass(serverLevel, pos, state);
        if (mass <= 0.0 || mass > Config.WEATHER2_GRABBED_BLOCK_MAX_MASS.get()) {
            manager.syncBlockParticleNew(pos, state, owner);
            return;
        }

        final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(serverLevel);
        final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(serverLevel);

        if (physicsSystem == null || container == null) {
            manager.syncBlockParticleNew(pos, state, owner);
            return;
        }

        if (TornadoDebris.countActive(container) >= Config.WEATHER2_MAX_CONCURRENT_TORNADO_DEBRIS.get()) {
            manager.syncBlockParticleNew(pos, state, owner);
            return;
        }

        final BoundingBox3i bounds = Objects.requireNonNull(BoundingBox3i.from(List.of(pos))).expand(1, 1, 1);
        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(serverLevel, pos, List.of(pos), bounds);

        if (subLevel.isRemoved()) {
            return;
        }

        final CompoundTag marker = new CompoundTag();
        marker.putBoolean(TornadoDebris.TAG, true);
        subLevel.setUserDataTag(marker);

        final Vec3 center = Vec3.atCenterOf(pos);
        final Vec3 initialVelocity = this.storm.spinObject(center, Vec3.ZERO, false, 1F, 1F, true, 0F);

        if (initialVelocity.lengthSqr() > 1.0E-6) {
            physicsSystem.getPhysicsHandle(subLevel).applyImpulseAtPoint(
                    JOMLConversion.toJOML(center),
                    JOMLConversion.toJOML(initialVelocity.scale(mass))
            );
        }
    }
}
