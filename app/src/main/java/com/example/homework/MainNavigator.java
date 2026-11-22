package com.example.homework;

public interface MainNavigator {
    void openItemDetail(long itemId);

    void openPostEditor(long itemId);

    void openProfile();

    void openDiscovery();

    void refreshNavigationIndicators();
}
