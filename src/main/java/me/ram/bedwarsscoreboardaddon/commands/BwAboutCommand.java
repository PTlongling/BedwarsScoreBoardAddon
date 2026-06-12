package me.ram.bedwarsscoreboardaddon.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.ram.bedwarsscoreboardaddon.Main;

public class BwAboutCommand implements Listener, CommandExecutor {

    private void sendAbout(CommandSender sender) {
        sender.sendMessage("§f=====================================================");
        sender.sendMessage("");
        sender.sendMessage("§b               BedwarsScoreBoardAddon");
        sender.sendMessage("");
        sender.sendMessage("§f  " + Main.getInstance().getLocaleConfig().getLanguage("version") + ": §a" + Main.getVersion());
        sender.sendMessage("");
        sender.sendMessage("§f  " + Main.getInstance().getLocaleConfig().getLanguage("author") + ": §aRam");
        sender.sendMessage("§f  §7Modified by §askyearth1");
        sender.sendMessage("");
        sender.sendMessage("§f=====================================================");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sendAbout(sender);
        return true;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("/bw about") || msg.equalsIgnoreCase("/bedwars about")) {
            event.setCancelled(true);
            sendAbout(event.getPlayer());
        }
    }
}
