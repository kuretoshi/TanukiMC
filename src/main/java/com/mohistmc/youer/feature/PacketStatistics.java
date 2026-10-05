package com.mohistmc.youer.feature;

public final class PacketStatistics {

    private PacketStatistics() {}

    public static void startStatisticsUpdater() {
        com.mohistmc.youer.feature.pulsegrasp.PacketStatistics.startStatisticsUpdater();
    }

    public static boolean isCollecting() {
        return com.mohistmc.youer.feature.pulsegrasp.PacketStatistics.isCollecting();
    }

    public static void updatePacketStats(String packetClassName, int bytes) {
        com.mohistmc.youer.feature.pulsegrasp.PacketStatistics.updatePacketStats(packetClassName, bytes, null);
    }

    public static long getTotalBytesSent() {
        return com.mohistmc.youer.feature.pulsegrasp.PacketStatistics.getTotalBytesSent();
    }

    public static long getTotalPacketsSent() {
        return com.mohistmc.youer.feature.pulsegrasp.PacketStatistics.getTotalPacketsSent();
    }
}
