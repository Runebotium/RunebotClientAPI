package com.example.RuneBotApi.WorldWalker.walker;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.ObjectPackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.WorldWalker.*;
import com.example.RuneBotApi.WorldWalker.pathfinder.Node;
import com.example.RuneBotApi.WorldWalker.pathfinder.Pathfinder;
import com.example.RuneBotApi.WorldWalker.pathfinder.TransportNode;
import com.example.RuneBotApi.WorldWalker.pathfinder.transports.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.EventBus;

import java.util.List;

@Singleton
public class WorldWalker {

    /**
     *  To make use of WorldWalker:
     *  utilize dependency injection, similar to ChinBreakHandler
     *  @Inject
     *  private WorldWalker walker
     *
     *  Fairly simple, `isWalking()` and `setTarget(WorldPoint target)` are the method you want
     */

    private static final String TAG = "[RBWalker]";
    private static final EventBus eventBus = RuneLite.getInjector().getInstance(EventBus.class);

    @Getter
    @Setter
    private static boolean isWalking;

    private final Client client;
    private final RuneBotWalkerPlugin plugin;
    private final RuneBotWalkerConfig config;

    @Getter
    private int failedAttempts;
    private Node nodeToRemove;
    private boolean recalculatePath;

    /**
     * Use walker.isWalking() to check if the walker is being used.
     * this will be the responsibility of the user opposed to this taking control
     * of the plugin.
     */

    private Node previousNode;
    private int timeout;

    @Inject
    private WorldWalker(Client client, RuneBotWalkerPlugin plugin, RuneBotWalkerConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.recalculatePath = false;
        this.timeout = 0;
        this.failedAttempts = 0;
    }

    /** The GameTick update call
     *   This is not intended to be called outside of the main Plugin class
     *   will likely be moved at some point
     * */

    public void update(Pathfinder pathfinder) {
        if (!isWalking) {
            return;
        }

        if (timeout > 0) {
            sendDebugChatMessage("Timeout");
            timeout--;
            return;
        }

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        if (recalculatePath) {
            sendDebugChatMessage("Recalculating path after taking transport node");
            plugin.restartPathfinding(playerLocation, pathfinder.getTarget());
            recalculatePath = false;
            return;
        }

        List<Node> path = pathfinder.getPath();

        if (path == null || path.isEmpty()) {
            isWalking = false;
            sendDebugChatMessage("No longer pathing");
            return;
        }

        if (nodeToRemove != null) {
            path.remove(nodeToRemove);
            nodeToRemove = null;
        }

        if (previousNode != null && previousNode instanceof TransportNode) {
            TransportNode node = (TransportNode) previousNode;

            if (node instanceof SpellTransportNode) {
                WorldArea area = node.getTransport().getDestinationArea();
                if (area.contains(playerLocation)) {
                    path.remove(previousNode);
                    previousNode = null;
                }
            } else {
                WorldPoint previousNodePoint = WorldPointUtil.unpackWorldPoint(previousNode.packedPosition);
                if (playerLocation.equals(previousNodePoint)) {
                    path.remove(previousNode);
                    previousNode = null;
                }
            }
        }

        int indexesAhead = 0;

        for (Node node : path) {
            if (shouldSkipRemove(node, indexesAhead)) {
                break;
            }
            WorldPoint current = WorldPointUtil.unpackWorldPoint(node.packedPosition);
            if (playerLocation.equals(current)) {
                sendDebugChatMessage("You are " + indexesAhead + " of the current node");
                break;
            }
            indexesAhead++;
        }

        if (indexesAhead > 0 && indexesAhead != path.size()) {
            sendDebugChatMessage("Clearing previous nodes of size: " + indexesAhead);
            path.subList(0, indexesAhead).clear();
        }

        if (path.isEmpty()) {
            isWalking = false;
            return;
        }

        if (path.size() == 1) {
            Node current = path.get(0);

            if (current instanceof TransportNode) {
                handleTransportNode(current);
            } else {
                handleSingleNode(current);
            }

            return;
        }

        if (path.size() == 2) {
            handleLastTwoNodes(path);
            return;
        }

        Node current = path.get(0);
        Node next = path.get(1);
        Node following = path.get(2);

        if (handleDoor(current, next)) {
            return;
        }

        if (handleDoor(next, following)) {
            return;
        }

        if (following instanceof TransportNode) {
            handleTransportNode(following);
            sendDebugChatMessage("Handling following transport node");
            return;
        }

        if (next instanceof TransportNode) {
            handleTransportNode(next);
            sendDebugChatMessage("Handling next transport node");
            return;
        }

        if (current instanceof TransportNode) {
            handleTransportNode(current);
            sendDebugChatMessage("Handling current transport node");
            return;
        }

        handleSingleNode(following);
    }

    /** Sets the destination WorldPoint for the walker to traverse
     *   Prefer ReflectWorldWalker over this to eliminate the dependency for other Plugins.
     * */

    public static void setTarget(WorldPoint target) {
        eventBus.post(new WalkerSetTarget(target));
    }

    private boolean shouldSkipRemove(Node node, int currentIndex) {
        if (node instanceof TransportNode) {
            TransportNode transportNode = (TransportNode) node;
            return transportNode.getTransport().getTransportType() == Transport.TransportType.TELEPORTATION_ITEM
                    && currentIndex == 0;
        }

        return false;
    }

    private void handleSingleNode(Node node) {
        isWalking = true;
        WorldPoint point = WorldPointUtil.unpackWorldPoint(node.packedPosition);
        WorldPoint playerPoint = client.getLocalPlayer().getWorldLocation();

        if (point.equals(playerPoint)) {
            sendDebugChatMessage("Couldn't handle single node");
            failedAttempts++;
            return;
        }

        failedAttempts = 0;

        sendDebugChatMessage("Moving to next node");

        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(point);
    }

    private boolean handleDoor(Node current, Node next) {
        Tile currentTile = WorldWalkerUtil.getTile(WorldPointUtil.unpackWorldPoint(current.packedPosition));
        Tile nextTile = WorldWalkerUtil.getTile(WorldPointUtil.unpackWorldPoint(next.packedPosition));

        if (currentTile == null || nextTile == null) {
            return false;
        }

        if (WorldWalkerUtil.isDoored(currentTile, nextTile)) {
            WallObject doorObject = currentTile.getWallObject();

            if (doorObject == null) {
                doorObject = nextTile.getWallObject();
            }

            if (doorObject == null) {
                failedAttempts++;
                return false;
            }

            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(doorObject, false, "Open", "Close");
            sendDebugChatMessage("Handling door");
            timeout = 2;
            failedAttempts = 0;
            return true;
        }

        return false;
    }

    private void handleLastTwoNodes(List<Node> path) {
        Node current = path.get(0);
        Node next = path.get(1);

        if (handleDoor(current, next)) {
            return;
        }

        if (!isRunEnabled()) {
            if (current instanceof TransportNode) {
                handleTransportNode(current);
                return;
            }

            handleSingleNode(current);
        } else {
            if (next instanceof TransportNode) {
                handleTransportNode(next);
                return;
            }

            handleSingleNode(next);
        }
    }

    private void handleTransportNode(Node node) {
        TransportNode transportNode = (TransportNode) node;
        sendDebugChatMessage(transportNode.getTransport().getDisplayInfo());
        previousNode = node;

        switch (transportNode.getTransport().getTransportType()) {
            case TRANSPORT:
            case AGILITY_SHORTCUT:
            case TELEPORTATION_LEVER:
            case TELEPORTATION_PORTAL:
            case BOAT:
            case SHIP:
                handleSimpleTransport(transportNode);
                break;
            case CANOE:
                break;
            case CHARTER_SHIP:
                handleCharterShip((CharterShipNode) transportNode);
                break;
            case FAIRY_RING:
                handleFairyRing((FairyRingNode) transportNode);
                break;
            case GNOME_GLIDER:
                handleGnomeGlider((GliderTransportNode) transportNode);
                break;
            case SPIRIT_TREE:
                handleSpiritTree((SpiritTreeNode) transportNode);
                break;
            case TELEPORTATION_ITEM:
                handleItemTransport((ItemTransportNode) transportNode);
                break;
            case TELEPORTATION_SPELL:
                handleSpellTransport((SpellTransportNode) transportNode);
                break;
        }

        failedAttempts = 0;
    }

    private void handleSimpleTransport(TransportNode transportNode) {
        Transport transport = transportNode.getTransport();

        String action = transport.getAction();
        String interactableName = transport.getInteractableName();

        Widget continueWidget = Widgets.search().withText("Click here to continue").hiddenState(false).first().orElse(null);

        if (continueWidget != null) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(continueWidget.getId(), -1);
            timeout = 1;
            return;
        }

        sendDebugChatMessage(action + ", " + interactableName);

        // TODO :: replace with interactableName after updating transports tsvs

        TileObjects.search().withName(interactableName).withAction(action).nearestToPlayer().ifPresentOrElse(tileObject -> {
            TileObjectInteraction.interact(tileObject, action);
            sendDebugChatMessage("Handling Object transport");
            if (transport.getWait() > 0) {
                timeout = transport.getWait();
            } else {
                timeout = 3; // TODO :: Remove this in favor of using timeouts solely from Transport wait
            }
        }, () -> {
            NPCs.search().withName(interactableName).withAction(action).nearestToPlayer().ifPresent(npc -> {
                NPCInteraction.interact(npc, action);
                sendDebugChatMessage("Handling NPC transport");
                if (transport.getWait() > 0) {
                    timeout = transport.getWait();
                } else {
                    timeout = 3; // TODO :: Remove this in favor of using timeouts solely from Transport wait
                }
            });
        });
    }

    private void handleSpellTransport(SpellTransportNode spellTransportNode) {
        if (spellTransportNode.isInArea()) {
            nodeToRemove = spellTransportNode;
            return;
        }

        spellTransportNode.teleport();
        timeout = 4;
    }

    private void handleItemTransport(ItemTransportNode itemTransportNode) {
        sendDebugChatMessage("Handling item transport for: " + itemTransportNode.getTransport().getDisplayInfo());

        if (itemTransportNode.teleport()) {
            timeout = 1;
            return;
        }

        timeout = itemTransportNode.getTransport().getWait() + 2; // sometimes the wait seemed too short
        recalculatePath = true;
    }

    private void handleCharterShip(CharterShipNode charterShipNode) {
        sendDebugChatMessage("Handling charter ship node to: " + charterShipNode.getTransport().getDisplayInfo());

        Transport transport = charterShipNode.getTransport();

        if (CharterShipNode.isWidgetOpen()) {
            charterShipNode.teleport();
            timeout = transport.getWait();
            recalculatePath = true;
            return;
        }

        NPCs.search().withName(transport.getInteractableName()).withAction(transport.getAction()).nearestToPlayer().ifPresent(npc -> {
            NPCInteraction.interact(npc, transport.getAction());
            timeout = 2;
        });
    }

    private void handleGnomeGlider(GliderTransportNode gliderTransportNode) {
        sendDebugChatMessage("Handling gnome glider node to: " + gliderTransportNode.getTransport().getDisplayInfo());

        Transport transport = gliderTransportNode.getTransport();

        if (gliderTransportNode.isWidgetOpen()) {
            gliderTransportNode.teleport();
            timeout = transport.getWait();
            recalculatePath = true;
            return;
        }

        NPCs.search().withName(transport.getInteractableName()).withAction(transport.getAction()).nearestToPlayer().ifPresent(npc -> {
            NPCInteraction.interact(npc, transport.getAction());
            timeout = 2;
        });
    }

    private void handleSpiritTree(SpiritTreeNode spiritTreeNode) {
        sendDebugChatMessage("Handling spirit tree node to: " + spiritTreeNode.getTransport().getDisplayInfo());

        if (spiritTreeNode.isWidgetOpen()) {
            String info = spiritTreeNode.getTransport().getDisplayInfo();
            int index = spiritTreeNode.getIndexFromText(info);

            if (index == -1) {
                return;
            }

            spiritTreeNode.teleport();
            timeout = spiritTreeNode.getTransport().getWait();
            recalculatePath = true;
            sendDebugChatMessage("Teleporting");
            return;
        }

        TileObjects.search().withName("Spirit tree").withAction("Travel").nearestToPlayer().ifPresent(tileObject -> {
            sendDebugChatMessage("Interacting spirit tree");
            TileObjectInteraction.interact(tileObject, "Travel");
            timeout = 2;
        });
    }

    private void handleFairyRing(FairyRingNode fairyRingNode) {
        Inventory.search().matchesWildCardNoCase("*lunar staff*").first().ifPresentOrElse(item -> {
            InventoryInteraction.useItem(item, "Equip", "Wear", "Wield");
        }, () -> {
            Inventory.search().matchesWildCardNoCase("*dramen staff*").first().ifPresent(item -> {
                InventoryInteraction.useItem(item, "Equip", "Wear", "Wield");
            });
        });

        if (fairyRingNode.isWidgetOpen()) {
            if (fairyRingNode.setDials()) {
                // timeout = 1; // Possibly offer a Timeouts section in config to offer some variation?
                return;
            }

            if (!fairyRingNode.teleport()) {
                return;
            }

            recalculatePath = true;
            timeout = FairyRingNode.TIMEOUT;
            return;
        }

        fairyRingNode.interact();
        timeout = 2;
    }

    /** Sends debug chat messages.
     *   Prefer this method over `EthanApiPlugin.sendClientMessage` to avoid annoyances
     *   for the user.
     * */

    private void sendDebugChatMessage(String message) {
        if (!config.debugMode()) {
            return;
        }

        EthanApiPlugin.sendClientMessage(TAG + " " + message);
    }

    private boolean isRunEnabled() {
        return client.getVarpValue(173) == 0;
    }


}
