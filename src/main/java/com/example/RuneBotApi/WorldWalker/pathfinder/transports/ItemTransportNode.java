package com.example.RuneBotApi.WorldWalker.pathfinder.transports;

import com.example.EthanApiPlugin.Collections.Equipment;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.WorldWalker.Transport;
import com.example.RuneBotApi.WorldWalker.pathfinder.Node;
import com.example.RuneBotApi.WorldWalker.pathfinder.TransportNode;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class ItemTransportNode extends TransportNode {

    // TODO
    //  Implement Bank Checks & reroute path to bank if it is a shorter path
    //  Entirely optional, but would be nice.
    //  Could also implement this bank check only if you are near a bank,
    //  i.e the Plugin using this would have the bank open when pathing is started
    //  thus creating an ItemTransportNode that would need to bank.
    //  Possibly add this information to the Transport itself, and offer quick Getters
    //  in this class.  Sky's the limit.

    private final boolean shouldBank;

    public ItemTransportNode(Transport transport, Node previous) {
        super(transport, previous);
        this.shouldBank = false;
    }

    public ItemTransportNode(Transport transport, Node previous, boolean shouldBank) {
        super(transport, previous);
        this.shouldBank = shouldBank;
    }

    public boolean teleport() {
        List<Integer> itemsRequired = getTransport().getItemRequirements();
        String displayInfo = getTransport().getDisplayInfo();
        String[] parts = displayInfo.split(";");
        String name = parts[0];
        String action = parts[1];
        boolean shouldEquip = getTransport().isEquippable();

        EthanApiPlugin.sendClientMessage("Teleporting with item: " + name + ", " + action);

        if (parts.length > 2) {
            String extra = parts[2];
            EthanApiPlugin.sendClientMessage("Should handle additional dialog for: " + extra);
        }

        AtomicBoolean equippedItem = new AtomicBoolean(false);

        Equipment.search().filter(item -> itemsRequired.contains(item.getItemId())).first().ifPresentOrElse(item -> {
            if (shouldEquip) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(item, action);
            }
        }, () -> {
            Inventory.search().filter(item -> itemsRequired.contains(item.getItemId())).first().ifPresent(item -> {
                if (shouldEquip) {
                    InventoryInteraction.useItem(item, "Equip", "Wear", "Wield");
                    equippedItem.set(true);
                } else {
                    InventoryInteraction.useItem(item, action);
                }
            });
        });

        return equippedItem.get();
    }
}
