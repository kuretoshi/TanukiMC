package test.youer.mod;

import com.mojang.serialization.MapCodec;
import java.util.concurrent.atomic.AtomicInteger;
import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod("youer_phase_probe")
public final class PhaseProbeMod {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("youer_phase_probe");
    private static final DeferredBlock<ScheduledProbeBlock> SCHEDULED_PROBE =
        BLOCKS.registerBlock("scheduled_probe", ScheduledProbeBlock::new, BlockBehaviour.Properties.of().randomTicks());
    private static final DeferredBlock<ProbeBlock> BLOCK_ENTITY_PROBE =
        BLOCKS.registerBlock("block_entity_probe", ProbeBlock::new, BlockBehaviour.Properties.of());
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "youer_phase_probe");
    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProbeBlockEntity>> PROBE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("block_entity_probe",
            () -> BlockEntityType.Builder.of(ProbeBlockEntity::new, BLOCK_ENTITY_PROBE.get()).build(null));

    public PhaseProbeMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(this::beforeLevel);
        NeoForge.EVENT_BUS.addListener(this::afterLevel);
        NeoForge.EVENT_BUS.addListener(this::beforeEntity);
    }

    private void beforeLevel(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide()) {
            checkOverlap();
            counter("worldDepth").incrementAndGet();
        }
    }

    private void afterLevel(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) {
            checkOverlap();
            counter("worldDepth").decrementAndGet();
            counter("worldTicks").incrementAndGet();
        }
    }

    private void checkOverlap() {
        if (counter("active").get() != 0) {
            counter("overlaps").incrementAndGet();
        }
    }

    private void beforeEntity(EntityTickEvent.Pre event) {
        var entity = event.getEntity();
        if (entity instanceof net.minecraft.server.level.ServerPlayer player
            && player.getGameProfile().getName().startsWith("ThreadProbe")) {
            counter("playerTicks").incrementAndGet();
            if (ThreadedServer.isRegionThread()) {
                counter("playerFailures").incrementAndGet();
            }
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)
            || entity.getType() != net.minecraft.world.entity.EntityType.ARMOR_STAND
            || entity.getY() != 112.0
            || !entity.getTags().contains("tanukimc_phase_probe_current")) {
            return;
        }
        int chunkX = entity.chunkPosition().x;
        int group = switch (chunkX) {
            case 15, 16 -> 15;
            case 128, 129 -> 128;
            case 256, 257 -> 256;
            case 384, 385 -> 384;
            default -> -1;
        };
        if (group < 0) {
            return;
        }
        int active = counter("entityActive").incrementAndGet();
        counter("entityMax").accumulateAndGet(active, Math::max);
        try {
            if (!ThreadedServer.isRegionThread() || !ThreadedServer.owns(level.getWorld(), chunkX, 0)) {
                counter("entityFailures").incrementAndGet();
            }
            counter("entityChunk" + group).incrementAndGet();
            try {
                Thread.sleep(20L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                counter("entityFailures").incrementAndGet();
            }
        } finally {
            counter("entityActive").decrementAndGet();
        }
    }

    private static AtomicInteger counter(String name) {
        // Bootstrap types share the probes across the MOD and plugin class loaders.
        return (AtomicInteger) System.getProperties().computeIfAbsent("youer.phase." + name, key -> new AtomicInteger());
    }

    private static final class ScheduledProbeBlock extends Block {
        ScheduledProbeBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int active = counter("scheduledActive").incrementAndGet();
            counter("scheduledMax").accumulateAndGet(active, Math::max);
            try {
                if (!ThreadedServer.isRegionThread()
                    || !ThreadedServer.owns(level.getWorld(), pos.getX() >> 4, pos.getZ() >> 4)) {
                    counter("scheduledFailures").incrementAndGet();
                }
                counter("scheduledTicks").incrementAndGet();
                counter("scheduledChunk" + (pos.getX() >> 4)).incrementAndGet();
                BlockState captured = (pos.getX() >> 4) == 15
                    ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
                level.captureNextSetBlock();
                level.callEvent(true);
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    counter("scheduledFailures").incrementAndGet();
                }
                if (level.setBlockAndUpdate(pos, captured) || level.blockFormState() != captured) {
                    counter("scheduledFailures").incrementAndGet();
                }
                level.setBlock(pos, captured, 3);
                if (level.capturedSetBlockState() != captured || level.getBlockState(pos) != state) {
                    counter("scheduledFailures").incrementAndGet();
                }
            } finally {
                counter("scheduledActive").decrementAndGet();
            }
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (pos.getY() < 96 || pos.getY() > 99) {
                return;
            }
            int chunkX = pos.getX() >> 4;
            if (chunkX != 15 && chunkX != 128 && chunkX != 256 && chunkX != 384) {
                return;
            }
            int active = counter("randomActive").incrementAndGet();
            counter("randomMax").accumulateAndGet(active, Math::max);
            try {
                if (!ThreadedServer.isRegionThread() || !ThreadedServer.owns(level.getWorld(), chunkX, 0)) {
                    counter("randomFailures").incrementAndGet();
                }
                counter("randomChunk" + chunkX).incrementAndGet();
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    counter("randomFailures").incrementAndGet();
                }
            } finally {
                counter("randomActive").decrementAndGet();
            }
        }
    }

    private static final class ProbeBlock extends BaseEntityBlock {
        private static final MapCodec<ProbeBlock> CODEC = simpleCodec(ProbeBlock::new);

        ProbeBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<ProbeBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ProbeBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, PROBE_BLOCK_ENTITY.get(), ProbeBlockEntity::tick);
        }
    }

    private static final class ProbeBlockEntity extends BlockEntity {
        ProbeBlockEntity(BlockPos pos, BlockState state) {
            super(PROBE_BLOCK_ENTITY.get(), pos, state);
        }

        private static void tick(Level level, BlockPos pos, BlockState state, ProbeBlockEntity blockEntity) {
            int chunkX = pos.getX() >> 4;
            int active = counter("beActive").incrementAndGet();
            counter("beMax").accumulateAndGet(active, Math::max);
            try {
                if (!ThreadedServer.isRegionThread() || !ThreadedServer.owns(level.getWorld(), chunkX, 0)) {
                    counter("beFailures").incrementAndGet();
                }
                counter("beChunk" + chunkX).incrementAndGet();
                try {
                    Thread.sleep(20L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    counter("beFailures").incrementAndGet();
                }
            } finally {
                counter("beActive").decrementAndGet();
            }
        }
    }
}
