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

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A class that stores information about a vault viewing including the holder of the vault, and the vault number.
 */
public class VaultViewInfo {

    public static final ItemStack FILLER_ICON = new ItemStack(Material.GRAY_DYE);
    public static final ItemStack NEXT_PAGE_ICON = new ItemStack(Material.GRAY_DYE);
    public static final ItemStack PREVIOUS_PAGE_ICON = new ItemStack(Material.GRAY_DYE);

    static {
        // TODO: Pull dynamically from packgen
        FILLER_ICON.editMeta(itemMeta -> {
            itemMeta.setCustomModelData(27);
            itemMeta.setDisplayName(ChatColor.RESET + "");
        });
        PREVIOUS_PAGE_ICON.editMeta(itemMeta -> {
            itemMeta.setCustomModelData(5);
            itemMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
        });
        NEXT_PAGE_ICON.editMeta(itemMeta -> {
            itemMeta.setCustomModelData(7);
            itemMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
        });
    }

    private static final int[] RESERVED_SLOTS = IntStream.rangeClosed(9, 17).toArray();
    private static final String SLOT_SETUP = "<#######>";

    final String vaultName;
    final int number;

    private final Map<Integer, ItemStack> holder = new HashMap<>();

    /**
     * Makes a VaultViewInfo object. Used for opening a vault owned by the opener.
     *
     * @param i vault number.
     */
    public VaultViewInfo(String vaultName, int i) {
        this.number = i;
        this.vaultName = vaultName;
    }

    /**
     * Initialize the data holder.
     *
     * @param player {@link Player} to handle.
     */
    public void initialize(@NotNull Player player) {
        final PlayerInventory inventory = player.getInventory();
        for (final int reservedSlot : RESERVED_SLOTS) {
            final ItemStack item = inventory.getItem(reservedSlot);
            if (item != null && !item.isEmpty()) {
                this.holder.put(reservedSlot, item);
            }

            final int slot = reservedSlot - 9;
            final char icon = SLOT_SETUP.charAt(slot);
            switch (icon) {
                case '#' -> inventory.setItem(reservedSlot, FILLER_ICON);
                case '>' -> inventory.setItem(reservedSlot, NEXT_PAGE_ICON);
                case '<' -> inventory.setItem(reservedSlot, PREVIOUS_PAGE_ICON);
            }
        }
    }

    /**
     * Scrap and handle restoring items for the given player.
     *
     * @param player {@link Player} to handle.
     */
    public void restore(@NotNull Player player) {
        final PlayerInventory inventory = player.getInventory();
        for (final int reservedSlot : RESERVED_SLOTS) {
            final ItemStack item = this.holder.get(reservedSlot);
            inventory.setItem(reservedSlot, item);
        }
    }

    /**
     * Get the holder of the vault.
     *
     * @return The holder of the vault.
     */
    public String getVaultName() {
        return this.vaultName;
    }

    /**
     * Get the vault number.
     *
     * @return The vault number.
     */
    public int getNumber() {
        return this.number;
    }

    @Override
    public String toString() {
        return this.vaultName + " " + this.number;
    }
}