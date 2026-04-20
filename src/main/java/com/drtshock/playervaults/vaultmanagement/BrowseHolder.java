/*
 * PlayerVaultsX
 * Copyright (C) 2013 Trent Hensler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.drtshock.playervaults.vaultmanagement;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Marks the browse-vaults and search-vaults GUIs so listeners can identify them
 * without confusing them with real {@link VaultHolder} vault inventories.
 */
public class BrowseHolder implements InventoryHolder {

    public enum Type {
        BROWSE,
        SEARCH
    }

    /**
     * Represents a single clickable entry in a browse or search GUI.
     */
    public record Entry(int vaultNumber, int sourceSlot) {}

    private final Type type;
    private final String targetName;
    private final String targetUuid;
    private final String query;
    private final List<Entry> entries;
    private final int page;
    private final int totalPages;
    private Inventory inventory;

    public BrowseHolder(@NotNull Type type, @NotNull String targetUuid, @NotNull String targetName, @NotNull List<Entry> entries, int page, int totalPages, String query) {
        this.type = type;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.page = page;
        this.totalPages = totalPages;
        this.query = query;
    }

    public @NotNull Type getType() {
        return this.type;
    }

    public @NotNull String getTargetUuid() {
        return this.targetUuid;
    }

    public @NotNull String getTargetName() {
        return this.targetName;
    }

    public String getQuery() {
        return this.query;
    }

    public @NotNull List<Entry> getEntries() {
        return this.entries;
    }

    public int getPage() {
        return this.page;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}