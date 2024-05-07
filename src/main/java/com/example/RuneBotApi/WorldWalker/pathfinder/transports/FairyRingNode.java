package com.example.RuneBotApi.WorldWalker.pathfinder.transports;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.WorldWalker.Transport;
import com.example.RuneBotApi.WorldWalker.pathfinder.Node;
import com.example.RuneBotApi.WorldWalker.pathfinder.TransportNode;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

public class FairyRingNode extends TransportNode {

    public static final int TIMEOUT = 6;

    private final String code;

    public FairyRingNode(Transport transport, Node previous) {
        super(transport, previous);
        code = transport.getDisplayInfo();
    }

    public boolean isWidgetOpen() {
        Widget fairyRingWidget = EthanApiPlugin.getClient().getWidget(WidgetInfo.FAIRY_RING);
        return fairyRingWidget != null && !fairyRingWidget.isHidden();
    }

    public void interact() {
        if (EthanApiPlugin.getClient().getVarbitValue(Varbits.DIARY_LUMBRIDGE_ELITE) == 0) {
            Inventory.search().filter(item ->
                    WildcardMatcher.matches("dramen staff*", Text.removeTags(item.getName()).toLowerCase())
                            || WildcardMatcher.matches("lunar staff*", Text.removeTags(item.getName()).toLowerCase())
            ).first().ifPresent(item -> {
                InventoryInteraction.useItem(item, "Equip", "Wear", "Wield");
            });
        }

        TileObjects.search().withName("Fairy ring").withAction("Configure").nearestToPlayer().ifPresent(tileObject -> {
            TileObjectInteraction.interact(tileObject, "Configure");
        });
    }

    public boolean teleport() {
        Widget widget = Widgets.search().withId(WidgetInfoExtended.FAIRY_RING_TELEPORT_BUTTON.getPackedId()).first().orElse(null);

        if (widget == null) {
            return false;
        }

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(widget, "Confirm");
        return true;
    }

    public boolean setDials() {
        Client client = EthanApiPlugin.getClient();

        for (Code c : Code.values()) {
            if (c.name().equals(code) && c != Code.INVALID && c != Code.AIR_DLR_DJQ_AJS) {
                int adcb = DialAdcb.valueOf(String.valueOf(code.charAt(0))).position;
                int iljk = DialIljk.valueOf(String.valueOf(code.charAt(1))).position;
                int psrq = DialPsrq.valueOf(String.valueOf(code.charAt(2))).position;
                int currentAdcb = client.getVarbitValue(Varbits.FAIRY_RING_DIAL_ADCB);
                int currentIljk = client.getVarbitValue(Varbits.FAIRY_RIGH_DIAL_ILJK);
                int currentPsrq = client.getVarbitValue(Varbits.FAIRY_RING_DIAL_PSRQ);

                if (adcb != currentAdcb) {
                    Widget rotate = client.getWidget(WidgetInfo.FAIRY_RING.getGroupId(), 19);

                    if (rotate != null && !rotate.isHidden()) {
                        MousePackets.queueClickPacket();
                        WidgetPackets.queueWidgetAction(rotate, "Rotate clockwise");
                    }
                    return true;
                }

                if (iljk != currentIljk) {
                    Widget rotate = client.getWidget(WidgetInfo.FAIRY_RING.getGroupId(), 21);

                    if (rotate != null && !rotate.isHidden()) {
                        MousePackets.queueClickPacket();
                        WidgetPackets.queueWidgetAction(rotate, "Rotate clockwise");
                    }
                    return true;
                }

                if (psrq != currentPsrq) {
                    Widget rotate = client.getWidget(WidgetInfo.FAIRY_RING.getGroupId(), 23);

                    if (rotate != null && !rotate.isHidden()) {
                        MousePackets.queueClickPacket();
                        WidgetPackets.queueWidgetAction(rotate, "Rotate clockwise");
                    }
                    return true;
                }
            }
        }

        return false;
    }

    enum DialAdcb {
        A (0),
        D (1),
        C (2),
        B (3);

        final int position;

        DialAdcb(int position) {
            this.position = position;
        }
    }

    enum DialIljk {
        I (0),
        L (1),
        J (3),
        K (2);

        final int position;

        DialIljk(int position) {
            this.position = position;
        }
    }

    enum DialPsrq {
        P (0),
        S (1),
        R (2),
        Q (3);

        final int position;

        DialPsrq(int position) {
            this.position = position;
        }
    }

    enum Code {
        INVALID (-1),
        AIQ (3),
        AIR (2),
        AIR_DLR_DJQ_AJS (-1),
        AJQ (15),
        AJR (14),
        AJS (13),
        AKP (8),
        AKQ (11),
        AKS (9),
        ALP (4),
        ALQ (7),
        ALR (6),
        ALS (5),
        BIP (48),
        BIQ (51),
        BIS (49),
        BJP (60),
        BJR (62),
        BJS (61),
        BKP (56),
        BKQ (59),
        BLP (52),
        BLR (54),
        BLS (53),
        CIP (32),
        CIR (34),
        CIS (33),
        CJR (46),
        CKP (40),
        CKS (41),
        CLP (36),
        CLR (38),
        CLS (37),
        DIP (16),
        DIQ (19),
        DIS (17),
        DJP (28),
        DJR (30),
        DKP (24),
        DKR (26),
        DKS (25),
        DLQ (23),
        DLR (22),
        DLS (21);

        final int varbValue;

        Code(int varbValue) {
            this.varbValue = varbValue;
        }
    }
}
