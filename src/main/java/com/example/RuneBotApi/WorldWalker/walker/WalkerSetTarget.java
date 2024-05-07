package com.example.RuneBotApi.WorldWalker.walker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
@Getter
public class WalkerSetTarget {
    private WorldPoint destination;
}
