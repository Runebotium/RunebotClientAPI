package com.example.RuneBotApi.Collections;

import com.example.EthanApiPlugin.Collections.query.EquipmentItemQuery;

import java.util.Arrays;

/*
 * This class is a copy of the Equipment class from EthanApiPlugin, but with the search method returning a EquipmentItemQuery
 */
public class Equipment extends com.example.EthanApiPlugin.Collections.Equipment {

    // Method to retry collection with EquipmentItemQuery return type
    public static EquipmentItemQuery RetryCollection() {
        return search(); // Calls inherited 'search' method and returns EquipmentItemQuery
    }

    // Method to check if all given names exist in items
    public static boolean hasItems(String ...names) {
        return Arrays.stream(names).filter(name -> !hasItem(name)).count() == 0;
    }

    // Method to check if all given IDs exist in items
    public static boolean hasItems(int ...ids) {
        return Arrays.stream(ids).filter(id -> !hasItem(id)).count() == 0;
    }

    // Method to check if an item with the given ID exists
    public static boolean hasItem(int id) {
        return Equipment.search().withId(id).first().isPresent();
    }

    // Method to check if an item with the given name exists (case insensitive)
    public static boolean hasItem(String name) {
        return Equipment.search().nameContainsNoCase(name).first().isPresent();
    }
}
