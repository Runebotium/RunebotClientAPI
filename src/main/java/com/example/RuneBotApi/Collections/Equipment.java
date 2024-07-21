package com.example.RuneBotApi.Collections;

import com.example.EthanApiPlugin.Collections.query.EquipmentItemQuery;

/*
    * This class is a copy of the Equipment class from EthanApiPlugin, but with the search method returning a EquipmentItemQuery
*/
public class Equipment extends com.example.EthanApiPlugin.Collections.Equipment {
    public static EquipmentItemQuery RetryCollection() {
        return search();
    }
}
