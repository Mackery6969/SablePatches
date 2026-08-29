package mackery.sablepatched.mixin.weather2.tornado_grabs_blocks_as_sublevels;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class GlueDetection {
    private static final boolean CREATE_PRESENT =
            classExists("com.simibubi.create.content.contraptions.glue.SuperGlueEntity");
    private static final boolean SIMULATED_PRESENT =
            classExists("dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity");

    private GlueDetection() {
    }

    private static boolean classExists(final String name) {
        try {
            Class.forName(name, false, GlueDetection.class.getClassLoader());
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    static boolean isGlued(final Level level, final BlockPos pos, final Direction direction) {
        if (CREATE_PRESENT && isSuperGlued(level, pos, direction)) {
            return true;
        }

        return SIMULATED_PRESENT && isHoneyGlued(level, pos, direction);
    }

    private static boolean isSuperGlued(final Level level, final BlockPos pos, final Direction direction) {
        return SuperGlueEntity.isGlued(level, pos, direction, null);
    }

    private static boolean isHoneyGlued(final Level level, final BlockPos pos, final Direction direction) {
        final BlockPos target = pos.relative(direction);
        final AABB span = new AABB(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerOf(target)).inflate(16);

        for (final HoneyGlueEntity glue : level.getEntitiesOfClass(HoneyGlueEntity.class, span)) {
            if (glue.contains(pos) && glue.contains(target)) {
                return true;
            }
        }

        return false;
    }
}
