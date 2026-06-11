package me.ram.bedwarsscoreboardaddon.addon;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.Utils;

public class GroupTeleport {

    private final Arena arena;
    private BukkitTask checkTask;
    private boolean triggered;

    public GroupTeleport(Arena arena) {
        this.arena = arena;
        this.triggered = false;

        if (!Config.group_teleport_enabled) {
            return;
        }

        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAndTrigger();
            }
        }.runTaskTimer(Main.getInstance(), 20L, 20L);
        arena.addGameTask(checkTask);
    }

    public void onEnd() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    private void checkAndTrigger() {
        if (triggered) {
            return;
        }
        Game game = arena.getGame();
        int timeLeft = game.getTimeLeft();
        int aliveCount = countAlivePlayers(game);

        if (timeLeft <= Config.group_teleport_time_threshold && aliveCount <= Config.group_teleport_player_threshold) {
            triggered = true;
            if (checkTask != null) {
                checkTask.cancel();
                checkTask = null;
            }
            startCountdown();
        }
    }

    private int countAlivePlayers(Game game) {
        int count = 0;
        for (Player player : game.getPlayers()) {
            if (!game.isSpectator(player)) {
                count++;
            }
        }
        return count;
    }

    private void startCountdown() {
        final int[] countdown = {5};
        new BukkitRunnable() {
            @Override
            public void run() {
                Game game = arena.getGame();
                if (countdown[0] > 0) {
                    String countStr = "§c" + countdown[0];
                    for (Player player : game.getPlayers()) {
                        Utils.sendTitle(player, 0, 25, 0, "§6§l集体传送", countStr);
                    }
                    countdown[0]--;
                } else {
                    cancel();
                    teleportAll();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }

    private void teleportAll() {
        Game game = arena.getGame();
        Location center = computeCenter(game);
        if (center == null) {
            return;
        }
        for (Player player : game.getPlayers()) {
            if (!game.isSpectator(player)) {
                player.teleport(center);
            }
        }
    }

    private Location computeCenter(Game game) {
        List<Location> spawns = new ArrayList<>();
        World world = null;
        for (Team team : game.getTeams().values()) {
            Location spawn = team.getSpawnLocation();
            if (spawn != null) {
                spawns.add(spawn);
                if (world == null) {
                    world = spawn.getWorld();
                }
            }
        }
        if (spawns.isEmpty() || world == null) {
            return null;
        }
        double x = 0, y = 0, z = 0;
        for (Location loc : spawns) {
            x += loc.getX();
            y += loc.getY();
            z += loc.getZ();
        }
        x /= spawns.size();
        y /= spawns.size();
        z /= spawns.size();
        return new Location(world, x, y, z);
    }
}
