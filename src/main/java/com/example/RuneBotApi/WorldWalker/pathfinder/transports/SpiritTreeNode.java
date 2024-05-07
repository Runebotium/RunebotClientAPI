package com.example.RuneBotApi.WorldWalker.pathfinder.transports;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.WorldWalker.Transport;
import com.example.RuneBotApi.WorldWalker.pathfinder.Node;
import com.example.RuneBotApi.WorldWalker.pathfinder.TransportNode;
import net.runelite.api.widgets.Widget;

public class SpiritTreeNode extends TransportNode {

    private final int index;

    public SpiritTreeNode(Transport transport, Node previous) {
        super(transport, previous);
        this.index = getIndexFromText(transport.getDisplayInfo());
    }

    public void teleport() {
        MousePackets.queueClickPacket();
        WidgetPackets.queueResumePause(12255235, index);
    }


    public boolean isWidgetOpen() {
        Widget widget = EthanApiPlugin.getClient().getWidget(12255235);
        return widget != null && !widget.isHidden();
    }

    public int getIndexFromText(String text) {
        String firstLetter = text.substring(0, 1);

        switch (firstLetter) {
            case "1":
                return 0;
            case "2":
                return 1;
            case "3":
                return 2;
            case "4":
                return 3;
            case "5":
                return 4;
            case "6":
                return 5;
            case "A":
                return 6;
            case "B":
                return 7;
            case "C":
                return 8;
            case "D":
                return 9;
        }

        return -1;
    }
}
