package me.ram.bedwarsscoreboardaddon.addon;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import io.github.bedwarsrel.events.BedwarsGameStartedEvent;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.ScoreboardUtil;

public class HealthBar implements Listener {

    private final Arena arena;

    public HealthBar(Arena arena) {
        this.arena = arena;
        if (Config.health_bar_enabled) {
            Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        }
    }

    public void onEnd() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player target = (Player) entity;
        if (!arena.isAlivePlayer(target)) {
            return;
        }
        int health = toHealthInt(target.getHealth() - e.getFinalDamage());
        broadcastHealth(target, health);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player target = (Player) entity;
        if (!arena.isAlivePlayer(target)) {
            return;
        }
        int health = toHealthInt(target.getHealth() + e.getAmount());
        broadcastHealth(target, health);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player target = e.getPlayer();
        if (!arena.isAlivePlayer(target)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (target.isOnline() && arena.isAlivePlayer(target)) {
                broadcastHealth(target, toHealthInt(target.getHealth()));
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameStarted(BedwarsGameStartedEvent e) {
        if (!e.getGame().getName().equals(arena.getGame().getName())) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            for (Player observer : arena.getGame().getPlayers()) {
                ScoreboardUtil.sendShowHealthPacket(observer);
                for (Player target : arena.getGame().getPlayers()) {
                    ScoreboardUtil.sendHealthValuePacket(observer, target, toHealthInt(target.getHealth()));
                }
            }
        }, 5L);
    }

    private void broadcastHealth(Player target, int health) {
        for (Player observer : arena.getGame().getPlayers()) {
            ScoreboardUtil.sendHealthValuePacket(observer, target, health);
        }
    }

    private int toHealthInt(double health) {
        if (health < 0) health = 0;
        return Integer.valueOf(new DecimalFormat("##").format(health));
    }
}
