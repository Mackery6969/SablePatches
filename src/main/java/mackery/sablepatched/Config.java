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

        public static final ModConfigSpec.BooleanValue WEATHER2_TORNADOES_GRAB_BLOCKS_AS_PHYSICS_OBJECTS = BUILDER
                        .comment(
                                        "Weather2 compat, optional/experimental (no-op if Weather2 isn't installed). Weather2's",
                                        "tornadoes currently grab individual blocks by silently queuing them to become air ~40",
                                        "ticks later and showing a purely cosmetic client-side particle in the meantime - the block",
                                        "has no collision, no real motion, and can't land anywhere; the old EntityMovingBlock that",
                                        "used to back this was never ported. When enabled, an eligible grabbed block instead",
                                        "becomes a real 1-block Sable sub-level (skipping the fake particle), reusing the exact",
                                        "same spinObject()-derived impulse as weather2TornadoesMoveSubLevels above so heavier blocks",
                                        "resist being flung more than light ones. It's tagged so only tornado debris (never a real",
                                        "player structure) auto-settles back into a normal placed block once it comes to rest -",
                                        "see weather2GrabbedBlockMaxMass below for the eligibility cutoff. Blocks with a block",
                                        "entity (chests, etc.) are always left to the vanilla path, to avoid losing their contents.",
                                        "Off by default: unlike the patches above, this creates new sub-levels (real ongoing tick",
                                        "cost - see fixRedundantMassMerge/skipUnobservedSubLevelTicking above) rather than removing",
                                        "unnecessary work, so it's a deliberate trade of some overhead for a real gameplay effect.")
                        .define("weather2TornadoesGrabBlocksAsPhysicsObjects", false);

        public static final ModConfigSpec.DoubleValue WEATHER2_GRABBED_BLOCK_MAX_MASS = BUILDER
                        .comment(
                                        "Only used when weather2TornadoesGrabBlocksAsPhysicsObjects is enabled. Blocks heavier than",
                                        "this (Sable's PhysicsBlockPropertyHelper.getMass(), in Sable's own mass units - data-driven",
                                        "per block type, check a few of your pack's block masses with Sable's own tooling before",
                                        "tuning this) are left to Weather2's normal (cosmetic-only) grab instead of becoming a",
                                        "physics object, so a tornado can't casually fling something absurdly heavy.")
                        .defineInRange("weather2GrabbedBlockMaxMass", 100.0, 0.0, Double.MAX_VALUE);

        public static final ModConfigSpec.IntValue WEATHER2_MAX_CONCURRENT_TORNADO_DEBRIS = BUILDER
                        .comment(
                                        "Only used when weather2TornadoesGrabBlocksAsPhysicsObjects is enabled. A strong tornado can",
                                        "grab several blocks per tick, and each one becomes a real sub-level - its own plot, light",
                                        "engine, mass tracker, and rigid body - not a free particle, so nothing should let that grow",
                                        "unbounded. Per level (dimension), once this many tornado-debris sub-levels are alive at",
                                        "once, any further grabbed blocks fall back to Weather2's normal cosmetic-only grab (same as",
                                        "an ineligible block) until some existing debris lands and clears out. The count is read",
                                        "live off the sub-level container itself rather than kept in a running tally, specifically",
                                        "so it can't drift out of sync and get stuck refusing new debris forever.")
                        .defineInRange("weather2MaxConcurrentTornadoDebris", 24, 0, Integer.MAX_VALUE);

        public static final ModConfigSpec.BooleanValue FIX_UNNECESSARY_SUBLEVEL_QUERIES = BUILDER
                        .comment(
                                        "Sable's SubLevelPhysicsSystem.queryIntersecting() has a spatial ticket-based index built for",
                                        "exactly this (SubLevelPhysicsSystem.USE_TICKETS_FOR_QUERIES), but it's hardcoded off in",
                                        "Sable itself - it throws if used, meaning the resident-tracking behind it isn't trusted by",
                                        "its own author yet. Forcing it on ourselves would risk entities silently failing to collide",
                                        "with sub-levels if that tracking has gaps, which is a far worse bug than the CPU it'd save,",
                                        "so this patch does not do that. Instead, every query (called from many places per entity",
                                        "per tick - fluid checks, block-inside checks, getOnPos, collision) currently falls back to",
                                        "a brute-force scan of every loaded sub-level in the level, even when none of them are",
                                        "anywhere near the entity asking. This adds one cheap, provably-safe pre-check: once per",
                                        "tick, the bounding boxes of every loaded sub-level are unioned into one padded envelope; a",
                                        "query outside that envelope returns empty immediately without touching the sub-level list",
                                        "at all, and a query that does overlap it falls through to the exact same brute-force scan",
                                        "as before. The envelope is refreshed once per tick (not live per-query), padded a few",
                                        "blocks to absorb a tick's worth of ordinary movement, so a fast-moving sub-level can't slip",
                                        "past it - the same last-tick-position broad-phase trade-off any spatial acceleration",
                                        "structure in a physics engine makes.")
                        .define("fixUnnecessarySubLevelQueries", true);

        public static final ModConfigSpec SPEC = BUILDER.build();
}
