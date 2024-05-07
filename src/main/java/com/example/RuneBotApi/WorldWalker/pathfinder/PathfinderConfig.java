package com.example.RuneBotApi.WorldWalker.pathfinder;

import com.example.EthanApiPlugin.Collections.Equipment;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.RuneBotApi.WorldWalker.*;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class PathfinderConfig {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_ABOVE_GROUND_LEVEL_20 = new WorldArea(2944, 3680, 448, 448, 0);
    private static final WorldArea WILDERNESS_ABOVE_GROUND_LEVEL_30 = new WorldArea(2944, 3760, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);
    //todo: check that these are correct y values.
    private static final WorldArea WILDERNESS_UNDERGROUND_LEVEL_20 = new WorldArea(2944, 10075, 320, 442, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND_LEVEL_30 = new WorldArea(2944, 10155, 320, 442, 0);

    private final SplitFlagMap mapData;
    private final ThreadLocal<CollisionMap> map;
    private final Map<WorldPoint, List<Transport>> allTransports;
    @Getter
    private Map<WorldPoint, List<Transport>> transports;

    // Copy of transports with packed positions for the hotpath; lists are not copied and are the same reference in both maps
    @Getter
    private PrimitiveIntHashMap<List<Transport>> transportsPacked;

    private final Client client;
    private final RuneBotWalkerPlugin plugin;
    private final RuneBotWalkerConfig config;
    private final ClientThread clientThread;

    @Getter
    private long calculationCutoffMillis;
    @Getter
    private boolean avoidWilderness;
    private boolean useAgilityShortcuts,
        useGrappleShortcuts,
        useBoats,
        useCanoes,
        useCharterShips,
        useShips,
        useFairyRings,
        useGnomeGliders,
        useSpiritTrees,
        useTeleportationLevers,
        useTeleportationPortals;
    private ItemTransportSetting itemTransportSetting;
    private int agilityLevel;
    private int rangedLevel;
    private int strengthLevel;
    private int prayerLevel;
    private int woodcuttingLevel;
    private int magicLevel;
    private int recalculateDistance;
    private Map<Quest, QuestState> questStates = new HashMap<>();
    private Map<Integer, Integer> varbitValues = new HashMap<>();

    public PathfinderConfig(SplitFlagMap mapData, Map<WorldPoint, List<Transport>> transports, Client client,
                            RuneBotWalkerPlugin plugin, RuneBotWalkerConfig config, ClientThread clientThread) {
        this.mapData = mapData;
        this.map = ThreadLocal.withInitial(() -> new CollisionMap(this.mapData));
        this.allTransports = transports;
        this.transports = new HashMap<>(allTransports.size());
        this.transportsPacked = new PrimitiveIntHashMap<>(allTransports.size());
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.clientThread = clientThread;
        refresh();
    }

    public boolean isNear(WorldPoint location) {
        if (plugin.isStartPointSet() || client.getLocalPlayer() == null) {
            return true;
        }
        return recalculateDistance < 0 ||
                (client.isInInstancedRegion() ?
                        WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()) :
                        client.getLocalPlayer().getWorldLocation()).distanceTo2D(location) <= recalculateDistance;
    }


    public CollisionMap getMap() {
        return map.get();
    }

    public void refresh() {
        calculationCutoffMillis = config.calculationCutoff() * Constants.GAME_TICK_LENGTH;
        avoidWilderness = config.avoidWilderness();
        useAgilityShortcuts = config.useAgilityShortcuts();
        useGrappleShortcuts = config.useGrappleShortcuts();
        useBoats = config.useBoats();
        useCanoes = config.useCanoes();
        useCharterShips = config.useCharterShips();
        useShips = config.useShips();
        useFairyRings = config.useFairyRings();
        useSpiritTrees = config.useSpiritTrees();
        useGnomeGliders = config.useGnomeGliders();
        useTeleportationLevers = config.useTeleportationLevers();
        useTeleportationPortals = config.useTeleportationPortals();
        itemTransportSetting = config.playerItemTransportSetting();

        if (GameState.LOGGED_IN.equals(client.getGameState())) {
            agilityLevel = client.getBoostedSkillLevel(Skill.AGILITY);
            rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
            strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
            prayerLevel = client.getBoostedSkillLevel(Skill.PRAYER);
            woodcuttingLevel = client.getBoostedSkillLevel(Skill.WOODCUTTING);
            magicLevel = client.getBoostedSkillLevel(Skill.MAGIC);
            refreshTransportData();
        }
    }

    private boolean hasRuneAmount(int runeId, int amount) {
        return (client.getVarbitValue(Varbits.RUNE_POUCH_RUNE1) == runeId
                && client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT1) >= amount)
                || (client.getVarbitValue(Varbits.RUNE_POUCH_RUNE2) == runeId
                && client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT2) >= amount)
                || (client.getVarbitValue(Varbits.RUNE_POUCH_RUNE3) == runeId
                && client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT3) >= amount)
                || (client.getVarbitValue(Varbits.RUNE_POUCH_RUNE4) == runeId
                && client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT4) >= amount);
    }

    /** Updating spell transports */

    public void refreshSpellTransportData(@Nonnull WorldPoint location, int wildernessLevel) {
        clientThread.invoke(() -> {
            List<Widget> inventoryRunes = Inventory.search().matchesWildCardNoCase("* rune").result();
            List<Transport> spellTransports = allTransports.getOrDefault(null, new ArrayList<>());
            List<Transport> usableTransports = new ArrayList<>(spellTransports.size());

            for (Transport transport : spellTransports) {
                if (!transport.getTransportType().equals(Transport.TransportType.TELEPORTATION_SPELL)) {
                    continue;
                }

                boolean canCastSpell = true;
                int currentSpellbookVarbit = client.getVarbitValue(4070);
                Spellbook currentSpellbook = Spellbook.fromVarbit(currentSpellbookVarbit);

                String[] parts = transport.getDisplayInfo().split(" ");
                String[] subParts = parts[parts.length - 1].split(";");
                String spellbook = subParts[subParts.length - 1];
                for (String part : parts) {
                    String currentPart;
                    if (part.contains(";")) {
                        currentPart = part.split(";")[0];
                    } else {
                        currentPart = part;
                    }

                    String[] runeParts = currentPart.split("-");
                    int amount = Integer.parseInt(runeParts[0]);
                    for (Runes runes : Runes.values()) {
                        if (runes.name().equalsIgnoreCase(runeParts[1])) {
                            boolean foundMatch = inventoryRunes
                                    .stream()
                                    .noneMatch(widget -> Text.removeTags(widget.getName())
                                            .contains(runeParts[1]) && widget.getItemQuantity() >= amount);
                            if ((!hasRuneAmount(runes.getId(), amount) && !foundMatch)
                                    || currentSpellbook != Spellbook.fromString(spellbook)) {
                                canCastSpell = false;
                            }
                        }
                    }
                }

                if (canCastSpell && transport.getMaxWildernessLevel() >= wildernessLevel) {
                    usableTransports.add(transport);
                }
            }

            transports.put(location, usableTransports);
            transportsPacked.put(WorldPointUtil.packWorldPoint(location), usableTransports);
        });
    }

    /** Specialized method for only updating player-held item transports */
    public void refreshPlayerTransportData(@Nonnull WorldPoint location, int wildernessLevel) {
        //TODO: This just checks the player's inventory and equipment.
        // Later, bank items could be included, but the player will probably need to configure which items are considered
        var inventoryItems = Arrays.stream(new InventoryID[]{InventoryID.INVENTORY, InventoryID.EQUIPMENT})
                .map(client::getItemContainer)
                .filter(Objects::nonNull)
                .map(ItemContainer::getItems)
                .flatMap(Arrays::stream)
                .map(Item::getId)
                .filter(itemId -> itemId != -1)
                .collect(Collectors.toList());

        List<Transport> playerItemTransports = allTransports.getOrDefault(null, new ArrayList<>());
        List<Transport> usableTransports = new ArrayList<>(playerItemTransports.size());
        for (Transport transport : playerItemTransports) {
            boolean itemInInventory = transport.getItemRequirements().isEmpty() ||
                    transport.getItemRequirements().stream().anyMatch(inventoryItems::contains);

            //questStates and varbits cannot be checked in a non-main thread, so item transports' quests and varbits are cached in `refreshTransportData`

            if (useTransport(transport) && itemInInventory && transport.getMaxWildernessLevel() >= wildernessLevel) {
                usableTransports.add(transport);
            }
        }

        transports.put(location, usableTransports);
        transportsPacked.put(WorldPointUtil.packWorldPoint(location), usableTransports);
    }

    private void refreshTransportData() {
        if (!Thread.currentThread().equals(client.getClientThread())) {
            return; // Has to run on the client thread; data will be refreshed when path finding commences
        }

        useFairyRings &= !QuestState.NOT_STARTED.equals(getQuestState(Quest.FAIRYTALE_II__CURE_A_QUEEN));
        useGnomeGliders &= QuestState.FINISHED.equals(getQuestState(Quest.THE_GRAND_TREE));
        useSpiritTrees &= QuestState.FINISHED.equals(getQuestState(Quest.TREE_GNOME_VILLAGE));

        transports.clear();
        transportsPacked.clear();
        for (Map.Entry<WorldPoint, List<Transport>> entry : allTransports.entrySet()) {
            List<Transport> usableTransports = new ArrayList<>(entry.getValue().size());
            for (Transport transport : entry.getValue()) {
                for (Quest quest : transport.getQuests()) {
                    try {
                        questStates.put(quest, getQuestState(quest));
                    } catch (NullPointerException ignored) {
                    }
                }

                for(TransportVarbit varbitCheck : transport.getVarbits()) {
                    varbitValues.put(varbitCheck.getVarbitId(), client.getVarbitValue(varbitCheck.getVarbitId()));
                }

                if(entry.getKey() == null){
                    //null keys are for player-centered transports. They are added in refreshPlayerTransportData at pathfinding time.
                    //still need to get quest states for these transports while we're in the client thread though
                    continue;
                }

                if (useTransport(transport)) {
                    usableTransports.add(transport);
                }
            }

            WorldPoint point = entry.getKey();

            if(point != null) {
                transports.put(point, usableTransports);
                transportsPacked.put(WorldPointUtil.packWorldPoint(point), usableTransports);
            }
        }
    }
    public static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 || WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public static boolean isInWilderness(int packedPoint) {
        return WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_ABOVE_GROUND) == 0 || WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_UNDERGROUND) == 0;
    }

    public boolean avoidWilderness(int packedPosition, int packedNeightborPosition, boolean targetInWilderness) {
        return avoidWilderness && !isInWilderness(packedPosition) && isInWilderness(packedNeightborPosition) && !targetInWilderness;
    }

    public boolean isInLevel20Wilderness(int packedPoint) {
        return WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_ABOVE_GROUND_LEVEL_20) == 0 || WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_UNDERGROUND_LEVEL_20) == 0;
    }

    public boolean isInLevel30Wilderness(int packedPoint){
        return WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_ABOVE_GROUND_LEVEL_30) == 0 || WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_UNDERGROUND_LEVEL_30) == 0;
    }

    public QuestState getQuestState(Quest quest) {
        return quest.getState(client);
    }

    private boolean completedQuests(Transport transport) {
        for (Quest quest : transport.getQuests()) {
            if (!QuestState.FINISHED.equals(questStates.getOrDefault(quest, QuestState.NOT_STARTED))) {
                return false;
            }
        }
        return true;
    }

    private boolean varbitChecks(Transport transport) {
        for (TransportVarbit varbitCheck : transport.getVarbits()) {
            if (!varbitValues.get(varbitCheck.getVarbitId()).equals(varbitCheck.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean useFairyRing() {
        return client.getVarbitValue(Varbits.DIARY_LUMBRIDGE_ELITE) >= 1
                || !Inventory.search()
                .filter(item ->
                        WildcardMatcher.matches("dramen staff*", Text.removeTags(item.getName()).toLowerCase())
                                || WildcardMatcher.matches("lunar staff*", Text.removeTags(item.getName()).toLowerCase())
                ).empty()
                || !Equipment.search()
                .filter(item ->
                        WildcardMatcher.matches("dramen staff*", Text.removeTags(item.getName()).toLowerCase())
                                || WildcardMatcher.matches("lunar staff*", Text.removeTags(item.getName()).toLowerCase())
                ).empty();
    }


    private boolean useTransport(Transport transport) {
        final int transportAgilityLevel = transport.getRequiredLevel(Skill.AGILITY);
        final int transportRangedLevel = transport.getRequiredLevel(Skill.RANGED);
        final int transportStrengthLevel = transport.getRequiredLevel(Skill.STRENGTH);
        final int transportPrayerLevel = transport.getRequiredLevel(Skill.PRAYER);
        final int transportWoodcuttingLevel = transport.getRequiredLevel(Skill.WOODCUTTING);

        final boolean isAgilityShortcut = transport.isAgilityShortcut();
        final boolean isGrappleShortcut = transport.isGrappleShortcut();
        final boolean isBoat = transport.isBoat();
        final boolean isCanoe = transport.isCanoe();
        final boolean isCharterShip = transport.isCharterShip();
        final boolean isShip = transport.isShip();
        final boolean isFairyRing = transport.isFairyRing();
        final boolean isGnomeGlider = transport.isGnomeGlider();
        final boolean isSpiritTree = transport.isSpiritTree();
        final boolean isTeleportationLever = transport.isTeleportationLever();
        final boolean isTeleportationPortal = transport.isTeleportationPortal();
        final boolean isPrayerLocked = transportPrayerLevel > 1;
        final boolean isQuestLocked = transport.isQuestLocked();
        final boolean isPlayerItem = transport.isPlayerItem();

        if (isAgilityShortcut) {
            if (!useAgilityShortcuts || agilityLevel < transportAgilityLevel) {
                return false;
            }

            if (isGrappleShortcut && (!useGrappleShortcuts || rangedLevel < transportRangedLevel || strengthLevel < transportStrengthLevel)) {
                return false;
            }
        }

        if (isBoat && !useBoats) {
            return false;
        }

        if (isCanoe && (!useCanoes || woodcuttingLevel < transportWoodcuttingLevel)) {
            return false;
        }

        if (isCharterShip && !useCharterShips) {
            return false;
        }

        if (isShip && !useShips) {
            return false;
        }

        if (isFairyRing && !useFairyRings && !useFairyRing()) {
            return false;
        }

        if (isGnomeGlider && !useGnomeGliders) {
            return false;
        }

        if (isSpiritTree && !useSpiritTrees) {
            return false;
        }

        if (isTeleportationLever && !useTeleportationLevers) {
            return false;
        }

        if (isTeleportationPortal && !useTeleportationPortals) {
            return false;
        }

        if (isPrayerLocked && prayerLevel < transportPrayerLevel) {
            return false;
        }

        if (isQuestLocked && !completedQuests(transport)) {
            return false;
        }

        if (isPlayerItem) {
            if (itemTransportSetting == ItemTransportSetting.None) {
                return false;
            }
        }

        if (!varbitChecks(transport)) {
            return false;
        }

        return true;
    }
}
