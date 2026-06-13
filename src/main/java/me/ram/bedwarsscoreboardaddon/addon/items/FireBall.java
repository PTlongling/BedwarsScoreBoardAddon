package me.ram.bedwarsscoreboardaddon.addon.items;

import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.FireballUtil;
import me.ram.bedwarsscoreboardaddon.utils.TakeItemUtil;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class FireBall implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!Config.fireball_enabled) return;
        ItemStack handItem = e.getItem();
        if (handItem == null || handItem.getType() != Material.FIREBALL) return;
        e.setCancelled(true);
        if (e.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.isSpectator(player)) return;

        Vector direction = player.getEyeLocation().getDirection();
        Fireball fireball = player.launchProjectile(Fireball.class);
        FireballUtil.setDirection(fireball, direction);
        try { fireball.setBounce(false); } catch (NoSuchMethodError ignored) {}
        fireball.setShooter(player);
        fireball.setYield(Config.fireball_yield);
        fireball.setIsIncendiary(false);
        fireball.setVelocity(fireball.getDirection().multiply(Config.fireball_velocity));
        fireball.setMetadata("BWFireBall", new FixedMetadataValue(Main.getInstance(), game.getName() + "." + player.getName()));
        TakeItemUtil.takeItem(player, handItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockIgnite(BlockIgniteEvent e) {
        Entity entity = e.getIgnitingEntity();
        if (entity instanceof Fireball && entity.hasMetadata("BWFireBall")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireballExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;
        ProjectileSource source = ((Fireball) e.getEntity()).getShooter();
        if (!(source instanceof Player)) return;
        if (!e.getEntity().hasMetadata("BWFireBall")) return;
        e.blockList().clear();
    }

    @EventHandler
    public void onFireballDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (!damager.hasMetadata("BWFireBall")) return;
        if (!(e.getEntity() instanceof Player && damager instanceof Fireball)) return;

        Player player = (Player) e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) return;

        String meta = damager.getMetadata("BWFireBall").get(0).asString();
        String[] parts = meta.split("\\.");
        if (parts.length >= 2) {
            Player shooter = org.bukkit.Bukkit.getPlayer(parts[1]);
            if (shooter != null) {
                Team playerTeam = game.getPlayerTeam(player);
                Team shooterTeam = game.getPlayerTeam(shooter);
                if (shooterTeam != null && shooterTeam == playerTeam) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        e.setDamage(Config.fireball_damage);
    }
}
