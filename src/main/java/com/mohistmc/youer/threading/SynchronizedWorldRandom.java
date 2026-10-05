package com.mohistmc.youer.threading;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomSupport;

/** Protects compound draws and the Gaussian cache with one world-local monitor. */
public final class SynchronizedWorldRandom implements RandomSource {
    private final RandomSource delegate;

    public SynchronizedWorldRandom() {
        this(RandomSupport.generateUniqueSeed());
    }

    public SynchronizedWorldRandom(long seed) {
        this(RandomSource.create(seed));
    }

    private SynchronizedWorldRandom(RandomSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized RandomSource fork() {
        return new SynchronizedWorldRandom(this.delegate.fork());
    }

    @Override
    public synchronized PositionalRandomFactory forkPositional() {
        return this.delegate.forkPositional();
    }

    @Override
    public synchronized void setSeed(long seed) {
        this.delegate.setSeed(seed);
    }

    @Override
    public synchronized int nextInt() {
        return this.delegate.nextInt();
    }

    @Override
    public synchronized int nextInt(int bound) {
        return this.delegate.nextInt(bound);
    }

    @Override
    public synchronized int nextInt(int origin, int bound) {
        return this.delegate.nextInt(origin, bound);
    }

    @Override
    public synchronized int nextIntBetweenInclusive(int min, int max) {
        return this.delegate.nextIntBetweenInclusive(min, max);
    }

    @Override
    public synchronized long nextLong() {
        return this.delegate.nextLong();
    }

    @Override
    public synchronized boolean nextBoolean() {
        return this.delegate.nextBoolean();
    }

    @Override
    public synchronized float nextFloat() {
        return this.delegate.nextFloat();
    }

    @Override
    public synchronized double nextDouble() {
        return this.delegate.nextDouble();
    }

    @Override
    public synchronized double nextGaussian() {
        return this.delegate.nextGaussian();
    }

    @Override
    public synchronized double triangle(double mean, double deviation) {
        return this.delegate.triangle(mean, deviation);
    }

    @Override
    public synchronized void consumeCount(int count) {
        this.delegate.consumeCount(count);
    }
}
