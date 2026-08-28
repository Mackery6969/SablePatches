package mackery.sablepatched.mixin.weather2;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class Weather2MixinPlugin implements IMixinConfigPlugin {

    private boolean weather2Present;

    @Override
    public void onLoad(final String mixinPackage) {
        this.weather2Present = classExists("weather2.weathersystem.storm.TornadoHelper");
    }

    private static boolean classExists(final String name) {
        try {
            Class.forName(name, false, Weather2MixinPlugin.class.getClassLoader());
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return this.weather2Present;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {
    }
}
