package me.ram.bedwarsscoreboardaddon.addon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsPlayerKilledEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;

public class KillSoulItem implements Listener {

	private final Map<UUID, Map<Integer, ItemStack>> itemsToKeep = new HashMap<>();

	@EventHandler
	public void onPlayerKilled(BedwarsPlayerKilledEvent e) {
		if (!Config.killsoul_enabled || e.getKiller() == null || e.getPlayer() == null) {
			return;
		}
		Player killer = e.getKiller();
		Game game = e.getGame();
		Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
		if (game.getState() != GameState.RUNNING || arena == null || game.isSpectator(killer)) {
			return;
		}
		if (game.getPlayerTeam(e.getPlayer()) == null || game.getPlayerTeam(killer) == null) {
			return;
		}
		int killStreaks = arena.getPlayerGameStorage().getKillStreaks(killer.getName());
		boolean shouldGiveSoul = false;
		if (Config.killsoul_autodetect) {
			if (BedwarsUtil.isXpMode(game)) {
				return;
			}
			if (game.getTeams().size() >= 8 || killStreaks >= 3) {
				shouldGiveSoul = true;
			}
		} else if (killStreaks >= Config.killsoul_onkillstreak) {
			shouldGiveSoul = true;
		}
		if (shouldGiveSoul) {
			try {
				ItemStack soul = new ItemStack(Material.valueOf(Config.killsoul_item_material));
				ItemMeta meta = soul.getItemMeta();
				meta.setDisplayName(Config.killsoul_item_name);
				if (!Config.killsoul_item_lore.isEmpty()) {
					meta.setLore(Config.killsoul_item_lore);
				}
				soul.setItemMeta(meta);
				killer.getInventory().addItem(soul);
			} catch (IllegalArgumentException ignored) {
			}
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (!Config.killsoul_enabled || !Config.killsoul_no_drop) {
			return;
		}
		Player player = event.getEntity().getPlayer();
		if (player == null) {
			return;
		}
		Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
		if (game == null) {
			return;
		}
		if (BedwarsUtil.isXpMode(game) || game.getPlayerTeam(player) == null || game.isSpectator(player)) {
			return;
		}
		Material soulMaterial;
		try {
			soulMaterial = Material.valueOf(Config.killsoul_item_material);
		} catch (IllegalArgumentException e) {
			return;
		}
		PlayerInventory inventory = player.getInventory();
		Map<Integer, ItemStack> keptItems = new HashMap<>();
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack item = inventory.getItem(i);
			if (item != null && item.getType().equals(soulMaterial)) {
				keptItems.put(i, item);
				event.getDrops().remove(item);
			}
		}
		if (!keptItems.isEmpty()) {
			itemsToKeep.put(player.getUniqueId(), keptItems);
		}
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		if (!Config.killsoul_enabled || !Config.killsoul_no_drop) {
			return;
		}
		Player player = event.getPlayer();
		UUID playerUUID = player.getUniqueId();
		Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
		if (game == null || BedwarsUtil.isXpMode(game) || game.getPlayerTeam(player) == null || game.isSpectator(player)) {
			itemsToKeep.remove(playerUUID);
			return;
		}
		Map<Integer, ItemStack> keptItems = itemsToKeep.remove(playerUUID);
		if (keptItems == null) {
			return;
		}
		PlayerInventory inventory = player.getInventory();
		for (Map.Entry<Integer, ItemStack> entry : keptItems.entrySet()) {
			inventory.setItem(entry.getKey(), entry.getValue());
		}
	}
}
