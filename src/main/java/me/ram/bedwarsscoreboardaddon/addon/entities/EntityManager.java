package me.ram.bedwarsscoreboardaddon.addon.entities;

import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;

import lombok.Getter;

import org.bukkit.GameMode;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EntityManager implements Listener {

    private static final Map<Entity, PetData> pets = new HashMap<>();
    private static BukkitTask teleportTask;
    private static BukkitTask targetTask;

    public static boolean isPet(Entity entity) {
        return pets.containsKey(entity);
    }

    public static Player getSpawner(Entity pet) {
        PetData data = pets.get(pet);
        return data != null ? data.getSpawner() : null;
    }

    public static Game getGame(Entity pet) {
        PetData data = pets.get(pet);
        return data != null ? data.getGame() : null;
    }

    public static void addPet(Entity entity, Game game, Player spawner) {
        if (pets.containsKey(entity)) return;
        pets.put(entity, new PetData(spawner, game));
        entity.setCustomName("§a§l[" + spawner.getDisplayName() + "§a§l] §b§l的宠物");
        if (entity instanceof Creature) {
            Player target = findNearestEnemy(entity, spawner, game);
            ((Creature) entity).setTarget(target);
            ((Creature) entity).setRemoveWhenFarAway(false);
        }
        if (entity instanceof Wolf) {
            ((Wolf) entity).setOwner(spawner);
            ((Wolf) entity).setTamed(true);
        }
        if (entity instanceof Skeleton) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isDead() || !entity.isValid()) { cancel(); return; }
                    entity.setFireTicks(0);
                }
            }.runTaskTimer(Main.getInstance(), 0L, 0L);
        }
        startTasks();
    }

    public static void removePet(Entity entity) {
        pets.remove(entity);
        if (pets.isEmpty()) stopTasks();
    }

    private static void startTasks() {
        if (teleportTask == null) {
            teleportTask = new BukkitRunnable() {
                @Override
                public void run() {
                    Iterator<Map.Entry<Entity, PetData>> it = pets.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Entity, PetData> entry = it.next();
                        Entity pet = entry.getKey();
                        Player owner = entry.getValue().getSpawner();
                        if (isInvalid(pet, owner)) { it.remove(); pet.remove(); continue; }
                        if (BedwarsUtil.isRespawning(owner)) continue;
                        if (pet instanceof Wolf && ((Wolf) pet).isSitting()) continue;
                        double maxDist = Config.spawner_max_distance;
                        if (pet.getLocation().distanceSquared(owner.getLocation()) > maxDist * maxDist) {
                            pet.teleport(owner);
                        }
                    }
                    if (pets.isEmpty()) stopTasks();
                }
            }.runTaskTimer(Main.getInstance(), 0L, Config.spawner_teleport_interval);
        }
        if (targetTask == null) {
            targetTask = new BukkitRunnable() {
                @Override
                public void run() {
                    pets.forEach((pet, data) -> {
                        Player owner = data.getSpawner();
                        if (isInvalid(pet, owner)) return;
                        if (pet instanceof Creature) {
                            ((Creature) pet).setTarget(findNearestEnemy(pet, owner, data.getGame()));
                        }
                    });
                }
            }.runTaskTimer(Main.getInstance(), 0L, Config.spawner_target_interval);
        }
    }

    private static void stopTasks() {
        if (teleportTask != null) { teleportTask.cancel(); teleportTask = null; }
        if (targetTask != null) { targetTask.cancel(); targetTask = null; }
    }

    private static boolean isInvalid(Entity pet, Player owner) {
        return owner == null || !owner.isOnline() || pet == null || pet.isDead() || !pet.isValid();
    }

    public static Player findNearestEnemy(Entity entity, Player spawner, Game game) {
        if (game == null) return null;
        Team spawnerTeam = game.getPlayerTeam(spawner);
        if (spawnerTeam == null) return null;
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player player : entity.getWorld().getPlayers()) {
            if (spawner.equals(player)) continue;
            if (game.isSpectator(player) || player.getGameMode() == GameMode.SPECTATOR) continue;
            if (spawnerTeam == game.getPlayerTeam(player)) continue;
            double dist = player.getLocation().distance(entity.getLocation());
            if (dist < 20 && dist < nearestDist) { nearest = player; nearestDist = dist; }
        }
        return nearest;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        removePet(e.getEntity());
    }

    @Getter
    private static class PetData {
        private final Player spawner;
        private final Game game;
        PetData(Player spawner, Game game) { this.spawner = spawner; this.game = game; }
    }
}
