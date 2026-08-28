package mackery.sablepatched;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue FIX_REDUNDANT_MASS_MERGE = BUILDER
                        .comment(
                                        "Skips Sable's per-physics-substep merged mass/inertia recomputation for sub-levels with no",
                                        "attached contraptions whose mass hasn't changed since the last substep (i.e. nothing was",
                                        "placed/broken on them, and nothing is attached that could be moving independently).",
                                        "The result is provably identical when skipped, not an approximation - it just avoids",
                                        "redundant allocations and a 3x3 matrix inversion for every stationary sub-level, every",
                                        "substep, forever.")
                        .define("fixRedundantMassMerge", true);

        public static final ModConfigSpec.BooleanValue FIX_SUBLEVEL_ASSEMBLY_REMOVED_CONTAINER_RACE = BUILDER
                        .comment(
                                        "Fixes a server crash (and permanent loss of the sub-level being split) when Sable's sub-level",
                                        "splitting races against another sub-level being removed earlier in the same tick.",
                                        "See https://github.com/ryanhcode/sable/issues/1344")
                        .define("fixSubLevelAssemblyRemovedContainerRace", true);

        public static final ModConfigSpec.BooleanValue SKIP_UNOBSERVED_SUBLEVEL_TICKING = BUILDER
                        .comment(
                                        "Skips a sub-level's per-tick light-update and contraption-validity work (LevelPlot.tick())",
                                        "when no player is tracking it and it has no active force-load ticket. Physics simulation is",
                                        "untouched - only light propagation and contraption cleanup are deferred until the sub-level",
                                        "is observed or force-loaded again, at which point they resume from where they left off.",
                                        "Unlike the other patches above this is a real behavior tradeoff, not a provably-identical",
                                        "optimization - measured at ~5.8% of total server tick time in a loaded test world (see spark",
                                        "profile from 2026-08-28), so worth it, but disable if unobserved sub-levels ever need to stay",
                                        "fully simulated (e.g. lighting must be instantly correct the moment a player reappears).")
                        .define("skipUnobservedSubLevelTicking", true);

        public static final ModConfigSpec.BooleanValue FIX_UNCAPPED_SUBLEVEL_LIGHT_DRAIN = BUILDER
                        .comment(
                                        "Sable's ServerLevelPlot.tick() drains its entire light-update backlog in a single tick",
                                        "(a `do { runLightUpdates() } while (hasLightWork())` loop with no cap), which can stall the",
                                        "server thread for one tick if a sub-level accumulates a large light-update backlog (e.g.",
                                        "after bulk block changes). This caps how many drain passes happen per tick, spreading the",
                                        "rest across subsequent ticks instead - same eventual result, just paced like vanilla's own",
                                        "light engine instead of draining unbounded.")
                        .define("fixUncappedSubLevelLightDrain", true);

        public static final ModConfigSpec.IntValue MAX_LIGHT_DRAIN_PASSES_PER_TICK = BUILDER
                        .comment("Only used when fixUncappedSubLevelLightDrain is enabled.")
                        .defineInRange("maxLightDrainPassesPerTick", 8, 1, Integer.MAX_VALUE);

        public static final ModConfigSpec.BooleanValue WEATHER2_TORNADOES_MOVE_SUBLEVELS = BUILDER
                        .comment(
                                        "Weather2 compat (no-op if Weather2 isn't installed). Weather2's tornadoes grab every",
                                        "vanilla Entity in their reach and spin/toss it via StormObject.spinObject() - but a Sable",
                                        "sub-level isn't an Entity, so tornadoes currently ignore parked airships and other",
                                        "structures entirely. This mirrors the same grab reach and reuses spinObject() itself",
                                        "(passing forCube=true, which Weather2 already special-cases for large/blocky objects) to",
                                        "compute a target velocity per sub-level, then applies the difference as a real point-",
                                        "impulse on its rigid body (scaled by its actual mass) instead of teleporting its",
                                        "velocity outright, so a lopsided structure can be spun, not just shoved.")
                        .define("weather2TornadoesMoveSubLevels", true);

        public static final ModConfigSpec SPEC = BUILDER.build();
}
