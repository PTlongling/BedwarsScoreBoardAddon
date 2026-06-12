package me.ram.bedwarsscoreboardaddon.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;

public class BedwarsUtil {

	public static boolean isRespawning(Player player) {
		Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
		if (game == null) {
			return false;
		}
		return isRespawning(game, player);
	}

	public static boolean isRespawning(Game game, Player player) {
		Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
		if (arena == null) {
			return false;
		}
		return arena.getRespawn().isRespawning(player);
	}

	public static boolean isSpectator(Player player) {
		Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
		if (game == null) {
			return false;
		}
		return isSpectator(game, player);
	}

	public static boolean isSpectator(Game game, Player player) {
		return game.isSpectator(player) || isRespawning(game, player);
	}

	public static boolean isDieOut(Game game, Team team) {
		if (!team.isDead(game)) {
			return false;
		}
		for (Player player : team.getPlayers()) {
			if (!game.isSpectator(player)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isXpMode(Game game) {
		return Config.isBedwarsXPEnabled && ldcr.BedwarsXP.Config.isGameEnabledXP(game.getName());
	}

	public static boolean isCanPlace(Game game, Location location) {
		if (isProtectionLocation(game, location)) return false;
		for (Entity entity : location.getWorld().getNearbyEntities(location.clone().add(0.5, 1, 0.5), 0.5, 1, 0.5)) {
			if (entity instanceof Player && !isSpectator(game, (Player) entity)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isProtectionLocation(Game game, Location location) {
		if (!game.getRegion().isInRegion(location)) return true;
		Block block = location.getBlock();
		if (Config.spawn_no_build_spawn_enabled) {
			for (Team team : game.getTeams().values()) {
				Location spawn = team.getSpawnLocation();
				if (spawn != null && spawn.distanceSquared(block.getLocation().clone().add(0.5, 0, 0.5))
						<= Math.pow(Config.spawn_no_build_spawn_range, 2)) {
					return true;
				}
			}
		}
		return false;
	}
}
