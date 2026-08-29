package mackery.sablepatched.mixin.weather2;

import net.minecraft.util.Mth;
import weather2.weathersystem.storm.StormObject;

public final class TornadoIntensity {
    private static final float MIN_FORCE_SCALE = 0.4F;
    private static final float MAX_FORCE_SCALE = 1.6F;

    private TornadoIntensity() {
    }

    public static float fraction(final StormObject storm) {
        return Mth.clamp(
                (storm.levelCurIntensityStage - StormObject.STATE_FORMING)
                        / (float) (StormObject.STATE_STAGE5 - StormObject.STATE_FORMING),
                0F, 1F
        );
    }

    public static float forceScale(final StormObject storm) {
        return MIN_FORCE_SCALE + (MAX_FORCE_SCALE - MIN_FORCE_SCALE) * fraction(storm);
    }

    public static double lerp(final StormObject storm, final double atMinIntensity, final double atMaxIntensity) {
        return atMinIntensity + (atMaxIntensity - atMinIntensity) * fraction(storm);
    }
}
