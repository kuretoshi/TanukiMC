package test.youer.plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhaseProbePlugin extends JavaPlugin implements Listener {
    private static final String PROBE_ENTITY_TAG = "tanukimc_phase_probe_current";
    private static final int REGIONS = 16;
    private static final int RUNS_PER_REGION = 10;
    private static final int ADJACENT_STREAMS = 4;
    private final Set<String> threads = ConcurrentHashMap.newKeySet();
    private final Set<String> movedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> readyPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> respawnedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> crossedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> brokenPlayers = ConcurrentHashMap.newKeySet();
    private final java.util.Map<String, Integer> startingChunks = new ConcurrentHashMap<>();
    private final Set<Long> subTicks = ConcurrentHashMap.newKeySet();
    private final AtomicInteger runs = new AtomicInteger();
    private final AtomicInteger children = new AtomicInteger();
    private final AtomicInteger bridges = new AtomicInteger();
    private final AtomicInteger maximum = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicInteger adjacentActive = new AtomicInteger();
    private final AtomicInteger adjacentRuns = new AtomicInteger();
    private final AtomicInteger entityMoves = new AtomicInteger();
    private final AtomicInteger retirementAccepted = new AtomicInteger();
    private final AtomicInteger retirementCallbacks = new AtomicInteger();
    private final AtomicInteger retirementRuns = new AtomicInteger();
    private final AtomicBoolean retirementReady = new AtomicBoolean();
    private final AtomicInteger cleanedProbeEntities = new AtomicInteger();
    private final AtomicInteger onlineRuns = new AtomicInteger();
    private final AtomicInteger permissionChecks = new AtomicInteger();
    private final AtomicBoolean ownershipChecked = new AtomicBoolean();
    private final AtomicBoolean remoteGuardExercised = new AtomicBoolean();
    private final AtomicBoolean remoteEntityGuardExercised = new AtomicBoolean();
    private final AtomicBoolean remotePlayerGuardExercised = new AtomicBoolean();
    private final AtomicBoolean remotePlayerProbeAttempted = new AtomicBoolean();
    private final AtomicBoolean localRideChecked = new AtomicBoolean();
    private final AtomicBoolean localEffectChecked = new AtomicBoolean();
    private final AtomicBoolean chunkLoadChecked = new AtomicBoolean();
    private final AtomicInteger chunkLoadRuns = new AtomicInteger();
    private final AtomicInteger asyncChunkLoads = new AtomicInteger();
    private final AtomicBoolean storedChunkLoaded = new AtomicBoolean();
    private final Integer storedChunkX = Integer.getInteger("youer.phase.stored_chunk_x");
    private final Integer storedChunkZ = Integer.getInteger("youer.phase.stored_chunk_z");
    private final boolean withCreate = Boolean.getBoolean("youer.phase.with_create");
    private final boolean withLithium = Boolean.getBoolean("youer.phase.with_lithium");
    private boolean lithiumLoaded;
    private final boolean withFerriteCore = Boolean.getBoolean("youer.phase.with_ferritecore");
    private boolean ferriteCoreLoaded;
    private final boolean withModernFix = Boolean.getBoolean("youer.phase.with_modernfix");
    private boolean modernFixLoaded;
    private final boolean withMekanism = Boolean.getBoolean("youer.phase.with_mekanism");
    private boolean mekanismLoaded;
    private final java.util.List<net.minecraft.core.BlockPos> mekanismPositions = new java.util.ArrayList<>();
    private final java.util.Map<Integer, Integer> mekanismTickSamples = new ConcurrentHashMap<>();
    private final Set<Integer> mekanismTickRegions = ConcurrentHashMap.newKeySet();
    private final Set<net.minecraft.core.BlockPos> mekanismBukkitPositions = ConcurrentHashMap.newKeySet();
    private final java.util.Map<Integer, Integer> mekanismBukkitSamples = new ConcurrentHashMap<>();
    private final Set<Integer> mekanismBukkitTickRegions = ConcurrentHashMap.newKeySet();
    private org.bukkit.Material mekanismBukkitMaterial;
    private java.lang.reflect.Field mekanismTicker;
    private final boolean withLuckPerms = Boolean.getBoolean("youer.phase.with_luckperms");
    private final boolean withWorldEdit = Boolean.getBoolean("youer.phase.with_worldedit");
    private int worldEditBlocks;
    private final Set<Integer> worldEditRegionEdits = ConcurrentHashMap.newKeySet();
    private final java.util.List<net.minecraft.core.BlockPos> createPositions = new java.util.ArrayList<>();
    private final java.util.List<net.minecraft.core.BlockPos> createMotorPositions = new java.util.ArrayList<>();
    private final java.util.Map<Integer, Integer> createCounterSamples = new ConcurrentHashMap<>();
    private final Set<Integer> createTickRegions = ConcurrentHashMap.newKeySet();
    private final Set<Integer> createPoweredRegions = ConcurrentHashMap.newKeySet();
    private final Set<Integer> createPlacementRegions = ConcurrentHashMap.newKeySet();
    private final Set<Integer> createPlacementPoweredRegions = ConcurrentHashMap.newKeySet();
    private final Set<Integer> createBukkitPlacedRegions = ConcurrentHashMap.newKeySet();
    private final Set<Integer> createBukkitPoweredRegions = ConcurrentHashMap.newKeySet();
    private org.bukkit.Material createBukkitCogMaterial;
    private org.bukkit.Material createBukkitCogItemMaterial;
    private final Set<Integer> createInventoryRegions = ConcurrentHashMap.newKeySet();
    private final Set<Integer> createInventoryPersistedRegions = ConcurrentHashMap.newKeySet();
    private final AtomicInteger createExistingChestRegions = new AtomicInteger();
    private final AtomicBoolean createBorderPlaced = new AtomicBoolean();
    private final AtomicBoolean createBorderPowered = new AtomicBoolean();
    private final AtomicBoolean createTickSampleFailed = new AtomicBoolean();
    private final AtomicInteger createPlacedPairs = new AtomicInteger();
    private final AtomicInteger createPlacementActive = new AtomicInteger();
    private final AtomicInteger createPlacementMax = new AtomicInteger();
    private final AtomicBoolean blockEntityOwnershipChecked = new AtomicBoolean();
    private java.lang.reflect.Field createTickCounter;
    private java.lang.reflect.Method createSpeedGetter;
    private final AtomicBoolean mainTaskScheduled = new AtomicBoolean();
    private final AtomicInteger mainTaskRuns = new AtomicInteger();
    private final AtomicInteger mainTaskOverlaps = new AtomicInteger();
    private final java.util.Map<Integer, String> adjacentThreadPerTick = new ConcurrentHashMap<>();
    private boolean scheduled;
    private boolean finished;
    private final boolean waitForPlayers = Boolean.getBoolean("youer.phase.wait_for_players");
    private final boolean waitForClient = Boolean.getBoolean("youer.phase.wait_for_client");
    private final boolean splitPlayerRegions = Boolean.getBoolean("youer.phase.split_player_regions");
    private boolean splitPlayerCrossed;
    private int ticks;
    private int generatorCases;
    private int cleanupSweeps;
    private ArmorStand adjacentEntity;
    private final java.util.List<ArmorStand> probeEntities = new java.util.ArrayList<>();
    private final java.util.List<ArmorStand> movingEntities = new java.util.ArrayList<>();
    private BlockEventProbe blockEvents;
    private FluidTickProbe fluidTicks;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.lithiumLoaded = net.neoforged.fml.ModList.get().isLoaded("lithium");
        this.ferriteCoreLoaded = net.neoforged.fml.ModList.get().isLoaded("ferritecore");
        this.modernFixLoaded = net.neoforged.fml.ModList.get().isLoaded("modernfix");
        this.mekanismLoaded = net.neoforged.fml.ModList.get().isLoaded("mekanism");
        if (this.withLithium && !this.lithiumLoaded) {
            this.failures.incrementAndGet();
        }
        if (this.withFerriteCore && !this.ferriteCoreLoaded) {
            this.failures.incrementAndGet();
        }
        if (this.withModernFix && !this.modernFixLoaded) {
            this.failures.incrementAndGet();
        }
        if (this.withMekanism && !this.mekanismLoaded) {
            this.failures.incrementAndGet();
        }
        if (this.withLuckPerms && !Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            this.failures.incrementAndGet();
        }
        var buildInfo = io.papermc.paper.ServerBuildInfo.buildInfo();
        if (!"TanukiMC".equals(Bukkit.getName())
            || !"TanukiMC".equals(buildInfo.brandName())
            || !net.kyori.adventure.key.Key.key("tanukimc", "tanukimc").equals(buildInfo.brandId())
            || !buildInfo.isBrandCompatible(net.kyori.adventure.key.Key.key("mohistmc", "youer"))) {
            this.failures.incrementAndGet();
            getLogger().severe("Unexpected server brand: " + buildInfo.brandId() + " / " + buildInfo.brandName());
        }
        for (String key : new String[] {"active", "worldDepth", "worldTicks", "overlaps", "scheduledTicks", "scheduledFailures", "scheduledActive", "scheduledMax", "scheduledChunk15", "scheduledChunk128", "scheduledChunk256", "scheduledChunk384", "randomActive", "randomMax", "randomFailures", "randomChunk15", "randomChunk128", "randomChunk256", "randomChunk384", "beActive", "beMax", "beFailures", "beChunk15", "beChunk128", "beChunk256", "beChunk384", "entityActive", "entityMax", "entityFailures", "entityChunk15", "entityChunk128", "entityChunk256", "entityChunk384", "playerTicks", "playerFailures"}) {
            counter(key);
        }
        Bukkit.getScheduler().runTaskTimer(this, this::control, 10L, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.splitPlayerRegions) {
            return;
        }
        var player = event.getPlayer();
        if (!player.getName().equals("ThreadProbeA") && !player.getName().equals("ThreadProbeB")) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> prepareProbePlayer(player.getUniqueId(), player.getName()), 2L);
    }

    private void prepareProbePlayer(java.util.UUID id, String name) {
        var player = Bukkit.getPlayer(id);
        if (player == null || !player.isOnline()) {
            this.failures.incrementAndGet();
            return;
        }
        if (player.isDead() || player.getHealth() <= 0) {
            if (!this.respawnedPlayers.add(name)) {
                this.failures.incrementAndGet();
                getLogger().warning("PLAYER_RESPAWN_FAILED player=" + name);
                return;
            }
            player.spigot().respawn();
            Bukkit.getScheduler().runTaskLater(this, () -> prepareProbePlayer(id, name), 2L);
            return;
        }
        player.setInvulnerable(true);
        World world = player.getWorld();
        Location destination;
        if (name.equals("ThreadProbeA")) {
            world.getChunkAt(26, -1);
            world.getChunkAt(27, -1);
            world.getBlockAt(433, -61, -5).setType(org.bukkit.Material.GRASS_BLOCK);
            destination = new Location(world, 417.0D, -60.0D, -4.3D);
        } else {
            world.getChunkAt(64, 0);
            world.getChunkAt(65, 0);
            world.getBlockAt(1041, -61, 9).setType(org.bukkit.Material.GRASS_BLOCK);
            this.startingChunks.put("ThreadProbeB", 64);
            destination = new Location(world, 1025.0D, -60.0D, 9.5D);
        }
        if (player.teleport(destination)) {
            this.readyPlayers.add(name);
        } else {
            this.failures.incrementAndGet();
            getLogger().warning("PLAYER_RESET_FAILED player=" + name + " health=" + player.getHealth()
                + " dead=" + player.isDead() + " valid=" + player.isValid());
        }
    }

    @EventHandler
    public void onPlayerFailMove(io.papermc.paper.event.player.PlayerFailMoveEvent event) {
        if (this.splitPlayerRegions) {
            getLogger().warning("PLAYER_MOVE_REJECTED player=" + event.getPlayer().getName()
                + " reason=" + event.getFailReason()
                + " from=" + event.getFrom() + " to=" + event.getTo());
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (this.waitForPlayers && event.getEntity().getName().startsWith("ThreadProbe")) {
            this.failures.incrementAndGet();
            var damage = event.getEntity().getLastDamageCause();
            getLogger().warning("PLAYER_PROBE_DEATH player=" + event.getEntity().getName()
                + " at=" + event.getEntity().getLocation()
                + " damage=" + (damage == null ? "unknown" : damage.getCause())
                + " damager=" + (damage instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity
                    ? byEntity.getDamager().getType() : "none"));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (this.waitForPlayers && event.getTo() != null
            && Math.abs(event.getTo().getX() - event.getFrom().getX()) > 0.01D) {
            String name = event.getPlayer().getName();
            this.movedPlayers.add(name);
            int startingChunk = this.startingChunks.computeIfAbsent(name, ignored -> event.getFrom().getBlockX() >> 4);
            if ((event.getTo().getBlockX() >> 4) != startingChunk) {
                this.crossedPlayers.add(name);
            }
            if (this.splitPlayerRegions && name.equals("ThreadProbeB")
                && (event.getTo().getBlockX() >> 4) >= 65) {
                this.splitPlayerCrossed = true;
            }
            if (counter("active").get() != 0) {
                counter("overlaps").incrementAndGet();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (this.waitForPlayers && event.getPlayer().getName().startsWith("ThreadProbe")) {
            getLogger().info("PLAYER_BLOCK_BREAK player=" + event.getPlayer().getName()
                + " at=" + event.getBlock().getLocation() + " type=" + event.getBlock().getType());
            if (event.getBlock().getType() != org.bukkit.Material.GRASS_BLOCK
                || event.getBlock().getY() != -61) {
                this.failures.incrementAndGet();
                getLogger().warning("Unexpected player block break: " + event.getPlayer().getName()
                    + " at " + event.getBlock().getLocation() + " type=" + event.getBlock().getType());
            }
            this.brokenPlayers.add(event.getPlayer().getName());
        }
    }

    private void control() {
        if (this.finished) {
            return;
        }
        if (this.waitForPlayers && !this.scheduled
            && (Bukkit.getOnlinePlayers().size() < 2 || this.splitPlayerRegions && this.readyPlayers.size() < 2)) {
            return;
        }
        if (this.waitForClient && !this.scheduled && Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        this.ticks++;
        if (this.scheduled && this.cleanupSweeps < 10) {
            cleanupOldProbeEntities(Bukkit.getWorlds().getFirst());
            this.cleanupSweeps++;
        }
        if (counter("active").get() != 0) {
            counter("overlaps").incrementAndGet();
        }
        if (!this.scheduled) {
            this.scheduled = true;
            try {
                GeneratorProbe.verify(false);
                this.generatorCases++;
                GeneratorProbe.verify(true);
                this.generatorCases++;
            } catch (Throwable throwable) {
                this.failures.incrementAndGet();
                getLogger().log(java.util.logging.Level.SEVERE, "Custom generator probe failed", throwable);
                finish();
                return;
            }
            World world = Bukkit.getWorlds().getFirst();
            if (!(((org.bukkit.craftbukkit.CraftWorld) world).getHandle().getRandom()
                instanceof com.mohistmc.youer.threading.SynchronizedWorldRandom)) {
                this.failures.incrementAndGet();
            }
            cleanupOldProbeEntities(world);
            if (this.withWorldEdit) {
                try {
                    this.worldEditBlocks = WorldEditProbe.verify(world);
                } catch (Throwable throwable) {
                    this.failures.incrementAndGet();
                    getLogger().log(java.util.logging.Level.SEVERE, "WorldEdit probe failed", throwable);
                    finish();
                    return;
                }
            }
            scheduleAdjacent(world);
            this.blockEvents = new BlockEventProbe(this, world);
            scheduleModBlockTicks(world);
            scheduleEntityMovement(world);
            this.fluidTicks = new FluidTickProbe(this, world);
            scheduleRetirement(world);
            for (int i = 0; i < REGIONS; i++) {
                int chunkX = i * 128;
                AtomicInteger localRuns = new AtomicInteger();
                Bukkit.getRegionScheduler().runAtFixedRate(this, world, chunkX, 0, task -> {
                    probe(world, chunkX);
                    if (localRuns.incrementAndGet() == 1) {
                        if (this.withWorldEdit && chunkX < 4 * 128) {
                            try {
                                if (WorldEditProbe.verifyRegion(world, chunkX) == 1) {
                                    this.worldEditRegionEdits.add(chunkX);
                                }
                            } catch (Throwable throwable) {
                                this.failures.incrementAndGet();
                                getLogger().log(java.util.logging.Level.SEVERE, "WorldEdit region probe failed", throwable);
                            }
                        }
                        int parentTick = Bukkit.getCurrentTick();
                        Bukkit.getRegionScheduler().execute(this, world, chunkX, 0, () -> {
                            if (Bukkit.getCurrentTick() <= parentTick) {
                                this.failures.incrementAndGet();
                            }
                            probe(world, chunkX);
                            this.children.incrementAndGet();
                        });
                    }
                    if (localRuns.get() == RUNS_PER_REGION) {
                        task.cancel();
                    }
                    this.runs.incrementAndGet();
                }, 1L, 2L);
            }
        }
        if (this.runs.get() == REGIONS * RUNS_PER_REGION && this.children.get() == REGIONS
            && this.adjacentRuns.get() == ADJACENT_STREAMS * RUNS_PER_REGION
            && this.entityMoves.get() == 4 * RUNS_PER_REGION) {
            finish();
        } else if (this.ticks >= 200) {
            logEntityState("timeout");
            this.failures.incrementAndGet();
            finish();
        }
    }

    private void scheduleAdjacent(World world) {
        for (int chunkX : new int[] {15, 16}) {
            AtomicInteger count = new AtomicInteger();
            Bukkit.getRegionScheduler().runAtFixedRate(this, world, chunkX, 0, task -> {
                probeAdjacent(world);
                if (count.incrementAndGet() == RUNS_PER_REGION) {
                    task.cancel();
                }
            }, 1L, 2L);
        }
        world.addPluginChunkTicket(15, 0, this);
        scheduleEntity(world);
        scheduleEntity(world);
    }

    private void scheduleModBlockTicks(World world) {
        var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
        net.minecraft.world.level.block.Block mekanismBlock = null;
        if (this.withMekanism) {
            var mekanismId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mekanism", "basic_energy_cube");
            mekanismBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(mekanismId).orElse(null);
            if (mekanismBlock == null) {
                this.failures.incrementAndGet();
            } else {
                this.mekanismBukkitMaterial = org.bukkit.craftbukkit.util.CraftMagicNumbers.getMaterial(mekanismBlock);
                if (this.mekanismBukkitMaterial == null || !this.mekanismBukkitMaterial.isBlock()
                    || org.bukkit.craftbukkit.util.CraftMagicNumbers.getBlock(this.mekanismBukkitMaterial) != mekanismBlock) {
                    this.failures.incrementAndGet();
                    getLogger().warning("Mekanism energy cube is unavailable as a Bukkit block material");
                }
            }
        }
        net.minecraft.world.level.block.Block createBlock = null;
        net.minecraft.world.level.block.Block createMotor = null;
        if (this.withCreate) {
            var createId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "cogwheel");
            createBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(createId).orElse(null);
            var motorId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "creative_motor");
            createMotor = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(motorId).orElse(null);
            if (createBlock == null || createMotor == null) {
                this.failures.incrementAndGet();
            } else {
                this.createBukkitCogMaterial = org.bukkit.craftbukkit.util.CraftMagicNumbers.getMaterial(createBlock);
                this.createBukkitCogItemMaterial = org.bukkit.craftbukkit.util.CraftMagicNumbers.getMaterial(createBlock.asItem());
                if (this.createBukkitCogMaterial == null || !this.createBukkitCogMaterial.isBlock()
                    || org.bukkit.craftbukkit.util.CraftMagicNumbers.getBlock(this.createBukkitCogMaterial) != createBlock) {
                    this.failures.incrementAndGet();
                    getLogger().warning("Create cogwheel is unavailable as a Bukkit block material");
                }
                if (this.createBukkitCogItemMaterial == null || !this.createBukkitCogItemMaterial.isItem()
                    || org.bukkit.craftbukkit.util.CraftMagicNumbers.getItem(this.createBukkitCogItemMaterial) != createBlock.asItem()) {
                    this.failures.incrementAndGet();
                    getLogger().warning("Create cogwheel is unavailable as a Bukkit item material");
                }
            }
        }
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("youer_phase_probe", "scheduled_probe");
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(id).orElseThrow();
        var blockEntityId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("youer_phase_probe", "block_entity_probe");
        var blockEntityBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(blockEntityId).orElseThrow();
        for (int chunkX : new int[] {15, 128, 256, 384}) {
            world.addPluginChunkTicket(chunkX, 0, this);
            var chunkPos = new net.minecraft.world.level.ChunkPos(chunkX, 0);
            level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED, chunkPos, 2, chunkPos, true);
            var pos = new net.minecraft.core.BlockPos(chunkX * 16 + 8, 68, 10);
            level.setBlock(pos, block.defaultBlockState(), 3);
            level.scheduleTick(pos, block, 5);
            var blockEntityPos = new net.minecraft.core.BlockPos(chunkX * 16 + 8, 104, 10);
            level.setBlock(blockEntityPos, blockEntityBlock.defaultBlockState(), 3);
            if (level.getBlockEntity(blockEntityPos) == null) {
                counter("beFailures").incrementAndGet();
            }
            if (mekanismBlock != null) {
                var mekanismPos = new net.minecraft.core.BlockPos(chunkX * 16 + 9, 112, 14);
                level.setBlock(mekanismPos, mekanismBlock.defaultBlockState(), 3);
                var tile = level.getBlockEntity(mekanismPos);
                if (tile == null || !tile.getClass().getName().startsWith("mekanism.")) {
                    this.failures.incrementAndGet();
                } else {
                    this.mekanismPositions.add(mekanismPos);
                    if (this.mekanismTicker == null) {
                        try {
                            this.mekanismTicker = tile.getClass().getField("ticker");
                        } catch (ReflectiveOperationException ex) {
                            this.failures.incrementAndGet();
                            getLogger().log(java.util.logging.Level.SEVERE, "Mekanism tick probe initialization failed", ex);
                        }
                    }
                }
            }
            if (createBlock != null && createMotor != null) {
                var oldCreatePos = new net.minecraft.core.BlockPos(chunkX * 16 + 9, 104, 10);
                if (level.getBlockState(oldCreatePos).is(createBlock)) {
                    level.setBlock(oldCreatePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
                var motorPos = new net.minecraft.core.BlockPos(chunkX * 16 + 10, 104, 10);
                var motorState = createMotor.defaultBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.EAST);
                level.setBlock(motorPos, motorState, 3);
                if (level.getBlockEntity(motorPos) == null) {
                    this.failures.incrementAndGet();
                } else {
                    this.createMotorPositions.add(motorPos);
                }
                var createPos = new net.minecraft.core.BlockPos(chunkX * 16 + 11, 104, 10);
                var createState = createBlock.defaultBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X);
                level.setBlock(createPos, createState, 3);
                var createEntity = level.getBlockEntity(createPos);
                if (createEntity == null || !createEntity.getClass().getName().startsWith("com.simibubi.create.")) {
                    this.failures.incrementAndGet();
                } else {
                    this.createPositions.add(createPos);
                    if (this.createTickCounter == null) {
                        try {
                            Class<?> type = createEntity.getClass();
                            while (type != null && !type.getName().equals("com.simibubi.create.foundation.blockEntity.SmartBlockEntity")) {
                                type = type.getSuperclass();
                            }
                            if (type == null) {
                                throw new IllegalStateException("Create block entity does not extend SmartBlockEntity");
                            }
                            this.createTickCounter = type.getDeclaredField("lazyTickCounter");
                            this.createTickCounter.setAccessible(true);
                            this.createSpeedGetter = createEntity.getClass().getMethod("getSpeed");
                        } catch (ReflectiveOperationException | RuntimeException ex) {
                            this.failures.incrementAndGet();
                            getLogger().log(java.util.logging.Level.SEVERE, "Create tick probe initialization failed", ex);
                        }
                    }
                }
            }
            for (int y = 96; y < 100; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        level.setBlock(new net.minecraft.core.BlockPos(chunkX * 16 + x, y, z), block.defaultBlockState(), 2);
                    }
                }
            }
        }
        if (createBlock != null && createMotor != null) {
            world.addPluginChunkTicket(16, 0, this);
            var motorPos = new net.minecraft.core.BlockPos(15 * 16 + 15, 104, 13);
            var cogPos = new net.minecraft.core.BlockPos(16 * 16, 104, 13);
            level.setBlock(motorPos, createMotor.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.EAST), 3);
            level.setBlock(cogPos, createBlock.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X), 3);
            if (level.getBlockEntity(motorPos) != null && level.getBlockEntity(cogPos) != null) {
                this.createBorderPlaced.set(true);
            } else {
                this.failures.incrementAndGet();
            }
        }
    }

    private void scheduleEntityMovement(World world) {
        for (int chunkX : new int[] {15, 128, 256, 384}) {
            world.addPluginChunkTicket(chunkX + 1, 0, this);
            ArmorStand entity = world.spawn(new Location(world, chunkX * 16 + 8, 112, 8), ArmorStand.class);
            entity.setGravity(false);
            entity.addScoreboardTag(PROBE_ENTITY_TAG);
            this.probeEntities.add(entity);
            this.movingEntities.add(entity);
            AtomicInteger count = new AtomicInteger();
            Bukkit.getRegionScheduler().runAtFixedRate(this, world, chunkX, 0, task -> {
                try {
                    if (!entity.isValid()) {
                        return;
                    }
                    if (!Bukkit.isOwnedByCurrentRegion(world, chunkX, 0)
                        || !Bukkit.isOwnedByCurrentRegion(world, chunkX + 1, 0)) {
                        this.failures.incrementAndGet();
                    }
                    int next = count.incrementAndGet();
                    var handle = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
                    handle.setPos(chunkX * 16 + (next % 2 == 0 ? 8 : 24), 112, 8);
                    this.entityMoves.incrementAndGet();
                    if (next == RUNS_PER_REGION) {
                        task.cancel();
                    }
                } catch (Throwable throwable) {
                    this.failures.incrementAndGet();
                    getLogger().log(java.util.logging.Level.SEVERE, "Entity movement probe failed", throwable);
                    task.cancel();
                }
            }, 1L, 1L);
        }
    }

    private void scheduleEntity(World world) {
        ArmorStand entity = world.spawn(new Location(world, 15 * 16 + 8, 70, 8), ArmorStand.class);
        ArmorStand passenger = world.spawn(new Location(world, 15 * 16 + 8, 70, 8), ArmorStand.class);
        this.adjacentEntity = entity;
        this.probeEntities.add(entity);
        this.probeEntities.add(passenger);
        entity.setGravity(false);
        passenger.setGravity(false);
        entity.addScoreboardTag(PROBE_ENTITY_TAG);
        passenger.addScoreboardTag(PROBE_ENTITY_TAG);
        AtomicInteger count = new AtomicInteger();
        var scheduledTask = entity.getScheduler().runAtFixedRate(this, task -> {
            if (!Bukkit.isOwnedByCurrentRegion(entity)) {
                this.failures.incrementAndGet();
            }
            if (count.get() == 0) {
                try {
                    boolean bukkitMounted = entity.addPassenger(passenger);
                    boolean bukkitDismounted = bukkitMounted && entity.eject() && passenger.getVehicle() == null;
                    var vehicleHandle = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
                    var passengerHandle = ((org.bukkit.craftbukkit.entity.CraftEntity) passenger).getHandle();
                    boolean directMounted = passengerHandle.startRiding(vehicleHandle, true);
                    if (directMounted) {
                        passengerHandle.stopRiding();
                    }
                    if (bukkitDismounted && directMounted && passengerHandle.getVehicle() == null) {
                        this.localRideChecked.set(true);
                    } else {
                        this.failures.incrementAndGet();
                    }
                    var effect = new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.POISON, 20);
                    var livingPassenger = (net.minecraft.world.entity.LivingEntity) passengerHandle;
                    if (livingPassenger.addEffect(effect) && livingPassenger.removeEffect(
                        net.minecraft.world.effect.MobEffects.POISON)) {
                        this.localEffectChecked.set(true);
                    } else {
                        this.failures.incrementAndGet();
                    }
                } catch (Throwable throwable) {
                    this.failures.incrementAndGet();
                    getLogger().log(java.util.logging.Level.SEVERE, "Local riding probe failed", throwable);
                }
            }
            probeAdjacent(world);
            if (count.incrementAndGet() == RUNS_PER_REGION) {
                task.cancel();
            }
        }, () -> this.failures.incrementAndGet(), 1L, 2L);
        if (scheduledTask == null) {
            this.failures.incrementAndGet();
            getLogger().severe("Entity scheduler rejected probe task");
        }
        logEntityState("registered");
    }

    private void cleanupOldProbeEntities(World world) {
        for (int chunkX : new int[] {15, 16, 128, 129, 256, 257, 384, 385}) {
            for (org.bukkit.entity.Entity entity : world.getChunkAt(chunkX, 0).getEntities()) {
                if (entity instanceof ArmorStand && !entity.getScoreboardTags().contains(PROBE_ENTITY_TAG)
                    && (entity.getY() == 112.0 || chunkX == 15 && entity.getY() == 70.0)) {
                    entity.remove();
                    this.cleanedProbeEntities.incrementAndGet();
                }
            }
        }
    }

    private void scheduleRetirement(World world) {
        ArmorStand entity = world.spawn(new Location(world, 15 * 16 + 8, 70, 8), ArmorStand.class);
        entity.setGravity(false);
        entity.addScoreboardTag(PROBE_ENTITY_TAG);
        java.util.concurrent.CountDownLatch removing = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 256; i++) {
                    if (entity.getScheduler().runDelayed(this, task -> this.retirementRuns.incrementAndGet(),
                        this.retirementCallbacks::incrementAndGet, 100L) != null) {
                        this.retirementAccepted.incrementAndGet();
                    }
                    if (i == 31) {
                        Bukkit.getRegionScheduler().execute(this, world, 15, 0, () -> {
                            removing.countDown();
                            entity.remove();
                        });
                        if (!removing.await(15L, java.util.concurrent.TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Region removal did not run");
                        }
                    } else if (i > 31) {
                        Thread.sleep(1L);
                    }
                }
            } catch (Throwable throwable) {
                this.failures.incrementAndGet();
                getLogger().log(java.util.logging.Level.SEVERE, "Entity retirement scheduling failed", throwable);
            } finally {
                this.retirementReady.set(true);
            }
        });
    }

    private void logEntityState(String stage) {
        ArmorStand entity = this.adjacentEntity;
        if (entity != null) {
            getLogger().info("ENTITY_PROBE " + stage + " valid=" + entity.isValid()
                + " dead=" + entity.isDead() + " chunkLoaded=" + entity.getWorld().isChunkLoaded(15, 0)
                + " visible=" + (entity.getWorld().getEntity(entity.getUniqueId()) != null));
        }
    }

    private void probeAdjacent(World world) {
        if (this.adjacentActive.incrementAndGet() != 1) {
            this.failures.incrementAndGet();
        }
        try {
            if (this.withCreate && this.createSpeedGetter != null) {
                var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                var cogPos = new net.minecraft.core.BlockPos(16 * 16, 104, 13);
                var cog = level.getBlockEntity(cogPos);
                if (cog != null) {
                    try {
                        if (((Number) this.createSpeedGetter.invoke(cog)).floatValue() != 0.0F) {
                            this.createBorderPowered.set(true);
                        }
                    } catch (ReflectiveOperationException | RuntimeException ex) {
                        if (this.createTickSampleFailed.compareAndSet(false, true)) {
                            this.failures.incrementAndGet();
                            getLogger().log(java.util.logging.Level.SEVERE, "Create border probe failed", ex);
                        }
                    }
                }
            }
            String thread = Thread.currentThread().getName();
            String previous = this.adjacentThreadPerTick.putIfAbsent(Bukkit.getCurrentTick(), thread);
            if (previous != null && !previous.equals(thread)) {
                this.failures.incrementAndGet();
            }
            if (!Bukkit.isOwnedByCurrentRegion(world, 15, 0) || !Bukkit.isOwnedByCurrentRegion(world, 16, 0, 1)) {
                this.failures.incrementAndGet();
            }
            Location local = new Location(world, 15 * 16 + 8, 70, 8);
            var localPosition = io.papermc.paper.math.Position.fine(local);
            if (!Bukkit.isOwnedByCurrentRegion(local)
                || !Bukkit.isOwnedByCurrentRegion(local, 0)
                || !Bukkit.isOwnedByCurrentRegion(world, localPosition)
                || !Bukkit.isOwnedByCurrentRegion(world, localPosition, 0)
                || !Bukkit.isOwnedByCurrentRegion(this.adjacentEntity)) {
                this.failures.incrementAndGet();
            }
            var chunks = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle().getChunkSource();
            if (chunks.getChunkNow(15, 0) == null || chunks.getChunkAtIfLoadedImmediately(15, 0) == null) {
                this.failures.incrementAndGet();
            }
            if (this.ownershipChecked.compareAndSet(false, true)) {
                var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                int remoteChunk = -1;
                for (int candidate : new int[] {128, 256, 384, 512, 640, 768, 896, 1024,
                    1152, 1280, 1408, 1536, 1664, 1792, 1920}) {
                    if (!Bukkit.isOwnedByCurrentRegion(world, candidate, 0)) {
                        remoteChunk = candidate;
                        break;
                    }
                }
                if (remoteChunk >= 0) {
                    this.remoteGuardExercised.set(true);
                    Location remoteLocation = new Location(world, remoteChunk * 16 + 8, 120, 10);
                    var remotePosition = io.papermc.paper.math.Position.fine(remoteLocation);
                    if (Bukkit.isOwnedByCurrentRegion(remoteLocation)
                        || Bukkit.isOwnedByCurrentRegion(remoteLocation, 1)
                        || Bukkit.isOwnedByCurrentRegion(world, remotePosition)
                        || Bukkit.isOwnedByCurrentRegion(world, remotePosition, 1)
                        || Bukkit.isOwnedByCurrentRegion(world, remoteChunk, 0, 1)) {
                        this.failures.incrementAndGet();
                    }
                    var remote = new net.minecraft.core.BlockPos(remoteChunk * 16 + 8, 120, 10);
                    var replacement = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
                    try {
                        level.setBlock(remote, replacement, 3);
                        this.failures.incrementAndGet();
                    } catch (IllegalStateException expected) {
                        // An adjacent region task must not mutate a different owner.
                    }
                    try {
                        level.getChunk(remoteChunk, 0).setBlockState(remote, replacement, false);
                        this.failures.incrementAndGet();
                    } catch (IllegalStateException expected) {
                        // Direct chunk writes must obey the same ownership boundary.
                    }
                    ArmorStand remoteEntity = this.movingEntities.stream()
                        .filter(entity -> !Bukkit.isOwnedByCurrentRegion(entity)).findFirst().orElse(null);
                    boolean removalDenied = false;
                    boolean directRemovalDenied = false;
                    boolean causeRemovalDenied = false;
                    boolean positionDenied = false;
                    boolean rawPositionDenied = false;
                    boolean velocityDenied = false;
                    boolean directVelocityDenied = false;
                    boolean rotationDenied = false;
                    boolean directRotationDenied = false;
                    boolean bukkitRemoteVehicleDenied = false;
                    boolean bukkitRemotePassengerDenied = false;
                    boolean directRemoteVehicleDenied = false;
                    boolean directRemotePassengerDenied = false;
                    boolean fireDenied = false;
                    boolean persistentDenied = false;
                    boolean gravityDenied = false;
                    boolean poseDenied = false;
                    boolean tagDenied = false;
                    boolean healthDenied = false;
                    boolean potionDenied = false;
                    boolean directHealthDenied = false;
                    boolean directAbsorptionDenied = false;
                    boolean directEffectDenied = false;
                    boolean directForceEffectDenied = false;
                    boolean directRemoveEffectsDenied = false;
                    boolean teleportDenied = false;
                    if (remoteEntity != null) {
                        try {
                            remoteEntity.remove();
                        } catch (IllegalStateException expected) {
                            removalDenied = true;
                        }
                        var remoteHandle = ((org.bukkit.craftbukkit.entity.CraftEntity) remoteEntity).getHandle();
                        try {
                            remoteHandle.discard();
                        } catch (IllegalStateException expected) {
                            directRemovalDenied = true;
                        }
                        try {
                            remoteHandle.discard(org.bukkit.event.entity.EntityRemoveEvent.Cause.PLUGIN);
                        } catch (IllegalStateException expected) {
                            causeRemovalDenied = true;
                        }
                        try {
                            remoteHandle.setPos(remoteHandle.getX() + 1, remoteHandle.getY(), remoteHandle.getZ());
                        } catch (IllegalStateException expected) {
                            positionDenied = true;
                        }
                        try {
                            remoteHandle.setPosRaw(remoteHandle.getX() + 1, remoteHandle.getY(), remoteHandle.getZ());
                        } catch (IllegalStateException expected) {
                            rawPositionDenied = true;
                        }
                        try {
                            remoteEntity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                        } catch (IllegalStateException expected) {
                            velocityDenied = true;
                        }
                        try {
                            remoteHandle.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                        } catch (IllegalStateException expected) {
                            directVelocityDenied = true;
                        }
                        try {
                            remoteEntity.setRotation(0, 0);
                        } catch (IllegalStateException expected) {
                            rotationDenied = true;
                        }
                        try {
                            remoteHandle.setYRot(0);
                        } catch (IllegalStateException expected) {
                            directRotationDenied = true;
                        }
                        try {
                            remoteEntity.addPassenger(this.adjacentEntity);
                        } catch (IllegalStateException expected) {
                            bukkitRemoteVehicleDenied = true;
                        }
                        try {
                            this.adjacentEntity.addPassenger(remoteEntity);
                        } catch (IllegalStateException expected) {
                            bukkitRemotePassengerDenied = true;
                        }
                        var adjacentHandle = ((org.bukkit.craftbukkit.entity.CraftEntity) this.adjacentEntity).getHandle();
                        try {
                            adjacentHandle.startRiding(remoteHandle);
                        } catch (IllegalStateException expected) {
                            directRemoteVehicleDenied = true;
                        }
                        try {
                            remoteHandle.startRiding(adjacentHandle);
                        } catch (IllegalStateException expected) {
                            directRemotePassengerDenied = true;
                        }
                        try {
                            remoteEntity.setFireTicks(0);
                        } catch (IllegalStateException expected) {
                            fireDenied = true;
                        }
                        try {
                            remoteEntity.setPersistent(true);
                        } catch (IllegalStateException expected) {
                            persistentDenied = true;
                        }
                        try {
                            remoteEntity.setGravity(false);
                        } catch (IllegalStateException expected) {
                            gravityDenied = true;
                        }
                        try {
                            remoteEntity.setBodyPose(new org.bukkit.util.EulerAngle(0, 0, 0));
                        } catch (IllegalStateException expected) {
                            poseDenied = true;
                        }
                        try {
                            remoteEntity.addScoreboardTag("tanukimc_remote_write");
                        } catch (IllegalStateException expected) {
                            tagDenied = true;
                        }
                        try {
                            remoteEntity.setHealth(remoteEntity.getHealth());
                        } catch (IllegalStateException expected) {
                            healthDenied = true;
                        }
                        try {
                            remoteEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.SPEED, 20, 0));
                        } catch (IllegalStateException expected) {
                            potionDenied = true;
                        }
                        var remoteLiving = (net.minecraft.world.entity.LivingEntity) remoteHandle;
                        try {
                            remoteLiving.setHealth(remoteLiving.getHealth());
                        } catch (IllegalStateException expected) {
                            directHealthDenied = true;
                        }
                        try {
                            remoteLiving.setAbsorptionAmount(0);
                        } catch (IllegalStateException expected) {
                            directAbsorptionDenied = true;
                        }
                        var effect = new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.POISON, 20);
                        try {
                            remoteLiving.addEffect(effect);
                        } catch (IllegalStateException expected) {
                            directEffectDenied = true;
                        }
                        try {
                            remoteLiving.forceAddEffect(effect, null);
                        } catch (IllegalStateException expected) {
                            directForceEffectDenied = true;
                        }
                        try {
                            remoteLiving.removeAllEffects();
                        } catch (IllegalStateException expected) {
                            directRemoveEffectsDenied = true;
                        }
                    }
                    try {
                        this.adjacentEntity.teleport(remoteLocation);
                    } catch (IllegalStateException expected) {
                        teleportDenied = true;
                    }
                    if (removalDenied && directRemovalDenied && causeRemovalDenied && positionDenied && rawPositionDenied
                        && velocityDenied && directVelocityDenied && rotationDenied && directRotationDenied
                        && bukkitRemoteVehicleDenied && bukkitRemotePassengerDenied && directRemoteVehicleDenied
                        && directRemotePassengerDenied && fireDenied && persistentDenied && gravityDenied && poseDenied
                        && tagDenied && healthDenied && potionDenied && directHealthDenied && directAbsorptionDenied
                        && directEffectDenied && directForceEffectDenied && directRemoveEffectsDenied && teleportDenied) {
                        this.remoteEntityGuardExercised.set(true);
                    } else {
                        this.failures.incrementAndGet();
                    }
                }
            }
            probe(world, 15);
            this.adjacentRuns.incrementAndGet();
        } finally {
            this.adjacentActive.decrementAndGet();
        }
    }

    private void probeRemotePlayer(Location destination) {
        var remote = Bukkit.getOnlinePlayers().stream()
            .filter(player -> !Bukkit.isOwnedByCurrentRegion(player)).findFirst().orElse(null);
        if (remote == null) {
            return;
        }
        if (!this.remotePlayerProbeAttempted.compareAndSet(false, true)) {
            return;
        }
        var denied = new java.util.ArrayList<String>();
        try { remote.teleport(destination); } catch (IllegalStateException expected) { denied.add("teleport"); }
        try { remote.setRotation(0, 0); } catch (IllegalStateException expected) { denied.add("rotation"); }
        try { remote.setGameMode(remote.getGameMode()); } catch (IllegalStateException expected) { denied.add("gameMode"); }
        try { remote.giveExp(0); } catch (IllegalStateException expected) { denied.add("experience"); }
        try { remote.setWalkSpeed(remote.getWalkSpeed()); } catch (IllegalStateException expected) { denied.add("walkSpeed"); }
        try {
            var craft = (org.bukkit.craftbukkit.entity.CraftPlayer) remote;
            craft.setRealHealth(craft.getHealth());
        } catch (IllegalStateException expected) { denied.add("realHealth"); }
        if (denied.size() == 6) {
            this.remotePlayerGuardExercised.set(true);
        } else {
            this.failures.incrementAndGet();
            getLogger().warning("Remote player guard denied=" + denied + " player=" + remote.getName());
        }
    }

    private void probe(World world, int chunkX) {
        AtomicInteger active = counter("active");
        this.maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
        try {
            if (this.withMekanism && this.mekanismTicker != null
                && (chunkX == 15 || chunkX == 128 || chunkX == 256 || chunkX == 384)) {
                var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                var bukkitPos = new net.minecraft.core.BlockPos(chunkX * 16 + 12, 112, 14);
                if (this.mekanismBukkitMaterial != null && this.mekanismBukkitPositions.add(bukkitPos)) {
                    world.getBlockAt(bukkitPos.getX(), bukkitPos.getY(), bukkitPos.getZ()).setType(this.mekanismBukkitMaterial);
                    if (level.getBlockEntity(bukkitPos) == null) {
                        this.failures.incrementAndGet();
                    }
                }
                var pos = new net.minecraft.core.BlockPos(chunkX * 16 + 9, 112, 14);
                var tile = level.getBlockEntity(pos);
                if (tile != null) {
                    try {
                        int sample = this.mekanismTicker.getInt(tile);
                        Integer previous = this.mekanismTickSamples.put(chunkX, sample);
                        if (previous != null && previous != sample) {
                            this.mekanismTickRegions.add(chunkX);
                        }
                        var bukkitTile = level.getBlockEntity(bukkitPos);
                        if (bukkitTile != null) {
                            int bukkitSample = this.mekanismTicker.getInt(bukkitTile);
                            Integer bukkitPrevious = this.mekanismBukkitSamples.put(chunkX, bukkitSample);
                            if (bukkitPrevious != null && bukkitPrevious != bukkitSample) {
                                this.mekanismBukkitTickRegions.add(chunkX);
                            }
                        }
                    } catch (ReflectiveOperationException ex) {
                        this.failures.incrementAndGet();
                        getLogger().log(java.util.logging.Level.SEVERE, "Mekanism tick probe failed", ex);
                    }
                }
            }
            if (this.withCreate && (chunkX == 15 || chunkX == 128 || chunkX == 256 || chunkX == 384)
                && this.createPlacementRegions.add(chunkX)) {
                var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                var cogId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "cogwheel");
                var motorId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "creative_motor");
                var cog = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(cogId).orElseThrow();
                var motor = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(motorId).orElseThrow();
                var motorState = motor.defaultBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.EAST);
                var cogState = cog.defaultBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X);
                this.createPlacementMax.accumulateAndGet(this.createPlacementActive.incrementAndGet(), Math::max);
                try {
                    for (int z = 0; z < 16; z += 2) {
                        var motorPos = new net.minecraft.core.BlockPos(chunkX * 16 + 12, 107, z);
                        var cogPos = motorPos.east();
                        level.setBlock(cogPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        level.setBlock(motorPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        level.setBlock(motorPos, motorState, 3);
                        level.setBlock(cogPos, cogState, 3);
                        if (level.getBlockEntity(motorPos) != null && level.getBlockEntity(cogPos) != null) {
                            this.createPlacedPairs.incrementAndGet();
                        } else {
                            this.failures.incrementAndGet();
                        }
                        Thread.sleep(2L);
                    }
                } finally {
                    this.createPlacementActive.decrementAndGet();
                }
                if (this.createBukkitCogMaterial != null) {
                    var motorPos = new net.minecraft.core.BlockPos(chunkX * 16 + 14, 104, 12);
                    var cogPos = motorPos.above();
                    level.setBlock(cogPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(motorPos, motor.defaultBlockState().setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.UP), 3);
                    world.getBlockAt(cogPos.getX(), cogPos.getY(), cogPos.getZ()).setType(this.createBukkitCogMaterial);
                    if (level.getBlockState(cogPos).is(cog) && level.getBlockEntity(cogPos) != null) {
                        this.createBukkitPlacedRegions.add(chunkX);
                    } else {
                        this.failures.incrementAndGet();
                    }
                }
                if (this.createBukkitCogItemMaterial != null) {
                    var chestBlock = world.getBlockAt(chunkX * 16 + 14, 110, 12);
                    if (chestBlock.getType() == org.bukkit.Material.CHEST) {
                        this.createExistingChestRegions.incrementAndGet();
                        var saved = ((org.bukkit.block.Chest) chestBlock.getState()).getBlockInventory().getItem(0);
                        if (saved != null && saved.getType() == this.createBukkitCogItemMaterial && saved.getAmount() == 3) {
                            this.createInventoryPersistedRegions.add(chunkX);
                        } else {
                            this.failures.incrementAndGet();
                        }
                    }
                    chestBlock.setType(org.bukkit.Material.CHEST);
                    var inventory = ((org.bukkit.block.Chest) chestBlock.getState()).getBlockInventory();
                    inventory.setItem(0, new org.bukkit.inventory.ItemStack(this.createBukkitCogItemMaterial, 3));
                    var stored = inventory.getItem(0);
                    if (stored != null && stored.getType() == this.createBukkitCogItemMaterial && stored.getAmount() == 3
                        && org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(stored).getItem() == cog.asItem()) {
                        this.createInventoryRegions.add(chunkX);
                    } else {
                        this.failures.incrementAndGet();
                    }
                }
            }
            if (chunkX == 128 && this.blockEntityOwnershipChecked.compareAndSet(false, true)) {
                var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                var localPos = new net.minecraft.core.BlockPos(128 * 16 + 8, 104, 10);
                var remotePos = new net.minecraft.core.BlockPos(384 * 16 + 8, 104, 10);
                if (level.getBlockEntity(localPos) == null || level.getBlockEntity(remotePos) != null) {
                    this.failures.incrementAndGet();
                    getLogger().warning("Block entity region ownership check failed");
                }
            }
            if (this.withCreate && this.createTickCounter != null
                && (chunkX == 15 || chunkX == 128 || chunkX == 256 || chunkX == 384)) {
                var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                try {
                    int poweredPairs = 0;
                    for (int z = 0; z < 16; z += 2) {
                        var cogPos = new net.minecraft.core.BlockPos(chunkX * 16 + 13, 107, z);
                        var cog = level.getBlockEntity(cogPos);
                        if (cog != null && ((Number) this.createSpeedGetter.invoke(cog)).floatValue() != 0.0F) {
                            poweredPairs++;
                        }
                    }
                    if (poweredPairs == 8) {
                        this.createPlacementPoweredRegions.add(chunkX);
                    }
                    var bukkitCog = level.getBlockEntity(new net.minecraft.core.BlockPos(chunkX * 16 + 14, 105, 12));
                    if (bukkitCog != null && ((Number) this.createSpeedGetter.invoke(bukkitCog)).floatValue() != 0.0F) {
                        this.createBukkitPoweredRegions.add(chunkX);
                    }
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    if (this.createTickSampleFailed.compareAndSet(false, true)) {
                        this.failures.incrementAndGet();
                        getLogger().log(java.util.logging.Level.SEVERE, "Create placement probe failed", ex);
                    }
                }
                var createPos = new net.minecraft.core.BlockPos(chunkX * 16 + 11, 104, 10);
                var createEntity = level.getBlockEntity(createPos);
                if (createEntity != null) {
                    try {
                        int sample = this.createTickCounter.getInt(createEntity);
                        Integer previous = this.createCounterSamples.put(chunkX, sample);
                        if (previous != null && previous != sample) {
                            this.createTickRegions.add(chunkX);
                        }
                        if (((Number) this.createSpeedGetter.invoke(createEntity)).floatValue() != 0.0F) {
                            this.createPoweredRegions.add(chunkX);
                        }
                    } catch (ReflectiveOperationException | RuntimeException ex) {
                        if (this.createTickSampleFailed.compareAndSet(false, true)) {
                            this.failures.incrementAndGet();
                            getLogger().log(java.util.logging.Level.SEVERE, "Create tick probe failed", ex);
                        }
                    }
                } else {
                    counter("createMissingSamples").incrementAndGet();
                }
            }
            if (this.splitPlayerRegions && chunkX == 256 && !this.remotePlayerProbeAttempted.get()) {
                probeRemotePlayer(new Location(world, chunkX * 16 + 8, 120, 10));
            }
            if (Bukkit.getOnlinePlayers().size() >= (this.waitForClient ? 1 : 2)) {
                this.onlineRuns.incrementAndGet();
                if (this.withLuckPerms) {
                    for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                        player.hasPermission("tanukimc.threading.probe");
                        this.permissionChecks.incrementAndGet();
                    }
                }
            }
            this.threads.add(Thread.currentThread().getName());
            var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
            var random = level.getRandom();
            for (int sample = 0; sample < 64; sample++) {
                random.nextLong();
                if (!Double.isFinite(random.nextGaussian())) {
                    this.failures.incrementAndGet();
                }
                if (!this.subTicks.add(level.nextSubTickCount())) {
                    this.failures.incrementAndGet();
                }
            }
            if (!Bukkit.isOwnedByCurrentRegion(world, chunkX, 0)) {
                this.failures.incrementAndGet();
            }
            if (chunkX == 0 && this.chunkLoadChecked.compareAndSet(false, true)) {
                if (this.storedChunkX != null && this.storedChunkZ != null) {
                    world.getChunkAtAsync(this.storedChunkX, this.storedChunkZ, false).whenComplete((stored, failure) -> {
                        if (failure != null || stored == null || stored.getX() != this.storedChunkX || stored.getZ() != this.storedChunkZ) {
                            this.failures.incrementAndGet();
                            getLogger().log(java.util.logging.Level.SEVERE, "Stored chunk load probe failed", failure);
                        } else {
                            this.storedChunkLoaded.set(true);
                        }
                    });
                }
                world.getChunkAtAsync(4127, 4127, false).whenComplete((chunk, failure) -> {
                    if (failure != null || chunk != null) {
                        this.failures.incrementAndGet();
                        getLogger().log(java.util.logging.Level.SEVERE, "Absent chunk load probe failed", failure);
                    } else {
                        this.asyncChunkLoads.incrementAndGet();
                    }
                });
                world.getChunkAtAsync(4096, 4096, true).whenComplete((chunk, failure) -> {
                    if (failure != null || chunk == null || chunk.getX() != 4096 || chunk.getZ() != 4096) {
                        this.failures.incrementAndGet();
                        getLogger().log(java.util.logging.Level.SEVERE, "Generated chunk load probe failed", failure);
                    } else {
                        this.asyncChunkLoads.incrementAndGet();
                    }
                });
                if (com.mohistmc.youer.threading.ThreadedServer.owns(world, 512, 0)) {
                    this.failures.incrementAndGet();
                }
                if (level.getChunk(512, 0) == null) {
                    this.failures.incrementAndGet();
                } else {
                    this.chunkLoadRuns.incrementAndGet();
                }
            }
            if (counter("worldDepth").get() != 0) {
                counter("overlaps").incrementAndGet();
            }
            // This API waits for the main thread, exercising progress while the barrier waits.
            world.setFullTime(6000L);
            this.bridges.incrementAndGet();
            if (this.mainTaskScheduled.compareAndSet(false, true)) {
                AtomicBoolean workerActive = new AtomicBoolean(true);
                net.minecraft.server.MinecraftServer.getServer().scheduleOnMain(() -> {
                    if (workerActive.get()) {
                        this.mainTaskOverlaps.incrementAndGet();
                    }
                    this.mainTaskRuns.incrementAndGet();
                });
                Thread.sleep(20L);
                workerActive.set(false);
            }
            Thread.sleep(20L);
            if (counter("worldDepth").get() != 0) {
                counter("overlaps").incrementAndGet();
            }
        } catch (Throwable throwable) {
            this.failures.incrementAndGet();
            getLogger().log(java.util.logging.Level.SEVERE, "Probe callback failed", throwable);
        } finally {
            active.decrementAndGet();
        }
    }

    private void finish() {
        this.finished = true;
        boolean passed = this.failures.get() == 0 && counter("overlaps").get() == 0
            && this.mainTaskRuns.get() == 1 && this.mainTaskOverlaps.get() == 0
            && this.chunkLoadRuns.get() == 1
            && this.asyncChunkLoads.get() == 2
            && (this.storedChunkX == null || this.storedChunkLoaded.get())
            && (this.splitPlayerRegions || this.remoteGuardExercised.get())
            && (this.splitPlayerRegions || this.remoteEntityGuardExercised.get())
            && this.blockEntityOwnershipChecked.get()
            && (!this.splitPlayerRegions || this.remotePlayerGuardExercised.get())
            && this.localRideChecked.get()
            && this.localEffectChecked.get()
            && (!this.withLithium || this.lithiumLoaded)
            && (!this.withFerriteCore || this.ferriteCoreLoaded)
            && (!this.withModernFix || this.modernFixLoaded)
            && (!this.withMekanism || this.mekanismLoaded
                && this.mekanismPositions.size() == 4 && this.mekanismTickRegions.size() == 4
                && this.mekanismBukkitPositions.size() == 4 && this.mekanismBukkitTickRegions.size() == 4)
            && (!this.withLuckPerms || this.permissionChecks.get() == this.bridges.get() * (this.waitForClient ? 1 : 2))
            && (!this.withWorldEdit || this.worldEditBlocks == 4 && this.worldEditRegionEdits.size() == 4)
            && (!this.withCreate || this.createPositions.size() == 4
                && this.createMotorPositions.size() == 4
                && this.createTickRegions.size() == 4
                && this.createPoweredRegions.size() == 4
                && this.createPlacementRegions.size() == 4 && this.createPlacedPairs.get() == 32
                && this.createPlacementMax.get() > 1
                && this.createPlacementPoweredRegions.size() == 4
                && this.createBukkitPlacedRegions.size() == 4 && this.createBukkitPoweredRegions.size() == 4
                && this.createInventoryRegions.size() == 4
                && this.createExistingChestRegions.get() == this.createInventoryPersistedRegions.size()
                && this.createBorderPlaced.get() && this.createBorderPowered.get()
                && counter("createMissingSamples").get() == 0
                && this.createPositions.stream().allMatch(pos -> {
                    var blockEntity = Bukkit.getWorlds().getFirst();
                    return ((org.bukkit.craftbukkit.CraftWorld) blockEntity).getHandle().getBlockEntity(pos) != null;
                }))
            && (!this.waitForPlayers || this.onlineRuns.get() == this.bridges.get()
                && counter("playerTicks").get() > 0 && counter("playerFailures").get() == 0
                && this.movedPlayers.contains("ThreadProbeA") && this.movedPlayers.contains("ThreadProbeB")
                && this.crossedPlayers.contains("ThreadProbeA") && this.crossedPlayers.contains("ThreadProbeB")
                && this.brokenPlayers.contains("ThreadProbeA") && this.brokenPlayers.contains("ThreadProbeB"))
            && (!this.splitPlayerRegions || this.splitPlayerCrossed
                && this.startingChunks.getOrDefault("ThreadProbeB", -1) == 64)
            && this.entityMoves.get() == 4 * RUNS_PER_REGION
            && this.retirementReady.get() && this.retirementAccepted.get() >= 32 && this.retirementAccepted.get() < 256
            && this.retirementCallbacks.get() == this.retirementAccepted.get() && this.retirementRuns.get() == 0
            && this.movingEntities.stream().allMatch(entity -> entity.isValid()
                && entity.getWorld().getEntity(entity.getUniqueId()) != null)
            && counter("entityFailures").get() == 0 && counter("entityMax").get() > 1
            && counter("entityChunk15").get() > 0 && counter("entityChunk128").get() > 0
            && counter("entityChunk256").get() > 0 && counter("entityChunk384").get() > 0
            && this.blockEvents != null && this.blockEvents.passed()
            && this.fluidTicks != null && this.fluidTicks.passed()
            && counter("scheduledTicks").get() == 4 && counter("scheduledFailures").get() == 0
            && counter("scheduledMax").get() > 1
            && counter("randomFailures").get() == 0 && counter("randomMax").get() > 1
            && counter("randomChunk15").get() > 0 && counter("randomChunk128").get() > 0
            && counter("randomChunk256").get() > 0 && counter("randomChunk384").get() > 0
            && counter("beFailures").get() == 0 && counter("beMax").get() > 1
            && counter("beChunk15").get() > 0 && counter("beChunk128").get() > 0
            && counter("beChunk256").get() > 0 && counter("beChunk384").get() > 0
            && counter("worldTicks").get() > 0 && this.maximum.get() > 1 && this.threads.size() > 1
            && this.subTicks.size() == (REGIONS * (RUNS_PER_REGION + 1) + ADJACENT_STREAMS * RUNS_PER_REGION) * 64
            && this.bridges.get() == REGIONS * (RUNS_PER_REGION + 1) + ADJACENT_STREAMS * RUNS_PER_REGION && this.generatorCases == 2;
        int mekanismCleaned = 0;
        if (this.withMekanism) {
            var level = ((org.bukkit.craftbukkit.CraftWorld) Bukkit.getWorlds().getFirst()).getHandle();
            for (var pos : this.mekanismPositions) {
                if (level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3)
                    && level.getBlockEntity(pos) == null) {
                    mekanismCleaned++;
                }
            }
            for (var pos : this.mekanismBukkitPositions) {
                if (level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3)
                    && level.getBlockEntity(pos) == null) {
                    mekanismCleaned++;
                }
            }
            passed &= mekanismCleaned == 8;
        }
        String lavaStates = this.fluidTicks == null ? "-" : this.fluidTicks.lavaStates();
        String lavaQueued = this.fluidTicks == null ? "-" : this.fluidTicks.queuedLava();
        int fluidCleaned = this.fluidTicks == null ? 0 : this.fluidTicks.cleanup();
        passed &= fluidCleaned == 4;
        getLogger().info("PHASE_PROBE " + (passed ? "PASS" : "FAIL")
            + " runs=" + this.runs + " children=" + this.children + " bridges=" + this.bridges
            + " threads=" + this.threads.size() + " maxConcurrent=" + this.maximum
            + " worldTicks=" + counter("worldTicks") + " overlaps=" + counter("overlaps")
            + " chunkLoadRuns=" + this.chunkLoadRuns + " asyncChunkLoads=" + this.asyncChunkLoads + " storedChunkLoaded=" + this.storedChunkLoaded + " createBlocks=" + this.createPositions.size()
            + " createTickRegions=" + this.createTickRegions.size()
            + " createPoweredRegions=" + this.createPoweredRegions.size()
            + " createPlacedPairs=" + this.createPlacedPairs.get()
            + " createPlacementMax=" + this.createPlacementMax.get()
            + " createPlacementPoweredRegions=" + this.createPlacementPoweredRegions.size()
            + " createBukkitPlacedRegions=" + this.createBukkitPlacedRegions.size()
            + " createBukkitPoweredRegions=" + this.createBukkitPoweredRegions.size()
            + " createInventoryRegions=" + this.createInventoryRegions.size()
            + " createInventoryPersistedRegions=" + this.createInventoryPersistedRegions.size()
            + " createBorderPlaced=" + this.createBorderPlaced.get() + " createBorderPowered=" + this.createBorderPowered.get()
            + " createSamples=" + this.createCounterSamples + " createMissingSamples=" + counter("createMissingSamples").get()
            + " remoteGuardExercised=" + this.remoteGuardExercised.get()
            + " remoteEntityGuardExercised=" + this.remoteEntityGuardExercised.get()
            + " blockEntityOwnershipChecked=" + this.blockEntityOwnershipChecked.get()
            + " remotePlayerGuardExercised=" + this.remotePlayerGuardExercised.get()
            + " localRideChecked=" + this.localRideChecked.get()
            + " localEffectChecked=" + this.localEffectChecked.get()
            + " lithiumLoaded=" + this.lithiumLoaded
            + " ferriteCoreLoaded=" + this.ferriteCoreLoaded
            + " modernFixLoaded=" + this.modernFixLoaded
            + " mekanismLoaded=" + this.mekanismLoaded + " mekanismTickRegions=" + this.mekanismTickRegions.size()
            + " mekanismBukkitTickRegions=" + this.mekanismBukkitTickRegions.size()
            + " mekanismCleaned=" + mekanismCleaned
            + " permissionChecks=" + this.permissionChecks
            + " worldEditBlocks=" + this.worldEditBlocks
            + " worldEditRegionEdits=" + this.worldEditRegionEdits.size()
            + " mainTaskRuns=" + this.mainTaskRuns + " mainTaskOverlaps=" + this.mainTaskOverlaps
            + " scheduledTicks=" + counter("scheduledTicks") + " scheduledFailures=" + counter("scheduledFailures")
            + " scheduledChunks=" + counter("scheduledChunk15") + "," + counter("scheduledChunk128")
                + "," + counter("scheduledChunk256") + "," + counter("scheduledChunk384")
            + " scheduledState=" + this.scheduledState()
            + " scheduledMax=" + counter("scheduledMax")
            + " randomMax=" + counter("randomMax") + " randomFailures=" + counter("randomFailures")
            + " randomChunks=" + counter("randomChunk15") + "," + counter("randomChunk128")
                + "," + counter("randomChunk256") + "," + counter("randomChunk384")
            + " beMax=" + counter("beMax") + " beFailures=" + counter("beFailures")
            + " beChunks=" + counter("beChunk15") + "," + counter("beChunk128")
                + "," + counter("beChunk256") + "," + counter("beChunk384")
            + " subTicks=" + this.subTicks.size()
            + " failures=" + this.failures + " generators=" + this.generatorCases + " adjacentRuns=" + this.adjacentRuns
            + " entityMoves=" + this.entityMoves
            + " cleanedProbeEntities=" + this.cleanedProbeEntities
            + " retirement=" + this.retirementCallbacks + "/" + this.retirementAccepted + "/" + this.retirementRuns
            + " entityMax=" + counter("entityMax") + " entityFailures=" + counter("entityFailures")
            + " entityChunks=" + counter("entityChunk15") + "," + counter("entityChunk128")
                + "," + counter("entityChunk256") + "," + counter("entityChunk384")
            + " onlineRuns=" + this.onlineRuns + " movedPlayers=" + this.movedPlayers.size()
            + " splitPlayerCrossed=" + this.splitPlayerCrossed
            + " crossedPlayers=" + this.crossedPlayers.size()
            + " brokenPlayers=" + this.brokenPlayers.size()
            + " brokenNames=" + this.brokenPlayers
            + " playerLocations=" + Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().startsWith("ThreadProbe"))
                .map(player -> player.getName() + ":" + player.getLocation().getX())
                .toList()
            + " playerTicks=" + counter("playerTicks")
            + " playerFailures=" + counter("playerFailures")
            + " blockEvents=" + (this.blockEvents == null ? 0 : this.blockEvents.count())
            + " fluidChunks=" + (this.fluidTicks == null ? 0 : this.fluidTicks.count())
            + " fluidMax=" + (this.fluidTicks == null ? 0 : this.fluidTicks.maximum())
            + " lavaForms=" + (this.fluidTicks == null ? 0 : this.fluidTicks.formed())
            + " lavaMax=" + (this.fluidTicks == null ? 0 : this.fluidTicks.lavaMaximum())
            + " lavaFlows=" + (this.fluidTicks == null ? 0 : this.fluidTicks.lavaFlows())
            + " lavaStates=" + lavaStates
            + " fluidY=" + (this.fluidTicks == null ? -1 : this.fluidTicks.sourceY())
            + " lavaQueued=" + lavaQueued
            + " fluidReclaimed=" + (this.fluidTicks == null ? 0 : this.fluidTicks.reclaimed())
            + " fluidCleaned=" + fluidCleaned
            + " fluidFailures=" + (this.fluidTicks == null ? 0 : this.fluidTicks.failures()));
        this.probeEntities.forEach(ArmorStand::remove);
        if (!this.waitForPlayers && !this.waitForClient) {
            Bukkit.shutdown();
        }
    }

    private String scheduledState() {
        var level = ((org.bukkit.craftbukkit.CraftWorld) Bukkit.getWorlds().getFirst()).getHandle();
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("youer_phase_probe", "scheduled_probe");
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(id).orElseThrow();
        StringBuilder state = new StringBuilder();
        for (int chunkX : new int[] {15, 128, 256, 384}) {
            if (!state.isEmpty()) {
                state.append(',');
            }
            var pos = new net.minecraft.core.BlockPos(chunkX * 16 + 8, 68, 10);
            long chunk = net.minecraft.world.level.ChunkPos.asLong(chunkX, 0);
            state.append(chunkX).append(':')
                .append(level.getBlockTicks().hasScheduledTick(pos, block) ? 'Q' : '-')
                .append(level.areEntitiesLoaded(chunk) ? 'E' : '-')
                .append(level.getChunkSource().isPositionTicking(chunk) ? 'T' : '-')
                .append(level.getBlockState(pos).getBlock() == block ? 'B' : '-');
        }
        return state.toString();
    }

    private AtomicInteger counter(String name) {
        return (AtomicInteger) System.getProperties().computeIfAbsent("youer.phase." + name, key -> new AtomicInteger());
    }
}
