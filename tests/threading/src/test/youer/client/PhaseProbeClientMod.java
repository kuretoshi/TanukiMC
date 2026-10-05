package test.youer.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod("youer_phase_probe")
public final class PhaseProbeClientMod {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("youer_phase_probe");
    private static final DeferredBlock<Block> SCHEDULED_PROBE =
        BLOCKS.registerBlock("scheduled_probe", Block::new, BlockBehaviour.Properties.of().randomTicks());
    private static final DeferredBlock<ProbeBlock> BLOCK_ENTITY_PROBE =
        BLOCKS.registerBlock("block_entity_probe", ProbeBlock::new, BlockBehaviour.Properties.of());
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "youer_phase_probe");
    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProbeBlockEntity>> PROBE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("block_entity_probe",
            () -> BlockEntityType.Builder.of(ProbeBlockEntity::new, BLOCK_ENTITY_PROBE.get()).build(null));

    public PhaseProbeClientMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
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
    }

    private static final class ProbeBlockEntity extends BlockEntity {
        ProbeBlockEntity(BlockPos pos, BlockState state) {
            super(PROBE_BLOCK_ENTITY.get(), pos, state);
        }
    }
}
