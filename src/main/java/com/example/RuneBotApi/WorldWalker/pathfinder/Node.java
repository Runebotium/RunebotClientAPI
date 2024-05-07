package com.example.RuneBotApi.WorldWalker.pathfinder;

import com.example.RuneBotApi.WorldWalker.WorldPointUtil;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Node {
    public final int packedPosition;
    public final Node previous;
    public final int cost;

    public Node(WorldPoint position, Node previous, int wait) {
        this.packedPosition = WorldPointUtil.packWorldPoint(position);
        this.previous = previous;
        this.cost = cost(previous, wait);
    }

    public Node(WorldPoint position, Node previous) {
        this(position, previous, 0);
    }

    public Node(int packedPosition, Node previous, int wait) {
        this.packedPosition = packedPosition;
        this.previous = previous;
        this.cost = cost(previous, wait);
    }

    public Node(int packedPosition, Node previous) {
        this(packedPosition, previous, 0);
    }

    public List<Node> getPath() {
        List<Node> path = new LinkedList<>();
        Node node = this;

        while (node != null) {
            path.add(0, node);
            node = node.previous;
        }

        return new ArrayList<>(path);
    }

    public List<Integer> getPathPacked() {
        List<Integer> path = new LinkedList<>();
        Node node = this;

        while (node != null) {
            path.add(0, node.packedPosition);
            node = node.previous;
        }

        return new ArrayList<>(path);
    }

    private int cost(Node previous, int wait) {
        int previousCost = 0;
        int distance = 0;

        if (previous != null) {
            previousCost = previous.cost;
            // Travel wait time is converted to distance as if the player is walking 1 tile/tick.
            // TODO: reduce the distance if the player is currently running and has enough run energy for the distance?
            distance = wait > 0 ? wait : WorldPointUtil.distanceBetween(previous.packedPosition, packedPosition);
        }

        return previousCost + distance;
    }
}
