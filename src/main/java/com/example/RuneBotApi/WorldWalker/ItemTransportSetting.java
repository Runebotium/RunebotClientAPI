package com.example.RuneBotApi.WorldWalker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemTransportSetting {
    None("None"),
    Inventory("Inventory"),
    Bank("Bank"),
    All("All");

    private final String type;

    @Override
    public String toString() {return type;}
}