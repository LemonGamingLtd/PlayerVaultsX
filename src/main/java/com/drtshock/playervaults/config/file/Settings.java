package com.drtshock.playervaults.config.file;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Settings {

    private List<String> TOGGLED_NAVIGATION_BAR = new ArrayList<>();

    public List<String> toggledNavigationBar() {
        if (TOGGLED_NAVIGATION_BAR == null) {
            TOGGLED_NAVIGATION_BAR = new ArrayList<>();
        }
        return TOGGLED_NAVIGATION_BAR;
    }

    public boolean toggledNavigationBar(@NotNull UUID playerId) {
        return toggledNavigationBar().contains(playerId.toString());
    }

    public boolean toggleNavigationBar(@NotNull UUID playerId) {
        final String playerIdRaw = playerId.toString();
        if (toggledNavigationBar().contains(playerIdRaw)) {
            toggledNavigationBar().remove(playerIdRaw);
            return true;
        } else {
            toggledNavigationBar().add(playerIdRaw);
            return false;
        }
    }
}
