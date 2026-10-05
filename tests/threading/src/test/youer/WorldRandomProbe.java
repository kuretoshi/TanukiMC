package test.youer;

import com.mohistmc.youer.threading.SynchronizedWorldRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.RandomSource;

public final class WorldRandomProbe {
    public static void main(String[] args) throws Exception {
        for (long seed : new long[] {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            RandomSource expected = RandomSource.create(seed);
            RandomSource actual = new SynchronizedWorldRandom(seed);
            for (int i = 0; i < 1000; i++) {
                require(actual.nextInt() == expected.nextInt(), "Integer sequence changed");
                require(actual.nextInt(113) == expected.nextInt(113), "Bounded integer sequence changed");
                require(actual.nextInt(-10, 20) == expected.nextInt(-10, 20), "Ranged integer sequence changed");
                require(actual.nextIntBetweenInclusive(-10, 20) == expected.nextIntBetweenInclusive(-10, 20), "Inclusive range changed");
                require(actual.nextLong() == expected.nextLong(), "Long sequence changed");
                require(actual.nextBoolean() == expected.nextBoolean(), "Boolean sequence changed");
                require(Float.floatToIntBits(actual.nextFloat()) == Float.floatToIntBits(expected.nextFloat()), "Float sequence changed");
                require(bits(actual.nextDouble()) == bits(expected.nextDouble()), "Double sequence changed");
                require(bits(actual.nextGaussian()) == bits(expected.nextGaussian()), "Gaussian sequence changed");
                require(bits(actual.triangle(3, 5)) == bits(expected.triangle(3, 5)), "Triangle sequence changed");
                actual.consumeCount(3);
                expected.consumeCount(3);
                if (i % 31 == 0) {
                    actual.setSeed(seed + i);
                    expected.setSeed(seed + i);
                    require(bits(actual.nextGaussian()) == bits(expected.nextGaussian()), "Reseeding must reset Gaussian cache");
                    require(actual.fork().nextLong() == expected.fork().nextLong(), "Fork sequence changed");
                    var actualFactory = actual.forkPositional();
                    var expectedFactory = expected.forkPositional();
                    require(actualFactory.at(-1, 2, 3).nextLong() == expectedFactory.at(-1, 2, 3).nextLong(), "Positional fork changed");
                    require(actualFactory.fromHashOf("probe").nextLong() == expectedFactory.fromHashOf("probe").nextLong(), "Hashed fork changed");
                }
            }
        }
        verifyConcurrent(false);
        verifyConcurrent(true);
        System.out.println("WORLD_RANDOM_PROBE PASS seeded-parity reseed gaussian forks concurrent-long concurrent-gaussian");
    }

    private static void verifyConcurrent(boolean gaussian) throws Exception {
        long seed = 421L;
        int perWorker = 4096;
        RandomSource actual = new SynchronizedWorldRandom(seed);
        ConcurrentLinkedQueue<Long> results = new ConcurrentLinkedQueue<>();
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(4)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                futures.add(workers.submit(() -> {
                    ready.countDown();
                    try {
                        require(start.await(5, TimeUnit.SECONDS), "Workers did not start together");
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                    }
                    for (int i = 0; i < perWorker; i++) {
                        results.add(gaussian ? bits(actual.nextGaussian()) : actual.nextLong());
                    }
                }));
            }
            try {
                require(ready.await(5, TimeUnit.SECONDS), "Workers did not become ready");
            } finally {
                start.countDown();
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        }
        RandomSource serial = RandomSource.create(seed);
        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 4 * perWorker; i++) {
            expected.add(gaussian ? bits(serial.nextGaussian()) : serial.nextLong());
        }
        List<Long> observed = new ArrayList<>(results);
        Collections.sort(expected);
        Collections.sort(observed);
        require(observed.equals(expected), "Concurrent draws must equal one complete serial stream, without torn compound draws");
    }

    private static long bits(double value) {
        return Double.doubleToLongBits(value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
