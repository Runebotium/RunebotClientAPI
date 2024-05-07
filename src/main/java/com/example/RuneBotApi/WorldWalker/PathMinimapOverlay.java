package com.example.RuneBotApi.WorldWalker;

import com.google.inject.Inject;
import com.example.RuneBotApi.WorldWalker.pathfinder.Node;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;
import java.util.List;

public class PathMinimapOverlay extends Overlay {
    private static final int TILE_WIDTH = 4;
    private static final int TILE_HEIGHT = 4;

    private final Client client;
    private final RuneBotWalkerPlugin plugin;
    private final RuneBotWalkerConfig config;

    @Inject
    private PathMinimapOverlay(Client client, RuneBotWalkerPlugin plugin, RuneBotWalkerConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.drawMinimap() || plugin.getPathfinder() == null) {
            return null;
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setClip(plugin.getMinimapClipArea());

        List<Node> pathPoints = plugin.getPathfinder().getPath();
        Color pathColor = plugin.getPathfinder().isDone() ? config.colourPath() : config.colourPathCalculating();
        for (Node pathPoint : pathPoints) {
            WorldPoint point = WorldPointUtil.unpackWorldPoint(pathPoint.packedPosition);
            if (point.getPlane() != client.getPlane()) {
                continue;
            }

            drawOnMinimap(graphics, point, pathColor);
        }

        return null;
    }

    private void drawOnMinimap(Graphics2D graphics, WorldPoint location, Color color) {
        for (WorldPoint point : WorldPoint.toLocalInstance(client, location)) {
            LocalPoint lp = LocalPoint.fromWorld(client, point);

            if (lp == null) {
                continue;
            }

            Point posOnMinimap = Perspective.localToMinimap(client, lp);

            if (posOnMinimap == null) {
                continue;
            }

            renderMinimapRect(client, graphics, posOnMinimap, TILE_WIDTH, TILE_HEIGHT, color);
        }
    }

    public static void renderMinimapRect(Client client, Graphics2D graphics, Point center, int width, int height, Color color) {
        double angle = client.getCameraYawTarget() * Math.PI / 1024.0d;

        graphics.setColor(color);
        graphics.rotate(angle, center.getX(), center.getY());
        graphics.fillRect(center.getX() - width / 2, center.getY() - height / 2, width, height);
        graphics.rotate(-angle, center.getX(), center.getY());
    }
}
