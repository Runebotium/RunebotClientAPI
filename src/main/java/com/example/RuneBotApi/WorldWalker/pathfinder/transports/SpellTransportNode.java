package com.example.RuneBotApi.WorldWalker.pathfinder.transports;

import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.WorldWalker.Transport;
import com.example.RuneBotApi.WorldWalker.pathfinder.Node;
import com.example.RuneBotApi.WorldWalker.pathfinder.TransportNode;
import net.runelite.api.coords.WorldArea;

public class SpellTransportNode extends TransportNode {

    private final String spellName;
    private final String actionName;
    private final WorldArea destinationArea;

    public SpellTransportNode(Transport transport, Node previous,
                              String spellName, String action,
                              WorldArea destinationArea) {
        super(transport, previous);
        this.spellName = spellName;
        this.actionName = action;
        this.destinationArea = destinationArea;
    }

    public void teleport() {
        if (isInArea()) {
            return;
        }

        Widgets.search().nameMatchesWildCardNoCase(this.spellName).first().ifPresent(spell -> {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(spell, this.actionName);
        });
    }

    public boolean isInArea() {
        return destinationArea.contains(EthanApiPlugin.getClient().getLocalPlayer().getWorldLocation());
    }
}
