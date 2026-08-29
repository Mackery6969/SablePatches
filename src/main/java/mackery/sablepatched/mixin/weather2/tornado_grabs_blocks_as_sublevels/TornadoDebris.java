package mackery.sablepatched.mixin.weather2.tornado_grabs_blocks_as_sublevels;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class TornadoDebris {

    static final String TAG = "sablepatched_tornado_debris";

    private TornadoDebris() {
    }

    static boolean isDebris(final ServerSubLevel subLevel) {
        final CompoundTag tag = subLevel.getUserDataTag();
        return tag != null && tag.getBoolean(TAG);
    }

    static int countActive(final ServerSubLevelContainer container) {
        int count = 0;

        for (final ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (isDebris(subLevel)) {
                count++;
            }
        }

        return count;
    }

    static void destroyWithEffects(final ServerLevel level, final BlockPos pos, final BlockState state) {
        level.levelEvent(2001, pos, Block.getId(state));
        Block.dropResources(state, level, pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
