package com.example.RuneBotApi.WorldWalker.pathfinder;

import com.example.RuneBotApi.WorldWalker.Transport;
import lombok.Getter;

@Getter
public class TransportNode extends Node implements Comparable<TransportNode> {

    private Transport transport;

    public TransportNode(Transport transport, Node previous) {
        super(transport.getDestination(), previous, transport.getWait());
        this.transport = transport;
    }

    @Override
    public int compareTo(TransportNode other) {
        return Integer.compare(cost, other.cost);
    }
}
