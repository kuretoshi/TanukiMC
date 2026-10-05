package test.youer;

import com.mohistmc.youer.threading.ThreadedRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public final class RegionTaskQueueProbe {
    public static void main(String[] args) throws Exception {
        Plugin plugin = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class},
            (proxy, method, values) -> {
                if (method.getName().equals("isEnabled")) {
                    return true;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] {World.class},
            (proxy, method, values) -> {
                if (method.getName().equals("getUID")) {
                    return new UUID(0L, 1L);
                }
                throw new UnsupportedOperationException(method.getName());
            });
        ThreadedRegionScheduler scheduler = ThreadedRegionScheduler.get();
        Field deadlinesField = ThreadedRegionScheduler.class.getDeclaredField("tasksByDeadline");
        deadlinesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Long2ObjectOpenHashMap<List<ScheduledTask>> deadlines =
            (Long2ObjectOpenHashMap<List<ScheduledTask>>) deadlinesField.get(scheduler);
        try {
            ScheduledTask first = scheduler.execute(plugin, world, 0, 0, ignored -> {}, 1_000_000L, -1L);
            ScheduledTask second = scheduler.execute(plugin, world, 0, 0, ignored -> {}, 1_000_000L, -1L);
            ScheduledTask later = scheduler.execute(plugin, world, 0, 0, ignored -> {}, 2_000_000L, -1L);
            require(deadlines.size() == 2 && deadlines.get(1_000_000L).size() == 2, "Tasks must share a deadline");
            require(first.cancel() == ScheduledTask.CancelledState.CANCELLED_BY_CALLER, "First cancellation must succeed");
            require(deadlines.get(1_000_000L).equals(List.of(second)), "Cancellation must preserve the other task");
            second.cancel();
            require(!deadlines.containsKey(1_000_000L), "An empty deadline must be removed immediately");
            require(deadlines.get(2_000_000L).equals(List.of(later)), "Other deadlines must remain intact");
            later.cancel();
            require(deadlines.isEmpty(), "Cancelled delayed tasks must not accumulate");
            System.out.println("REGION_TASK_QUEUE_PROBE PASS cancellation deadline cleanup");
        } finally {
            ThreadedRegionScheduler.shutdown();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
