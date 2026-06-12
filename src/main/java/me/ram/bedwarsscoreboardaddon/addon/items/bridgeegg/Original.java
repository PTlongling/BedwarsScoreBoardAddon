package me.ram.bedwarsscoreboardaddon.addon.items.bridgeegg;

import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Original implements SetBlock {

    public void start(Game game, Egg egg, Player player) {
        new BukkitRunnable() {
            int placed = 0;

            @Override
            public void run() {
                if (!egg.isDead()) {
                    new BukkitRunnable() {
                        final Location location = egg.getLocation().add(0, -1, 0);

                        @Override
                        public void run() {
                            if (game.isOverSet() || game.getState() != GameState.RUNNING) {
                                this.cancel();
                                return;
                            }
                            location.setX((int) location.getX());
                            location.setY((int) location.getY());
                            location.setZ((int) location.getZ());

                            List<Location> locs = new ArrayList<>();
                            locs.add(location);
                            Vector v = egg.getVelocity();
                            double ax = Math.abs(v.getX());
                            double ay = Math.abs(v.getY());
                            double az = Math.abs(v.getZ());
                            if (ay < ax || ay < az) {
                                locs.add(offset(location, -1, 0, -1));
                                locs.add(offset(location, -1, 0, 0));
                                locs.add(offset(location, 0, 0, -1));
                            } else {
                                locs.add(offset(location, 0, 1, 0));
                                locs.add(offset(location, -1, 1, -1));
                                locs.add(offset(location, -1, 1, 0));
                                locs.add(offset(location, 0, 1, -1));
                                locs.add(offset(location, -1, 0, -1));
                                locs.add(offset(location, -1, 0, 0));
                                locs.add(offset(location, 0, 0, -1));
                            }
                            for (Location loc : locs) {
                                Block block = loc.getBlock();
                                if (BridgeEgg.canPlace(block, player, game, loc) && placed <= Config.bridge_egg_max_blocks) {
                                    placed++;
                                    block.setType(Material.SANDSTONE);
                                    game.getRegion().addPlacedBlock(block, null);
                                    if (BridgeEgg.sound != null) {
                                        player.playSound(loc, BridgeEgg.sound, 1.0f, 1.0f);
                                    }
                                }
                            }
                        }
                    }.runTaskLater(Main.getInstance(), 5L);
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 0L);
    }

    private Location offset(Location base, int x, int y, int z) {
        return base.getBlock().getLocation().add(x, y, z);
    }
}
