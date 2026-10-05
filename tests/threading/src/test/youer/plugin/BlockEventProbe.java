package test.youer.plugin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.plugin.java.JavaPlugin;

final class BlockEventProbe implements Listener {
    private final World world;
    private final ServerLevel level;
    private final BlockPos first = new BlockPos(248, 68, 8);
    private final BlockPos second = new BlockPos(249, 68, 8);
    private final List<Integer> observed = new ArrayList<>();

    BlockEventProbe(JavaPlugin plugin, World world) {
        this.world = world;
        this.level = ((CraftWorld) world).getHandle();
        world.addPluginChunkTicket(15, 0, plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.level.setBlock(this.first, Blocks.NOTE_BLOCK.defaultBlockState(), 3);
        this.level.setBlock(this.second, Blocks.NOTE_BLOCK.defaultBlockState(), 3);
        this.level.blockEvent(this.first, Blocks.NOTE_BLOCK, 0, 0);
        this.level.blockEvent(this.first, Blocks.NOTE_BLOCK, 0, 0);
        this.level.blockEvent(this.second, Blocks.NOTE_BLOCK, 0, 0);
    }

    @EventHandler
    public void onNote(NotePlayEvent event) {
        var block = event.getBlock();
        if (block.getWorld() != this.world || block.getY() != 68 || block.getZ() != 8
            || block.getX() < 248 || block.getX() > 249) {
            return;
        }
        this.observed.add(block.getX());
        event.setCancelled(true);
        if (this.observed.size() == 1) {
            this.level.blockEvent(this.first, Blocks.NOTE_BLOCK, 0, 0);
        }
    }

    boolean passed() {
        return this.observed.equals(List.of(248, 249, 248));
    }

    int count() {
        return this.observed.size();
    }
}
