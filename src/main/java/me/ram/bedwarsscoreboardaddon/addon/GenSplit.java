package me.ram.bedwarsscoreboardaddon.addon;

import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;

public class GenSplit implements Listener {

	@Getter
	private final Game game;
	@Getter
	private final Arena arena;

	public GenSplit(Arena arena) {
		this.arena = arena;
		this.game = arena.getGame();
		Main.getInstance().getServer().getPluginManager().registerEvents(this, Main.getInstance());
	}

	public void onEnd() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler
	public void onResPickup(PlayerPickupItemEvent e) {
		if (!Config.resource_gensplit_enabled || e.isCancelled() || e.getItem() == null) {
			return;
		}
		ItemStack itemStack = e.getItem().getItemStack();
		if (itemStack == null) {
			return;
		}
		boolean matches = false;
		for (String materialName : Config.resource_gensplit_items) {
			try {
				if (itemStack.getType().equals(Material.valueOf(materialName))) {
					matches = true;
					break;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		if (!matches) {
			return;
		}
		Player player = e.getPlayer();
		if (!arena.isAlivePlayer(player)) {
			return;
		}
		Team team = game.getPlayerTeam(player);
		if (team == null) {
			return;
		}
		double range = Config.resource_gensplit_range;
		Collection<Entity> nearby = player.getLocation().getWorld().getNearbyEntities(player.getLocation(), range, range, 2.0);
		for (Entity entity : nearby) {
			if (!(entity instanceof Player)) {
				continue;
			}
			Player teammate = (Player) entity;
			if (teammate.getUniqueId().equals(player.getUniqueId())) {
				continue;
			}
			if (!arena.isAlivePlayer(teammate)) {
				continue;
			}
			if (!team.equals(game.getPlayerTeam(teammate))) {
				continue;
			}
			teammate.getInventory().addItem(itemStack.clone());
		}
	}
}
