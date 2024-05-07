package com.example.RuneBotApi.WorldWalker.pathfinder.transports;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.WorldWalker.Transport;
import com.example.RuneBotApi.WorldWalker.pathfinder.Node;
import com.example.RuneBotApi.WorldWalker.pathfinder.TransportNode;
import net.runelite.api.widgets.Widget;

public class GliderTransportNode extends TransportNode {

    private static final int WIDGET_ID = 9043968;

    public GliderTransportNode(Transport transport, Node previous) {
        super(transport, previous);
    }

    public boolean isWidgetOpen() {
        Widget widget = EthanApiPlugin.getClient().getWidget(WIDGET_ID);
        return widget != null && !widget.isHidden();
    }

    public void teleport() {
        for (Location loc : Location.values()) {
            if (loc.locationName.equalsIgnoreCase(getTransport().getDisplayInfo())) {
                Widget widget = EthanApiPlugin.getClient().getWidget(loc.widgetId);
                if (widget != null && !widget.isHidden()) {
                    MousePackets.queueClickPacket();
                    WidgetPackets.queueWidgetAction(widget, widget.getActions()[0]);
                    break;
                }
            }
        }
    }

    enum Location {
        TA_QUIR_PRIW ("Ta Quir Priq", 9043972, 4),
        SINDARPOS ("Sindarpos" , 9043975, 7),
        LEMANTO_ANDRA ("Lemnato Andra" , 9043978, 10),
        KAR_KEWO ("Kar-Hewo", 9043981, 13),
        GANDIUS ("Gandius", 9043984, 15),
        LEMANTOLLY_UNDRI ("Lemantolly Undri", 9043989, 21),
        OOKOOKOLLY_UNDRI ("Ookookolly Undri", 9043993, 25);

        final String locationName;
        final int widgetId;
        final int childId;

        Location(String locationName, int widgetId, int childId) {
            this.locationName = locationName;
            this.widgetId = widgetId;
            this.childId = childId;
        }
    }
}
