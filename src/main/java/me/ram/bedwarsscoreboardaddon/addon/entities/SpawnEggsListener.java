package me.ram.bedwarsscoreboardaddon.addon.entities;

import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.TakeItemUtil;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnEggsListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.isCancelled() && (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM
                || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)) {
            e.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack handItem = e.getItem();
        if (handItem == null || (handItem.getType() != Material.MONSTER_EGG && handItem.getType() != Material.MONSTER_EGGS)) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING || game.isSpectator(player)) return;

        EntityType entityType;
        switch (handItem.getDurability()) {
            case 97:
                if (!Config.silverfish_spawner_enabled) return;
                entityType = EntityType.SILVERFISH;
                break;
            case 51:
                if (!Config.skeleton_spawner_enabled) return;
                entityType = EntityType.SKELETON;
                break;
            case 52:
                if (!Config.spider_spawner_enabled) return;
                entityType = EntityType.SPIDER;
                break;
            case 95:
                if (!Config.wolf_spawner_enabled) return;
                entityType = EntityType.WOLF;
                break;
            default:
                return;
        }

        e.setCancelled(true);
        TakeItemUtil.takeItem(player, handItem);

        Location spawnLoc = getSafeSpawnLocation(e.getClickedBlock().getLocation(), e.getBlockFace());
        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);
        EntityManager.addPet(entity, game, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTargetPlayer(EntityTargetLivingEntityEvent e) {
        Entity entity = e.getEntity();
        if (!EntityManager.isPet(entity)) return;
        if (!(e.getTarget() instanceof Player)) return;

        Player target = (Player) e.getTarget();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(target);
        if (game == null) return;
        Player spawner = EntityManager.getSpawner(entity);
        if (spawner == null) return;

        Player nearest = EntityManager.findNearestEnemy(entity, spawner, game);
        if (nearest != null) {
            e.setTarget(nearest);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMonsterAttack(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (!EntityManager.isPet(damager)) return;
        if (!(e.getEntity() instanceof Player)) return;

        Player target = (Player) e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(target);
        if (game == null) return;
        Player spawner = EntityManager.getSpawner(damager);
        if (spawner == null) return;

        io.github.bedwarsrel.game.Team spawnerTeam = game.getPlayerTeam(spawner);
        io.github.bedwarsrel.game.Team targetTeam = game.getPlayerTeam(target);
        if (game.isSpectator(target) || target.getGameMode() == org.bukkit.GameMode.SPECTATOR
                || spawner == target || spawnerTeam == null || targetTeam == null || spawnerTeam == targetTeam) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttackPet(EntityDamageByEntityEvent e) {
        if (EntityManager.isPet(e.getEntity())) {
            e.setCancelled(false);
        }
    }

    @EventHandler
    public void onPetDeath(EntityDeathEvent e) {
        Entity entity = e.getEntity();
        if (!EntityManager.isPet(entity)) return;
        e.setDroppedExp(0);
        e.getDrops().clear();

        if (entity.getType() == EntityType.WOLF) return;

        Player spawner = EntityManager.getSpawner(entity);
        if (spawner == null) return;

        org.bukkit.event.entity.EntityDamageEvent dmg = entity.getLastDamageCause();
        String cause = "死了";
        if (dmg != null) {
            switch (dmg.getCause()) {
                case ENTITY_EXPLOSION: case BLOCK_EXPLOSION: cause = "炸死了"; break;
                case ENTITY_ATTACK: cause = "被杀死了"; break;
                case FALL: cause = "摔死了"; break;
                case PROJECTILE: cause = "射死了"; break;
                case VOID: cause = "掉出了世界"; break;
                default: break;
            }
        }
        Player killer = e.getEntity().getKiller();
        String msg = entity.getCustomName() + "§f" + cause;
        if (killer != null) msg = entity.getCustomName() + "§f被" + killer.getDisplayName() + "§f" + cause;
        spawner.sendMessage(msg);
    }

    @EventHandler
    public void onSilverfishChangeBlock(EntityChangeBlockEvent e) {
        if (e.getEntity().getType() == EntityType.SILVERFISH && EntityManager.isPet(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    private Location getSafeSpawnLocation(Location base, BlockFace face) {
        Location loc = base.clone();
        switch (face) {
            case UP: loc.add(0, 1, 0); break;
            case DOWN: loc.add(0, -1, 0); break;
            case NORTH: loc.add(0, 0, -1); break;
            case SOUTH: loc.add(0, 0, 1); break;
            case EAST: loc.add(1, 0, 0); break;
            case WEST: loc.add(-1, 0, 0); break;
            default: loc.add(0, 1, 0); break;
        }
        while (!loc.getBlock().isEmpty()) {
            loc.add(0, 1, 0);
        }
        return loc;
    }
}
