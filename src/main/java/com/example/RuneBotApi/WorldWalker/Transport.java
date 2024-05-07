package com.example.RuneBotApi.WorldWalker;

import com.example.EthanApiPlugin.PathFinding.GlobalCollisionMap;
import com.google.common.base.Strings;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class represents a travel point between two WorldPoints.
 */
public class Transport {

    // TODO:
    //  We need to make a TransportRequirements class to hold different requirements to un-clutter this class
    //  It's low priority, but this should be a good reminder.  This is because different Transports might require
    //  different types of requirements.   It will be required before making any switch from TSV to another format

    /**
     *  Before touching this file, or modifying any of the transports data.
     *
     *  I highly recommend installing the tsv editor plugin for Intellij.  You can just open a tsv file
     *  and it should tell you at the top of the file that there is a plugin available.
     *
     *  Adding transports is easy in the text editor.  Separate fields by tabs.  Some require special delimiters.
     *  Overally it is a bad design, and we will eventually make a switch to JSON formatted files for transports.
     */

    /** The starting point of this transport */
    @Getter
    private final WorldPoint origin;

    /** The ending point of this transport */
    private final WorldPoint destination;

    /** The skill levels required to use this transport */
    private final int[] skillLevels = new int[Skill.values().length];

    /** The quests required to use this transport */
    @Getter
    private List<Quest> quests = new ArrayList<>();

    /** The ids of items required to use this transport. If the player has **any** matching item, this transport is valid */
    @Getter
    private List<Integer> itemRequirements = new ArrayList<>();

    /** Whether the transport is an agility shortcut */
    @Getter
    private boolean isAgilityShortcut;

    /** Whether the transport is a crossbow grapple shortcut */
    @Getter
    private boolean isGrappleShortcut;

    /** Whether the transport is a boat */
    @Getter
    private boolean isBoat;

    /** Whether the transport is a canoe */
    @Getter
    private boolean isCanoe;

    /** Whether the transport is a charter ship */
    @Getter
    private boolean isCharterShip;

    /** Whether the transport is a ship */
    @Getter
    private boolean isShip;

    /** Whether the transport is a fairy ring */
    @Getter
    private boolean isFairyRing;

    /** Whether the transport is a gnome glider */
    @Getter
    private boolean isGnomeGlider;

    /** Whether the transport is a spirit tree */
    @Getter
    private boolean isSpiritTree;

    /** Whether the transport is a teleportation lever */
    @Getter
    private boolean isTeleportationLever;

    /** Whether the transport is a teleportation portal */
    @Getter
    private boolean isTeleportationPortal;

    /** Whether the transport is a player-held item */
    @Getter
    private boolean isPlayerItem;

    /** Whether the transport is a teleportation spell */
    @Getter
    private boolean isSpell;

    /** For playerItems, if the item is equippable */
    @Getter
    private final boolean isEquippable;

    /** The additional travel time */
    @Getter
    private int wait;

    /** Info to display for this transport. For spirit trees, fairy rings, and others, this is the destination option to pick. */
    @Getter
    private String displayInfo;

    /** If this is an item transport, this tracks if it is consumable (as opposed to having infinite uses) */
    @Getter
    private final boolean isConsumable;

    /** If this is an item transport, this is the maximum wilderness level that it can be used in */
    @Getter
    private final int maxWildernessLevel;

    /** Any varbits to check for the transport to be valid. All must pass for a transport to be valid */
    @Getter
    private final List<TransportVarbit> varbits;

    /** The Type of Transport this is */
    @Getter
    private TransportType transportType;

    /** An action associated with the Transport - if any */
    @Getter
    private String action;

    /** The Object or NPC "Interactable" name - if any */
    @Getter
    private String interactableName;

    /** Optional WorldArea to find the player in after using a transport - currently only supported by spells.  Null otherwise */
    @Getter
    private WorldArea destinationArea = null;

    Transport(final WorldPoint origin, final WorldPoint destination) {
        this.origin = origin;
        this.destination = destination;
        this.transportType = TransportType.TRANSPORT;
        this.action = "";
        this.interactableName = "";
        this.isConsumable = false;
        this.maxWildernessLevel = -1;
        this.varbits = new ArrayList<>();
        this.isEquippable = false;
    }

    Transport(final String line, TransportType transportType) {
        final String DELIM = " ";
        final String EXTRA_DELIM = ";";

        String[] parts = line.split("\t");

        String[] parts_origin = parts[0].split(DELIM);
        String[] parts_destination = parts[1].split(DELIM);

        this.transportType = transportType;

        if (TransportType.TELEPORTATION_SPELL.equals(transportType)) {
//            if (parts_destination.length != 5) {
//                TODO
//                  Need to handle Respawn Teleport here
//            }

            this.origin = null;
            this.destination = new WorldPoint(
                    Integer.parseInt(parts_destination[0]),
                    Integer.parseInt(parts_destination[1]),
                    Integer.parseInt(parts_destination[2]));
            this.isConsumable = false;
            this.maxWildernessLevel = 20;
            this.varbits = new ArrayList<>();
            this.isEquippable = false;
            this.destinationArea = new WorldArea(this.destination,
                    Integer.parseInt(parts_destination[3]),
                    Integer.parseInt(parts_destination[4]));

            initializeSpellTransportData(parts);
            return;
        }

        if(!TransportType.TELEPORTATION_ITEM.equals(transportType)) {
            origin = new WorldPoint(
                    Integer.parseInt(parts_origin[0]),
                    Integer.parseInt(parts_origin[1]),
                    Integer.parseInt(parts_origin[2]));
        } else {
            origin = null;
        }

        destination = new WorldPoint(
            Integer.parseInt(parts_destination[0]),
            Integer.parseInt(parts_destination[1]),
            Integer.parseInt(parts_destination[2]));

        if (parts.length >= 3 && !parts[2].isEmpty()
            && transportType != TransportType.FAIRY_RING
            && transportType != TransportType.TELEPORTATION_ITEM) {
            String[] menuParts = parts[2].split(EXTRA_DELIM);
            this.action = menuParts[0];
            this.interactableName = menuParts[1];
        }

        // Skill requirements
        if (parts.length >= 4 && !parts[3].isEmpty()) {
            String[] skillRequirements = parts[3].split(EXTRA_DELIM);

            for (String requirement : skillRequirements) {
                String[] levelAndSkill = requirement.split(DELIM);

                int level = Integer.parseInt(levelAndSkill[0]);
                String skillName = levelAndSkill[1];

                Skill[] skills = Skill.values();
                for (int i = 0; i < skills.length; i++) {
                    if (skills[i].getName().equals(skillName)) {
                        skillLevels[i] = level;
                        break;
                    }
                }
            }
        }

        itemRequirements = new ArrayList<>();
        //item requirements are currently only implemented for player-held item transports
        if(TransportType.TELEPORTATION_ITEM.equals(transportType)){
            for (String item : parts[4].split(DELIM)) {
                int itemId = Integer.parseInt(item);
                itemRequirements.add(itemId);
            }
        }

        // Quest requirements
        if (parts.length >= 6 && !parts[5].isEmpty()) {
            this.quests = findQuests(parts[5]);
        }

        // Additional travel time
        if (parts.length >= 7 && !parts[6].isEmpty()) {
            this.wait = Integer.parseInt(parts[6]);
        }

        // Destination
        if (parts.length >= 8 && !parts[7].isEmpty()) {
            this.displayInfo = parts[7];
        }

        //Consumable - for item transports
        this.isConsumable = parts.length >= 9 && parts[8].equals("T");

        //Wilderness level - for item transports
        if(parts.length >= 10 && !parts[9].isEmpty()) {
            this.maxWildernessLevel = Integer.parseInt(parts[9]);
        } else {
            this.maxWildernessLevel = -1;
        }

        this.isEquippable = parts.length >= 11 && parts[10].equals("T");

        this.varbits = new ArrayList<>();
        //Varbit check - all must evaluate to true
        if(parts.length >= 12 && !parts[11].isEmpty()) {
            for (String varbitCheck : parts[11].split(DELIM)) {
                var varbitParts = varbitCheck.split("=");
                int varbitId = Integer.parseInt(varbitParts[0]);
                int varbitValue = Integer.parseInt(varbitParts[1]);
                varbits.add(new TransportVarbit(varbitId, varbitValue));
            }
        }

        isAgilityShortcut = TransportType.AGILITY_SHORTCUT.equals(transportType);
        isGrappleShortcut = isAgilityShortcut && (getRequiredLevel(Skill.RANGED) > 1 || getRequiredLevel(Skill.STRENGTH) > 1);
        isBoat = TransportType.BOAT.equals(transportType);
        isCanoe = TransportType.CANOE.equals(transportType);
        isCharterShip = TransportType.CHARTER_SHIP.equals(transportType);
        isShip = TransportType.SHIP.equals(transportType);
        isGnomeGlider = TransportType.GNOME_GLIDER.equals(transportType);
        isSpiritTree = TransportType.SPIRIT_TREE.equals(transportType);
        isTeleportationLever = TransportType.TELEPORTATION_LEVER.equals(transportType);
        isTeleportationPortal = TransportType.TELEPORTATION_PORTAL.equals(transportType);
        isPlayerItem = TransportType.TELEPORTATION_ITEM.equals(transportType);
        isSpell = TransportType.TELEPORTATION_SPELL.equals(transportType);
    }

    private void initializeSpellTransportData(String[] parts) {
        // Max length is 7, most often 6.
        // Skip indexes 0, 1 because they are used already.

        this.action = "Cast"; // TODO: Needs to support teleport spells that are unlocked via diaries - this suffices for now.
        this.interactableName = parts[2];
        this.displayInfo = parts[3];


        // Skill requirements
        if (!parts[4].isBlank() && !parts[4].isEmpty()) {
            String[] skillRequirements = parts[4].split(";");

            for (String requirement : skillRequirements) {
                String[] levelAndSkill = requirement.split(" ");

                int level = Integer.parseInt(levelAndSkill[0]);

                Skill[] skills = Skill.values();
                for (int i = 0; i < skills.length; i++) {
                    if (skills[i].equals(Skill.MAGIC)) {
                        skillLevels[i] = level;
                        break;
                    }
                }
            }
        }

        // Display Info is used for Spellbook requirement
        if (!parts[5].isBlank() && !parts[5].isEmpty()) {
            this.displayInfo += "," + parts[5];
        }

        // Quest requirements, only if length >= 7 and not empty
        if (parts.length >= 7 && !parts[6].isEmpty() && !parts[6].isBlank()) {
            this.quests = findQuests(parts[6]);
        }

        // TODO:
        //  this.varbits can be used here for diary unlocks I think?  I have not looked into yet just yet.
    }

    public WorldPoint getDestination() {
        if (destination == null) {
            return destinationArea.toWorldPoint();
        }

        return destination;
    }

    /** The skill level required to use this transport */
    public int getRequiredLevel(Skill skill) {
        return skillLevels[skill.ordinal()];
    }

    /** Whether the transport has one or more quest requirements */
    public boolean isQuestLocked() {
        return !quests.isEmpty();
    }

    private static List<Quest> findQuests(String questNamesCombined) {
        String[] questNames = questNamesCombined.split(";");
        List<Quest> quests = new ArrayList<>();
        for (String questName : questNames) {
            for (Quest quest : Quest.values()) {
                if (quest.getName().equals(questName)) {
                    quests.add(quest);
                    break;
                }
            }
        }
        return quests;
    }

    private static void addItemTransports(Map<WorldPoint, List<Transport>> transports) {
        try {
            String s = new String(Util.readAllBytes(GlobalCollisionMap.class.getResourceAsStream("items.tsv")), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                Transport transport = new Transport(line, TransportType.TELEPORTATION_ITEM);
                transports.computeIfAbsent(null, k -> new ArrayList<>()).add(transport);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addSpellTransports(Map<WorldPoint, List<Transport>> transports) {
        try {
            String s = new String(Util.readAllBytes(GlobalCollisionMap.class.getResourceAsStream("teleport_spells.tsv")), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                Transport transport = new Transport(line, TransportType.TELEPORTATION_SPELL);
                transports.computeIfAbsent(null, k -> new ArrayList<>()).add(transport);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void addTransports(Map<WorldPoint, List<Transport>> transports, String path, TransportType transportType) {
        try {
            String s = new String(Util.readAllBytes(GlobalCollisionMap.class.getResourceAsStream(path)), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            List<String> fairyRingsQuestNames = new ArrayList<>();
            List<WorldPoint> fairyRings = new ArrayList<>();
            List<String> fairyRingCodes = new ArrayList<>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }

                if (TransportType.FAIRY_RING.equals(transportType)) {
                    String[] p = line.split("\t");
                    fairyRings.add(new WorldPoint(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                    fairyRingCodes.add(p.length >= 4 ? p[3].replaceAll("_", " ") : null);
                    fairyRingsQuestNames.add(p.length >= 7 ? p[6] : "");
                } else {
                    Transport transport = new Transport(line, transportType);
                    WorldPoint origin = transport.getOrigin();
                    transports.computeIfAbsent(origin, k -> new ArrayList<>()).add(transport);
                }
            }
            if (TransportType.FAIRY_RING.equals(transportType)) {
                for (WorldPoint origin : fairyRings) {
                    for (int i = 0; i < fairyRings.size(); i++) {
                        WorldPoint destination = fairyRings.get(i);
                        String questName = fairyRingsQuestNames.get(i);
                        if (origin.equals(destination)) {
                            continue;
                        }
                        Transport transport = new Transport(origin, destination);
                        transport.isFairyRing = true;
                        transport.wait = 5;
                        transport.displayInfo = fairyRingCodes.get(i);
                        transport.action = "Configure";
                        transport.interactableName = "Fairy ring";
                        transport.transportType = TransportType.FAIRY_RING;
                        transports.computeIfAbsent(origin, k -> new ArrayList<>()).add(transport);
                        if (!Strings.isNullOrEmpty(questName)) {
                            transport.quests = findQuests(questName);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<WorldPoint, List<Transport>> loadAllFromResources() {
        HashMap<WorldPoint, List<Transport>> transports = new HashMap<>();
        addTransports(transports, "transports.tsv", TransportType.TRANSPORT);
        addTransports(transports, "agility_shortcuts.tsv", TransportType.AGILITY_SHORTCUT);
        addTransports(transports, "boats.tsv", TransportType.BOAT);
        addTransports(transports, "canoes.tsv", TransportType.CANOE);
        addTransports(transports, "charter_ships.tsv", TransportType.CHARTER_SHIP);
        addTransports(transports, "ships.tsv", TransportType.SHIP);
        addTransports(transports, "fairy_rings.tsv", TransportType.FAIRY_RING);
        addTransports(transports, "gnome_gliders.tsv", TransportType.GNOME_GLIDER);
        addTransports(transports, "spirit_trees.tsv", TransportType.SPIRIT_TREE);
        addTransports(transports, "levers.tsv", TransportType.TELEPORTATION_LEVER);
        addTransports(transports, "portals.tsv", TransportType.TELEPORTATION_PORTAL);

        addItemTransports(transports);
        //addSpellTransports(transports);
        return transports;
    }

    public enum TransportType {
        TRANSPORT,
        AGILITY_SHORTCUT,
        BOAT,
        CANOE,
        CHARTER_SHIP,
        SHIP,
        FAIRY_RING,
        GNOME_GLIDER,
        SPIRIT_TREE,
        TELEPORTATION_LEVER,
        TELEPORTATION_PORTAL,
        TELEPORTATION_SPELL,
        TELEPORTATION_ITEM
    }
}
