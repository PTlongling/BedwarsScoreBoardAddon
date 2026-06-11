package me.ram.bedwarsscoreboardaddon.network;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.NetworkUtil;

public class UpdateCheck implements Listener {

	private static final String API_URL = "https://api.github.com/repos/PTlongling/BedwarsScoreBoardAddon/releases/latest";
	private static final String DOWNLOAD_URL = "https://github.com/PTlongling/BedwarsScoreBoardAddon/releases/latest";

	private static String latestVersion;

	public UpdateCheck() {
		Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
			if (Config.update_check_enabled) {
				Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
					fetchLatestVersion();
					Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
						if (Config.update_check_enabled && latestVersion != null && !latestVersion.equals(Main.getVersion())) {
							sendUpdateInfo(Bukkit.getConsoleSender());
						}
					}, 100L);
				});
			}
			Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
				if (Config.update_check_enabled) {
					fetchLatestVersion();
				}
			}, 20 * 86400, 20 * 86400);
		}, 5);
	}

	@EventHandler
	public void playerJoin(PlayerJoinEvent e) {
		if (Config.update_check_enabled && Config.update_check_report && latestVersion != null) {
			if (e.getPlayer().hasPermission("bedwarsscoreboardaddon.updatecheck") && !latestVersion.equals(Main.getVersion())) {
				sendUpdateInfo(e.getPlayer());
			}
		}
	}

	public static void upCheck(org.bukkit.command.CommandSender sender) {
		sender.sendMessage((String) Main.getInstance().getLocaleConfig().getLanguage("update_checking"));
		Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
			if (fetchLatestVersion()) {
				if (latestVersion.equals(Main.getVersion())) {
					sender.sendMessage((String) Main.getInstance().getLocaleConfig().getLanguage("no_update"));
				} else {
					sendUpdateInfo(sender);
				}
			} else {
				sender.sendMessage((String) Main.getInstance().getLocaleConfig().getLanguage("update_check_failed"));
			}
		});
	}

	private static void sendUpdateInfo(org.bukkit.command.CommandSender sender) {
		Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
			sender.sendMessage("§f=====================================================");
			sender.sendMessage("§7 ");
			sender.sendMessage("               §aBedwarsScoreBoardAddon");
			sender.sendMessage("§7 ");
			sender.sendMessage("  §e" + Main.getInstance().getLocaleConfig().getLanguage("update_info"));
			sender.sendMessage("§7 ");
			sender.sendMessage("  §f" + Main.getInstance().getLocaleConfig().getLanguage("running_version") + ": §a" + Main.getVersion());
			sender.sendMessage("  §f" + Main.getInstance().getLocaleConfig().getLanguage("update_version") + ": §a" + latestVersion);
			sender.sendMessage("§7 ");
			sender.sendMessage("  §f" + Main.getInstance().getLocaleConfig().getLanguage("update_download") + ": §b§n" + DOWNLOAD_URL);
			sender.sendMessage("§7 ");
			sender.sendMessage("§f=====================================================");
		}, 5L);
	}

	private static boolean fetchLatestVersion() {
		String document = NetworkUtil.getDocument(API_URL);
		if (document == null || document.isEmpty()) {
			return false;
		}
		String parsed = parseTagName(document);
		if (parsed == null) {
			return false;
		}
		latestVersion = parsed;
		return true;
	}

	private static String parseTagName(String json) {
		int idx = json.indexOf("\"tag_name\"");
		if (idx < 0) {
			return null;
		}
		int colon = json.indexOf(":", idx);
		if (colon < 0) {
			return null;
		}
		int start = json.indexOf("\"", colon + 1);
		if (start < 0) {
			return null;
		}
		int end = json.indexOf("\"", start + 1);
		if (end < 0) {
			return null;
		}
		String tag = json.substring(start + 1, end);
		// Strip leading 'v' so "v2.13.2" becomes "2.13.2"
		if (tag.startsWith("v") || tag.startsWith("V")) {
			tag = tag.substring(1);
		}
		return tag;
	}
}
