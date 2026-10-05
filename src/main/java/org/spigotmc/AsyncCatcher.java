package org.spigotmc;

import com.mohistmc.youer.threading.ThreadedServer;
import net.minecraft.server.MinecraftServer;

public class AsyncCatcher {

    public static boolean enabled = true;

    public static void catchOp(String reason) {
        if (AsyncCatcher.enabled && Thread.currentThread() != MinecraftServer.getServer().serverThread && !ThreadedServer.relaxedAsyncCatcher()) {
            MinecraftServer.LOGGER.error("Thread {} failed main thread check: {}", Thread.currentThread().getName(), reason, new Throwable()); // Paper
            throw new IllegalStateException("Asynchronous " + reason + "!");
        }
    }

    public static boolean catchAsync() {
        return Thread.currentThread() != MinecraftServer.getServer().serverThread && !ThreadedServer.relaxedAsyncCatcher();
    }
}
