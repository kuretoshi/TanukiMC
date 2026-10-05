package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.threading.ThreadedServer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.vehicle.MinecartSpawner;
import net.minecraft.world.level.SpawnData;
import org.bukkit.block.spawner.SpawnRule;
import org.bukkit.block.spawner.SpawnerEntry;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.block.CraftCreatureSpawner;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.minecart.SpawnerMinecart;

public final class CraftMinecartMobSpawner extends CraftMinecart implements SpawnerMinecart, org.bukkit.craftbukkit.spawner.PaperSharedSpawnerLogic { // Paper - more spawner API
    CraftMinecartMobSpawner(CraftServer server, MinecartSpawner entity) {
        super(server, entity);
    }


    @Override
    public EntityType getSpawnedType() {
        SpawnData spawnData = getHandle().getSpawner().nextSpawnData;
        if (spawnData == null) {
            return null;
        }

        Optional<net.minecraft.world.entity.EntityType<?>> type = net.minecraft.world.entity.EntityType.by(spawnData.getEntityToSpawn());
        return type.map(CraftEntityType::minecraftToBukkit).orElse(null);
    }

    @Override
    public void setSpawnedType(EntityType entityType) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            if (entityType == null) {
                getHandle().getSpawner().spawnPotentials = SimpleWeightedRandomList.empty(); // need clear the spawnPotentials to avoid nextSpawnData being replaced later
                getHandle().getSpawner().nextSpawnData = new SpawnData();
                return null;
            }
            Preconditions.checkArgument(entityType != EntityType.UNKNOWN, "Can't spawn EntityType %s from mob spawners!", entityType);

            RandomSource rand = getHandle().level().getRandom();
            getHandle().getSpawner().setEntityId(CraftEntityType.bukkitToMinecraft(entityType), getHandle().level(), rand, getHandle().blockPosition());
            return null;
        });
    }

    @Override
    public EntitySnapshot getSpawnedEntity() {
        SpawnData spawnData = getHandle().getSpawner().nextSpawnData;
        if (spawnData == null) {
            return null;
        }

        return CraftEntitySnapshot.create(spawnData.getEntityToSpawn());
    }

    @Override
    public void setSpawnedEntity(EntitySnapshot snapshot) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            CraftCreatureSpawner.setSpawnedEntity(getHandle().getSpawner(), snapshot, null, null);
            return null;
        });
    }

    @Override
    public void setSpawnedEntity(SpawnerEntry spawnerEntry) {
        Preconditions.checkArgument(spawnerEntry != null, "Entry cannot be null");

        ThreadedServer.onMainThreadIfRegionThread(() -> {
            CraftCreatureSpawner.setSpawnedEntity(getHandle().getSpawner(), spawnerEntry.getSnapshot(), spawnerEntry.getSpawnRule(), spawnerEntry.getEquipment());
            return null;
        });
    }

    @Override
    public void addPotentialSpawn(EntitySnapshot snapshot, int weight, SpawnRule spawnRule) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            CraftCreatureSpawner.addPotentialSpawn(getHandle().getSpawner(), snapshot, weight, spawnRule, null);
            return null;
        });
    }

    @Override
    public void addPotentialSpawn(SpawnerEntry spawnerEntry) {
        Preconditions.checkArgument(spawnerEntry != null, "Entry cannot be null");

        ThreadedServer.onMainThreadIfRegionThread(() -> {
            CraftCreatureSpawner.addPotentialSpawn(getHandle().getSpawner(), spawnerEntry.getSnapshot(), spawnerEntry.getSpawnWeight(), spawnerEntry.getSpawnRule(), spawnerEntry.getEquipment());
            return null;
        });
    }

    @Override
    public void setPotentialSpawns(Collection<SpawnerEntry> entries) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            CraftCreatureSpawner.setPotentialSpawns(getHandle().getSpawner(), entries);
            return null;
        });
    }

    @Override
    public List<SpawnerEntry> getPotentialSpawns() {
        return CraftCreatureSpawner.getPotentialSpawns(getHandle().getSpawner());
    }

    @Override
    public int getDelay() {
        return getHandle().getSpawner().spawnDelay;
    }

    @Override
    public void setDelay(int delay) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getSpawner().spawnDelay = delay;
            return null;
        });
    }

    @Override
    public int getMinSpawnDelay() {
        return getHandle().getSpawner().minSpawnDelay;
    }

    @Override
    public void setMinSpawnDelay(int spawnDelay) {
        Preconditions.checkArgument(spawnDelay <= getMaxSpawnDelay(), "Minimum Spawn Delay must be less than or equal to Maximum Spawn Delay");
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getSpawner().minSpawnDelay = spawnDelay;
            return null;
        });
    }

    @Override
    public int getMaxSpawnDelay() {
        return getHandle().getSpawner().maxSpawnDelay;
    }

    @Override
    public void setMaxSpawnDelay(int spawnDelay) {
        Preconditions.checkArgument(spawnDelay > 0, "Maximum Spawn Delay must be greater than 0.");
        Preconditions.checkArgument(spawnDelay >= getMinSpawnDelay(), "Maximum Spawn Delay must be greater than or equal to Minimum Spawn Delay");
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getSpawner().maxSpawnDelay = spawnDelay;
            return null;
        });
    }

    @Override
    public int getMaxNearbyEntities() {
        return getHandle().getSpawner().maxNearbyEntities;
    }

    @Override
    public void setMaxNearbyEntities(int maxNearbyEntities) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getSpawner().maxNearbyEntities = maxNearbyEntities;
            return null;
        });
    }

    @Override
    public int getSpawnCount() {
        return getHandle().getSpawner().spawnCount;
    }

    @Override
    public void setSpawnCount(int count) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getSpawner().spawnCount = count;
            return null;
        });
    }

    @Override
    public int getRequiredPlayerRange() {
        return getHandle().getSpawner().requiredPlayerRange;
    }

    @Override
    public void setRequiredPlayerRange(int requiredPlayerRange) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getSpawner().requiredPlayerRange = requiredPlayerRange;
            return null;
        });
    }

    @Override
    public int getSpawnRange() {
        return getHandle().getSpawner().spawnRange;
    }

    @Override
    public void setSpawnRange(int spawnRange) {
        ThreadedServer.onMainThreadIfRegionThread(() -> {
            getHandle().getSpawner().spawnRange = spawnRange;
            return null;
        });
    }

    @Override
    public MinecartSpawner getHandle() {
        return (MinecartSpawner) entity;
    }

    @Override
    public String toString() {
        return "CraftMinecartMobSpawner";
    }

    // Paper start - more spawner API
    @Override
    public net.minecraft.world.level.BaseSpawner getSpawner() {
        return this.getHandle().getSpawner();
    }

    @Override
    public net.minecraft.world.level.Level getInternalWorld() {
        return this.getHandle().level();
    }

    @Override
    public net.minecraft.core.BlockPos getInternalPosition() {
        return this.getHandle().blockPosition();
    }
    // Paper end - more spawner API
}
