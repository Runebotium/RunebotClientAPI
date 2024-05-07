package com.example.RuneBotApi.WorldWalker.pathfinder.transports;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.WorldWalker.Transport;
import com.example.RuneBotApi.WorldWalker.pathfinder.Node;
import com.example.RuneBotApi.WorldWalker.pathfinder.TransportNode;
import net.runelite.api.widgets.Widget;

public class CharterShipNode extends TransportNode {

    private static final int WIDGET_ID = 4718592;

    public CharterShipNode(Transport transport, Node previous) {
        super(transport, previous);
    }

    public static boolean isWidgetOpen() {
        Widget widget = EthanApiPlugin.getClient().getWidget(WIDGET_ID);
        return widget != null && !widget.isHidden();
    }

    public void teleport() {
        Location location = null;

        for (Location loc : Location.values()) {
            String name = loc.name().replace("_", " ");
            String displayInfo = getTransport().getDisplayInfo();
            if (name.equalsIgnoreCase(displayInfo)) {
                location = loc;
            }
        }

        if (location == null) {
            return;
        }

        Widget widget = EthanApiPlugin.getClient().getWidget(72, location.childId);

        if (widget != null && !widget.isHidden()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(widget, widget.getActions()[0]);
        }
    }

    enum Location {
        PORT_TYRAS (2),
        PORT_PHASMATYS (5),
        CATHERBY (8),
        SHIPYARD (11),
        MUSA_POINT ( 14),
        BRIMHAVEN (20),
        PORT_KHAZARD (23),
        PORT_SARIM (26),
        MOS_LE_HARMLESS (29),
        CORSAIR_COVE (32),
        PRIFDDINAS (35),
        PORT_PISCARILIUS (38),
        LANDS_END (41);

        final int childId;

        Location(int childId) {
            this.childId = childId;
        }
    }
}
