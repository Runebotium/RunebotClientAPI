package com.example.EthanApiPlugin.Collections;

import com.example.EthanApiPlugin.Collections.query.NPCQuery;
import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.RuneLite;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for handling and querying NPCs in the RuneLite client.
 */

public class NPCs {
    private static final Client client = RuneLite.getInjector().getInstance(Client.class);
    private static final List<NPC> npcList = new ArrayList<>();
    private static int lastUpdate = -1;

    /**
     * Searches and retrieves an NPCQuery object containing a list of NPCs in the current game tick.
     *
     * @return an NPCQuery object with the list of NPCs
     */
    public static NPCQuery search() {
        int currentTick = client.getTickCount();
        if (lastUpdate != currentTick || npcList.isEmpty()) {
            updateNPCList();
            lastUpdate = currentTick;
        }
        return new NPCQuery(npcList);
    }

    /**
     * Updates the npcList with the current NPCs from the client.
     * This method filters out invalid NPCs and only includes those with valid IDs.
     */
    private static void updateNPCList() {
        npcList.clear();
        for (NPC npc : client.getNpcs()) {
            if (isValidNPC(npc)) {
                npcList.add(npc);
            }
        }
    }

    /**
     * Checks if an NPC is valid.
     * An NPC is considered valid if it is not null and its ID is not -1.
     *
     * @param npc the NPC to check
     * @return true if the NPC is valid, false otherwise
     */
    private static boolean isValidNPC(NPC npc) {
        return npc != null && npc.getId() != -1;
    }
}
