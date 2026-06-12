package me.ram.bedwarsscoreboardaddon.addon.items;

import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.TakeItemUtil;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class TNT implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!Config.tnt_enabled) return;
        Player player = e.getPlayer();
        ItemStack handItem = e.getItemInHand();
        if (handItem == null || handItem.getType() != Material.TNT) return;

        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.isSpectator(player)) return;

        e.setBuild(true);
        e.setCancelled(false);
        e.getBlock().setType(Material.AIR);

        TNTPrimed tnt;
        if (Config.tnt_offsetfix) {
            tnt = (TNTPrimed) player.getWorld().spawnEntity(
                    e.getBlock().getLocation().add(0.5, 0, 0.5), EntityType.PRIMED_TNT);
        } else {
            tnt = (TNTPrimed) player.getWorld().spawnEntity(
                    e.getBlock().getLocation(), EntityType.PRIMED_TNT);
        }
        if (tnt == null) return;
        tnt.setYield(Config.tnt_yield);
        tnt.setIsIncendiary(false);
        tnt.setFuseTicks(Config.tnt_fuse_ticks);
        tnt.setMetadata("BWLightTNT", new FixedMetadataValue(Main.getInstance(), game.getName() + "." + player.getName()));
        TakeItemUtil.takeItem(player, handItem);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (!(damager instanceof TNTPrimed) || !damager.hasMetadata("BWLightTNT")) return;
        if (!(e.getEntity() instanceof Player)) return;

        Player player = (Player) e.getEntity();
        if (player.isDead()) return;

        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING || game.isSpectator(player)) return;

        String meta = damager.getMetadata("BWLightTNT").get(0).asString();
        String[] parts = meta.split("\\.");
        if (parts.length >= 2) {
            Player placer = Bukkit.getPlayer(parts[1]);
            if (placer != null) {
                Team playerTeam = game.getPlayerTeam(player);
                Team placerTeam = game.getPlayerTeam(placer);
                if (placerTeam != null && playerTeam == placerTeam) {
                    e.setCancelled(true);
                    return;
                }
                game.setPlayerDamager(player, placer);
            }
        }

        double distance = player.getLocation().distance(damager.getLocation());
        if (Config.tnt_killable_enabled && distance <= Config.tnt_killable_distance) {
            e.setCancelled(true);
            player.setHealth(0);
        } else {
            e.setDamage(Config.tnt_damage);
        }
    }
}
