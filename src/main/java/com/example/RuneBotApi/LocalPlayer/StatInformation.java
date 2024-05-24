package com.example.RuneBotApi.LocalPlayer;

import com.example.EthanApiPlugin.EthanApiPlugin;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.RuneLite;
import net.runelite.client.plugins.Plugin;


public final class StatInformation {

    static Client client = EthanApiPlugin.getClient();

    public static int getLevel(Skill skillName, StatType type)
    {
        return type == StatType.BASE ?
                client.getRealSkillLevel(skillName) : client.getBoostedSkillLevel(skillName);

    }

    public static int getHpPercentage()
    {
        return (int)((double)getLevel(Skill.HITPOINTS, StatType.BOOSTED) / getLevel(Skill.HITPOINTS, StatType.BASE) * 100);
    }

    public static int getHp()
    {
        return getLevel(Skill.HITPOINTS, StatType.BOOSTED);
    }
}
