package io.papermc.paper.threadedregions.scheduler;

import com.mohistmc.youer.threading.ThreadedRegionScheduler;
import com.mohistmc.youer.threading.ThreadedServer;
import java.util.function.Consumer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class FallbackRegionScheduler implements RegionScheduler {

    @Override
    public void execute(@NotNull final Plugin plugin, @NotNull final World world, final int chunkX, final int chunkZ, @NotNull final Runnable run) {
        if (ThreadedServer.enabled()) {
            ThreadedRegionScheduler.get().execute(plugin, world, chunkX, chunkZ, task -> run.run(), 0L, -1L);
            return;
        }
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, run);
    }

    @Override
    public @NotNull ScheduledTask run(@NotNull final Plugin plugin, @NotNull final World world, final int chunkX, final int chunkZ, @NotNull final Consumer<ScheduledTask> task) {
        if (ThreadedServer.enabled()) {
            return ThreadedRegionScheduler.get().execute(plugin, world, chunkX, chunkZ, task, 1L, -1L);
        }
        return plugin.getServer().getGlobalRegionScheduler().run(plugin, task);
    }

    @Override
    public @NotNull ScheduledTask runDelayed(@NotNull final Plugin plugin, @NotNull final World world, final int chunkX, final int chunkZ, @NotNull final Consumer<ScheduledTask> task, final long delayTicks) {
        if (ThreadedServer.enabled()) {
            if (delayTicks <= 0L) {
                throw new IllegalArgumentException("Delay ticks may not be <= 0");
            }
            return ThreadedRegionScheduler.get().execute(plugin, world, chunkX, chunkZ, task, delayTicks, -1L);
        }
        return plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task, delayTicks);
    }

    @Override
    public @NotNull ScheduledTask runAtFixedRate(@NotNull final Plugin plugin, @NotNull final World world, final int chunkX, final int chunkZ, @NotNull final Consumer<ScheduledTask> task, final long initialDelayTicks, final long periodTicks) {
        if (ThreadedServer.enabled()) {
            if (initialDelayTicks <= 0L) {
                throw new IllegalArgumentException("Initial delay ticks may not be <= 0");
            }
            if (periodTicks <= 0L) {
                throw new IllegalArgumentException("Period ticks may not be <= 0");
            }
            return ThreadedRegionScheduler.get().execute(plugin, world, chunkX, chunkZ, task, initialDelayTicks, periodTicks);
        }
        return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
    }
}
