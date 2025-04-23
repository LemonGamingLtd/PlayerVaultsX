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

import com.drtshock.playervaults.PlayerVaults;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A class that stores information about a vault viewing including the holder of the vault, and the vault number.
 */
public class VaultViewInfo {

    public static final ItemStack FILLER_ICON = new ItemStack(Material.GRAY_DYE);
    public static final ItemStack NEXT_PAGE_ICON = new ItemStack(Material.GRAY_DYE);
    public static final ItemStack PREVIOUS_PAGE_ICON = new ItemStack(Material.GRAY_DYE);
    public static final ItemStack DISABLE_BAR_ICON = new ItemStack(Material.GRAY_DYE);

    static {
        // TODO: Pull dynamically from packgen
        final ItemMeta fillerIconItemMeta = FILLER_ICON.getItemMeta();
        fillerIconItemMeta.setCustomModelData(27);
        fillerIconItemMeta.setDisplayName(ChatColor.RESET + "");
        FILLER_ICON.setItemMeta(fillerIconItemMeta);

        final ItemMeta previousPageIconItemMeta = NEXT_PAGE_ICON.getItemMeta();
        previousPageIconItemMeta.setCustomModelData(5);
        previousPageIconItemMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
        PREVIOUS_PAGE_ICON.setItemMeta(previousPageIconItemMeta);

        final ItemMeta nextPageIconItemMeta = NEXT_PAGE_ICON.getItemMeta();
        nextPageIconItemMeta.setCustomModelData(7);
        nextPageIconItemMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
        NEXT_PAGE_ICON.setItemMeta(nextPageIconItemMeta);

        final ItemMeta disableBarIconItemMeta = DISABLE_BAR_ICON.getItemMeta();
        disableBarIconItemMeta.setCustomModelData(20);
        disableBarIconItemMeta.setDisplayName(ChatColor.RED + ChatColor.BOLD.toString() + "Disable Navigation Bar!");
        disableBarIconItemMeta.setLore(List.of(ChatColor.RED + "You may reenable it with /pv togglebar!"));
        DISABLE_BAR_ICON.setItemMeta(disableBarIconItemMeta);
    }

    private static final int[] RESERVED_SLOTS = IntStream.rangeClosed(9, 17).toArray();
    private static final String SLOT_SETUP = "<###x###>";

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
        if (PlayerVaults.getInstance().getSettings().toggledNavigationBar(player.getUniqueId())) {
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        for (final int reservedSlot : RESERVED_SLOTS) {
            final ItemStack item = inventory.getItem(reservedSlot);
            this.holder.put(reservedSlot, item);

            final int slot = reservedSlot - 9;
            final char icon = SLOT_SETUP.charAt(slot);

            final ItemStack nextPageIcon = NEXT_PAGE_ICON.clone();
            nextPageIcon.setAmount(number + 1);

            final ItemStack previousPageIcon = PREVIOUS_PAGE_ICON.clone();
            previousPageIcon.setAmount(number);

            switch (icon) {
                case '#' -> inventory.setItem(reservedSlot, FILLER_ICON.clone());
                case '>' -> inventory.setItem(reservedSlot, nextPageIcon);
                case '<' -> inventory.setItem(reservedSlot, previousPageIcon);
                case 'x' -> inventory.setItem(reservedSlot, DISABLE_BAR_ICON.clone());
            }
        }
    }

    /**
     * Scrap and handle restoring items for the given player.
     *
     * @param player {@link Player} to handle.
     */
    public void restore(@NotNull Player player) {
        if (this.holder.isEmpty()) {
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        for (final int reservedSlot : RESERVED_SLOTS) {
            final ItemStack item = this.holder.get(reservedSlot);
            inventory.setItem(reservedSlot, item);
        }
    }

    /**
     * Check if the view is of a foreign ("other") player.
     *
     * @param player Player to check.
     * @return {@code true} if it is foreign, {@code false} otherwise.
     */
    public boolean isForeign(@NotNull Player player) {
        return !getVaultName().equals(player.getUniqueId().toString());
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