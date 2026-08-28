package mackery.sablepatched;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(SablePatched.MODID)
public class SablePatched {
    public static final String MODID = "sablepatched";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SablePatched(final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
