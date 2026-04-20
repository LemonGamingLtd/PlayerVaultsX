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
import com.drtshock.playervaults.util.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Builds and serves the browse-vaults and search-vaults GUIs. Acts as the
 * counterpart to {@link VaultManager} for the new viewer-side navigation flows.
 */
public final class VaultBrowser {

    public static final int GUI_SIZE = 6 * 9;
    public static final int RESULTS_PER_PAGE = 45;
    public static final int PREV_SLOT = 45;
    public static final int INFO_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private VaultBrowser() {}

    public static void openBrowse(@NotNull Player viewer, @NotNull String targetUuid, @NotNull String targetName, int page) {
        boolean self = targetUuid.equals(viewer.getUniqueId().toString());
        List<Integer> numbers = collectBrowseVaultNumbers(viewer, targetUuid, self);
        if (numbers.isEmpty()) {
            PlayerVaults.getInstance().getTL().browseEmpty().title().send(viewer);
            return;
        }

        List<BrowseHolder.Entry> entries = new ArrayList<>(numbers.size());
        for (int n : numbers) {
            entries.add(new BrowseHolder.Entry(n, -1));
        }

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) RESULTS_PER_PAGE));
        page = clampPage(page, totalPages);

        BrowseHolder holder = new BrowseHolder(BrowseHolder.Type.BROWSE, targetUuid, targetName,
                entries, page, totalPages, null);
        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, browseTitle(targetName, self));
        holder.setInventory(inv);

        fillContent(inv, entries, page, holder.getType(), targetUuid);
        decorateNav(inv, holder);

        viewer.openInventory(inv);
    }

    public static void openSearch(@NotNull Player viewer, @NotNull String targetUuid, @NotNull String targetName, @NotNull String query, int page) {
        boolean self = targetUuid.equals(viewer.getUniqueId().toString());
        SearchResult result = performSearch(viewer, targetUuid, self, query);
        if (result.entries.isEmpty()) {
            PlayerVaults.getInstance().getTL().searchNoResults().title().with("query", query).send(viewer);
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(result.entries.size() / (double) RESULTS_PER_PAGE));
        page = clampPage(page, totalPages);

        BrowseHolder holder = new BrowseHolder(BrowseHolder.Type.SEARCH, targetUuid, targetName, result.entries, page, totalPages, query);
        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, searchTitle(query));
        holder.setInventory(inv);

        int from = page * RESULTS_PER_PAGE;
        int to = Math.min(from + RESULTS_PER_PAGE, result.entries.size());
        for (int i = from; i < to; i++) {
            BrowseHolder.Entry entry = result.entries.get(i);
            ItemStack matched = result.items.get(i);
            inv.setItem(i - from, decorateSearchResult(matched, entry, targetName, self));
        }
        decorateNav(inv, holder);
        PlayerVaults.getInstance().getTL().searchResults()
                .title()
                .with("count", String.valueOf(result.entries.size()))
                .with("query", query)
                .send(viewer);

        viewer.openInventory(inv);
    }

    public static void openSamePage(@NotNull Player viewer, @NotNull BrowseHolder holder, int newPage) {
        if (holder.getType() == BrowseHolder.Type.BROWSE) {
            openBrowse(viewer, holder.getTargetUuid(), holder.getTargetName(), newPage);
        } else {
            openSearch(viewer, holder.getTargetUuid(), holder.getTargetName(),
                    holder.getQuery() == null ? "" : holder.getQuery(), newPage);
        }
    }

    private record SearchResult(List<BrowseHolder.Entry> entries, List<ItemStack> items) {}

    private static SearchResult performSearch(@NotNull Player viewer, @NotNull String targetUuid, boolean self, @NotNull String query) {
        String needle = ChatColor.stripColor(query).toLowerCase(Locale.ROOT).trim();
        List<BrowseHolder.Entry> entries = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        if (needle.isEmpty()) {
            return new SearchResult(entries, items);
        }

        List<Integer> numbers = new ArrayList<>(VaultManager.getInstance().getVaultNumbers(targetUuid));
        if (numbers.isEmpty()) {
            return new SearchResult(entries, items);
        }
        Collections.sort(numbers);

        YamlConfiguration file = VaultManager.getInstance().getPlayerVaultFile(targetUuid, true);
        if (file == null) {
            return new SearchResult(entries, items);
        }

        int hardLimit = RESULTS_PER_PAGE * 6;
        for (int number : numbers) {
            if (self && !VaultOperations.checkPerms(viewer, number)) {
                continue;
            }
            String data = file.getString("vault" + number);
            if (data == null) {
                continue;
            }
            ItemStack[] contents = CardboardBoxSerialization.fromStorage(data, targetUuid);
            if (contents == null) {
                continue;
            }
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack item = contents[slot];
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                if (!matches(item, needle)) {
                    continue;
                }
                entries.add(new BrowseHolder.Entry(number, slot));
                items.add(item.clone());
                if (entries.size() >= hardLimit) {
                    return new SearchResult(entries, items);
                }
            }
        }
        return new SearchResult(entries, items);
    }

    private static boolean matches(@NotNull ItemStack item, @NotNull String needle) {
        if (isNavIcon(item)) {
            return false;
        }
        String materialName = item.getType().name().toLowerCase(Locale.ROOT);
        if (materialName.contains(needle) || materialName.replace('_', ' ').contains(needle)) {
            return true;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        String paperDisplayName = ComponentText.invokeComponentGetter(meta, "displayName");
        if (paperDisplayName != null && paperDisplayName.toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        String paperItemName = ComponentText.invokeComponentGetter(meta, "itemName");
        if (paperItemName != null && paperItemName.toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }

        if (meta.hasDisplayName()) {
            String name = ChatColor.stripColor(meta.getDisplayName()).toLowerCase(Locale.ROOT);
            if (name.contains(needle)) {
                return true;
            }
        }

        List<String> paperLore = ComponentText.invokeLoreGetter(meta);
        if (paperLore != null) {
            for (String line : paperLore) {
                if (line != null && line.toLowerCase(Locale.ROOT).contains(needle)) {
                    return true;
                }
            }
        } else if (meta.hasLore() && meta.getLore() != null) {
            for (String lore : meta.getLore()) {
                if (lore == null) continue;
                if (ChatColor.stripColor(lore).toLowerCase(Locale.ROOT).contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNavIcon(@NotNull ItemStack item) {
        return item.isSimilar(VaultViewInfo.FILLER_ICON)
                || item.isSimilar(VaultViewInfo.NEXT_PAGE_ICON)
                || item.isSimilar(VaultViewInfo.PREVIOUS_PAGE_ICON)
                || item.isSimilar(VaultViewInfo.DISABLE_BAR_ICON);
    }

    private static List<Integer> collectBrowseVaultNumbers(@NotNull Player viewer, @NotNull String targetUuid, boolean self) {
        List<Integer> numbers = new ArrayList<>();
        if (self) {
            int max = Math.min(VaultOperations.countVaults(viewer),
                    PlayerVaults.getInstance().getMaxVaultAmountPermTest());
            for (int i = 1; i <= max; i++) {
                numbers.add(i);
            }
        } else {
            numbers.addAll(VaultManager.getInstance().getVaultNumbers(targetUuid));
            Collections.sort(numbers);
        }
        return numbers;
    }

    private static void fillContent(@NotNull Inventory inv, @NotNull List<BrowseHolder.Entry> entries, int page, @NotNull BrowseHolder.Type type, @NotNull String targetUuid) {
        if (type != BrowseHolder.Type.BROWSE) {
            return;
        }
        int from = page * RESULTS_PER_PAGE;
        int to = Math.min(from + RESULTS_PER_PAGE, entries.size());
        YamlConfiguration file = VaultManager.getInstance().getPlayerVaultFile(targetUuid, false);
        for (int i = from; i < to; i++) {
            BrowseHolder.Entry entry = entries.get(i);
            inv.setItem(i - from, buildVaultIcon(entry.vaultNumber(), file, targetUuid));
        }
    }

    private static ItemStack buildVaultIcon(int number, @Nullable YamlConfiguration file, @NotNull String targetUuid) {
        int storedCount = countItems(number, file, targetUuid);
        Material material = storedCount == 0 ? Material.CHEST : Material.ENDER_CHEST;
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Vault " + ChatColor.YELLOW + "#" + number);
            List<String> lore = new ArrayList<>(3);
            if (storedCount == 0) {
                lore.add(ChatColor.GRAY + "Empty");
            } else {
                lore.add(ChatColor.GRAY + "Stored items: " + ChatColor.WHITE + storedCount);
            }
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to open!");
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        icon.setAmount(Math.max(1, Math.min(64, number)));
        return icon;
    }

    private static int countItems(int number, @Nullable YamlConfiguration file, @NotNull String targetUuid) {
        if (file == null) return 0;
        String data = file.getString("vault" + number);
        if (data == null) return 0;
        ItemStack[] contents = CardboardBoxSerialization.fromStorage(data, targetUuid);
        if (contents == null) return 0;
        int count = 0;
        for (ItemStack stack : contents) {
            if (stack != null && stack.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }

    private static ItemStack decorateSearchResult(@NotNull ItemStack matched, @NotNull BrowseHolder.Entry entry, @NotNull String targetName, boolean self) {
        ItemStack display = matched.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            return display;
        }
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>(2);
        lore.add("");
        if (self) {
            lore.add(ChatColor.AQUA + "Found in: " + ChatColor.WHITE + "Vault #" + entry.vaultNumber());
        } else {
            lore.add(ChatColor.AQUA + "Found in: " + ChatColor.WHITE + targetName + ChatColor.GRAY + " / "
                    + ChatColor.WHITE + "Vault #" + entry.vaultNumber());
        }
        if (entry.sourceSlot() >= 0) {
            lore.add(ChatColor.DARK_AQUA + "Slot: " + ChatColor.WHITE + (entry.sourceSlot() + 1));
        }
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to open this vault!");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private static void decorateNav(@NotNull Inventory inv, @NotNull BrowseHolder holder) {
        for (int i = RESULTS_PER_PAGE; i < GUI_SIZE; i++) {
            inv.setItem(i, VaultViewInfo.FILLER_ICON.clone());
        }
        if (holder.getPage() > 0) {
            ItemStack prev = VaultViewInfo.PREVIOUS_PAGE_ICON.clone();
            prev.setAmount(holder.getPage());
            inv.setItem(PREV_SLOT, prev);
        }
        if (holder.getPage() < holder.getTotalPages() - 1) {
            ItemStack next = VaultViewInfo.NEXT_PAGE_ICON.clone();
            next.setAmount(holder.getPage() + 2);
            inv.setItem(NEXT_SLOT, next);
        }

        ItemStack info = new ItemStack(holder.getType() == BrowseHolder.Type.SEARCH
                ? Material.SPYGLASS : Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            int total = holder.getEntries().size();
            if (holder.getType() == BrowseHolder.Type.SEARCH) {
                meta.setDisplayName(ChatColor.AQUA + "Search Results");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Query: " + ChatColor.WHITE + (holder.getQuery() == null ? "" : holder.getQuery()));
                lore.add(ChatColor.GRAY + "Matches: " + ChatColor.WHITE + total);
                lore.add(ChatColor.GRAY + "Page: " + ChatColor.WHITE + (holder.getPage() + 1) + ChatColor.GRAY + " / " + ChatColor.WHITE + holder.getTotalPages());
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.GOLD + "Your Vaults");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + holder.getTargetName());
                lore.add(ChatColor.GRAY + "Vaults: " + ChatColor.WHITE + total);
                lore.add(ChatColor.GRAY + "Page: " + ChatColor.WHITE + (holder.getPage() + 1) + ChatColor.GRAY + " / " + ChatColor.WHITE + holder.getTotalPages());
                meta.setLore(lore);
            }
            info.setItemMeta(meta);
        }
        inv.setItem(INFO_SLOT, info);
    }

    private static int clampPage(int page, int totalPages) {
        if (page < 0) return 0;
        if (page >= totalPages) return totalPages - 1;
        return page;
    }

    private static String browseTitle(@NotNull String targetName, boolean self) {
        if (self) {
            return PlayerVaults.getInstance().getTL().browseTitle()
                    .with("player", targetName).getLegacy();
        }
        return PlayerVaults.getInstance().getTL().browseTitleOther()
                .with("player", targetName).getLegacy();
    }

    private static String searchTitle(@NotNull String query) {
        String trimmed = query.length() > 16 ? query.substring(0, 16) + "…" : query;
        return PlayerVaults.getInstance().getTL().searchTitle()
                .with("query", trimmed).getLegacy();
    }

    public static String resolveTargetUuid(@NotNull String identifier) {
        try {
            UUID parsed = UUID.fromString(identifier);
            return parsed.toString();
        } catch (IllegalArgumentException ignored) {
        }
        OfflinePlayer found = Bukkit.getPlayerExact(identifier);
        if (found == null) {
            found = Bukkit.getOfflinePlayer(identifier);
        }
        if (found != null) {
            return found.getUniqueId().toString();
        }
        return identifier;
    }

    public static boolean canUse(@NotNull Player player) {
        return player.hasPermission(Permission.COMMANDS_USE);
    }
}