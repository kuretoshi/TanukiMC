package ca.spottedleaf.moonrise.common.util;

public class TickThread extends Thread {

    public TickThread(Runnable target, String name) {
        super(target, name);
    }

    public TickThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public static boolean isTickThread() {
        return Thread.currentThread() instanceof TickThread;
    }
}
