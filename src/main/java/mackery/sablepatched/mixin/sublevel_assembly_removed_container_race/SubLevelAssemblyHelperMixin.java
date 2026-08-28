package mackery.sablepatched.mixin.sublevel_assembly_removed_container_race;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mackery.sablepatched.Config;
import mackery.sablepatched.SablePatched;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SubLevelAssemblyHelper.class)
public class SubLevelAssemblyHelperMixin {

    @ModifyVariable(method = "assembleBlocks", at = @At("STORE"), ordinal = 0)
    private static SubLevel sablepatched$treatRemovedContainerAsNone(final SubLevel containingSubLevel) {
        if (Config.FIX_SUBLEVEL_ASSEMBLY_REMOVED_CONTAINER_RACE.get()
                && containingSubLevel != null
                && containingSubLevel.isRemoved()) {
            SablePatched.LOGGER.debug(
                    "Sable tried to assemble a split-off sub-level inside an already-removed containing "
                            + "sub-level ({}); treating it as uncontained instead of crashing the server "
                            + "(ryanhcode/sable#1344).",
                    containingSubLevel.getUniqueId());
            return null;
        }

        return containingSubLevel;
    }
}
