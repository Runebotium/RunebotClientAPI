package com.example.EthanApiPlugin.Collections;

import com.example.EthanApiPlugin.Collections.query.ItemQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Bank {
    static Client client = RuneLite.getInjector().getInstance(Client.class);
    static List<Widget> bankItems = new ArrayList<>();
    boolean bankUpdate = true;

    public static ItemQuery search() {
        return new ItemQuery(bankItems.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public static boolean isOpen() {
        return client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER) != null && !client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER).isHidden();
    }



    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged e) {
        if (e.getContainerId() == InventoryID.BANK.getId()) {
            int i = 0;
            Bank.bankItems.clear();
            for (Item item : e.getItemContainer().getItems()) {
                try {
                    if(item==null){
                        i++;
                        continue;
                    }
                    if(EthanApiPlugin.itemDefs.get(item.getId()).getPlaceholderTemplateId()==14401){
                        i++;
                        continue;
                    }
                    Bank.bankItems.add(new BankItemWidget(EthanApiPlugin.itemDefs.get(item.getId()).getName(),item.getId(),item.getQuantity(),i));
                }catch (NullPointerException | ExecutionException ex){
                    //todo fix this
                }
                i++;
            }
        }
    }

    public static boolean hasItem(int id) {
        return hasItem(id, 1, false);
    }

    public static boolean hasItem(int id, int amount) {
        return getItemAmount(id, false) >= amount;
    }

    public static boolean hasItem(int id, int amount, boolean stacked) {
        return getItemAmount(id, stacked) >= amount;
    }

    public static int getItemAmount(int itemId, boolean stacked) {
        return stacked ?
                Bank.search().withId(itemId).first().map(Widget::getItemQuantity).orElse(0) :
                Bank.search().withId(itemId).result().size();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.HOPPING || gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.CONNECTION_LOST) {
            Bank.bankItems.clear();
        }
    }
}
