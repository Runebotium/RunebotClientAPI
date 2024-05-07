package com.example.RuneBotApi.WorldWalker.pathfinder;

import com.example.RuneBotApi.WorldWalker.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

public class Pathfinder implements Runnable {
    private final PathfinderStats stats;
    @Getter
    private volatile boolean done = false;
    private volatile boolean cancelled = false;

    @Getter
    private final WorldPoint start;
    @Getter
    private final WorldPoint target;

    private final int targetPacked;

    private final PathfinderConfig config;
    private final CollisionMap map;
    private final boolean targetInWilderness;

    // Capacities should be enough to store all nodes without requiring the queue to grow
    // They were found by checking the max queue size
    private final Deque<Node> boundary = new ArrayDeque<>(4096);
    private final Queue<Node> pending = new PriorityQueue<>(256);
    private final VisitedTiles visited;

    @Getter
    private List<Node> path = new ArrayList<>();

    /** Item transports for player-held are not added until the first valid wilderness tile is encountered, 30 for some items, 20 for most */
    private int maxWildernessLevelItemsAdded;

    public Pathfinder(PathfinderConfig config, WorldPoint start, WorldPoint target) {
        stats = new PathfinderStats();
        this.config = config;
        this.map = config.getMap();
        this.start = start;
        this.target = target;
        visited = new VisitedTiles(map);
        targetPacked = WorldPointUtil.packWorldPoint(target);
        targetInWilderness = PathfinderConfig.isInWilderness(target);
        maxWildernessLevelItemsAdded = 31;
        new Thread(this).start();
    }

    public void cancel() {
        cancelled = true;
    }

    public PathfinderStats getStats() {
        if (stats.started && stats.ended) {
            return stats;
        }

        // Don't give incomplete results
        return null;
    }

    private Node addNeighbors(Node node) {
        List<Node> nodes = map.getNeighbors(node, visited, config);
        for (Node neighbor : nodes) {
            if (neighbor.packedPosition == targetPacked) {
                return neighbor;
            }

            if (config.isAvoidWilderness() && config.avoidWilderness(node.packedPosition, neighbor.packedPosition, targetInWilderness)) {
                continue;
            }

            visited.set(neighbor.packedPosition);
            if (neighbor instanceof TransportNode) {
                pending.add(neighbor);
                ++stats.transportsChecked;
            } else {
                boundary.addLast(neighbor);
                ++stats.nodesChecked;
            }
        }

        return null;
    }

    @Override
    public void run() {
        stats.start();
        boundary.addFirst(new Node(start, null));

        int bestDistance = Integer.MAX_VALUE;
        long bestHeuristic = Integer.MAX_VALUE;
        long cutoffDurationMillis = config.getCalculationCutoffMillis();
        long cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;

        while (!cancelled && (!boundary.isEmpty() || !pending.isEmpty())) {
            Node node = boundary.peekFirst();
            Node p = pending.peek();

            if (p != null && (node == null || p.cost < node.cost)) {
                boundary.addFirst(p);
                pending.poll();
            }

            node = boundary.removeFirst();

            if (this.maxWildernessLevelItemsAdded > 20) {
                //make sure item transports aren't added twice
                boolean shouldAddItems = false;
                boolean shouldAddSpell = false;
                //these are overlapping boundaries, so if the node isn't in level 30, it's in 0-29
                // likewise, if the node isn't in level 20, it's in 0-19
                if (this.maxWildernessLevelItemsAdded > 30 && !config.isInLevel30Wilderness(node.packedPosition)) {
                    this.maxWildernessLevelItemsAdded = 30;
                    shouldAddItems = true;
                }
                if (this.maxWildernessLevelItemsAdded > 20 && !config.isInLevel20Wilderness(node.packedPosition)) {
                    this.maxWildernessLevelItemsAdded = 20;
                    shouldAddItems = true;
                    shouldAddSpell = true;
                }
                if (shouldAddItems) {
                    config.refreshPlayerTransportData(WorldPointUtil.unpackWorldPoint(node.packedPosition), this.maxWildernessLevelItemsAdded);
                }
                if (shouldAddSpell) {
                    config.refreshSpellTransportData(WorldPointUtil.unpackWorldPoint(node.packedPosition), this.maxWildernessLevelItemsAdded);
                }
            }

            if (node.packedPosition == targetPacked || !config.isNear(start)) {
                path = node.getPath();
                break;
            }

            int distance = WorldPointUtil.distanceBetween(node.packedPosition, targetPacked);
            long heuristic = distance + WorldPointUtil.distanceBetween(node.packedPosition, targetPacked, 2);
            if (heuristic < bestHeuristic || (heuristic <= bestHeuristic && distance < bestDistance)) {
                path = node.getPath();
                bestDistance = distance;
                bestHeuristic = heuristic;
                cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;
            }

            if (System.currentTimeMillis() > cutoffTimeMillis) {
                break;
            }

            // Check if target was found without processing the queue to find it
            if ((p = addNeighbors(node)) != null) {
                path = node.getPath();
                break;
            }
        }

        done = !cancelled;

        boundary.clear();
        visited.clear();
        pending.clear();

        stats.end(); // Include cleanup in stats to get the total cost of pathfinding
    }

    public static class PathfinderStats {
        @Getter
        private int nodesChecked = 0, transportsChecked = 0;
        private long startNanos, endNanos;
        private volatile boolean started = false, ended = false;

        public int getTotalNodesChecked() {
            return nodesChecked + transportsChecked;
        }

        public long getElapsedTimeNanos() {
            return endNanos - startNanos;
        }

        private void start() {
            started = true;
            nodesChecked = 0;
            transportsChecked = 0;
            startNanos = System.nanoTime();
        }

        private void end() {
            endNanos = System.nanoTime();
            ended = true;
        }
    }
}
