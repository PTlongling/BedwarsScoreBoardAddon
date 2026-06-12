package me.ram.bedwarsscoreboardaddon.addon.items.bridgeegg;

import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;

import io.github.bedwarsrel.game.Game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class HuaYuTing implements SetBlock {

    public void start(Game game, Egg egg, Player player) {
        final Set<String> placed = new HashSet<>();
        final Location[] lastLoc = {null};
        final int[] count = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (egg.isDead()) {
                    this.cancel();
                    return;
                }
                Location loc = egg.getLocation();
                Location ground = loc.clone().subtract(0, 2, 0);

                if (BedwarsUtil.isProtectionLocation(game, loc)) {
                    egg.remove();
                    this.cancel();
                    return;
                }

                if (player.getLocation().distance(loc) > 2.0) {
                    if (count[0] >= Config.bridge_egg_max_blocks) {
                        this.cancel();
                        return;
                    }
                    if (lastLoc[0] == null || lastLoc[0].distance(ground) >= 2.0) {
                        String key = (int) ground.getX() + "," + (int) ground.getZ();
                        if (!placed.contains(key)) {
                            placed.add(key);
                            lastLoc[0] = ground.clone();
                            count[0] += placeCross(ground, game, player);
                            if (count[0] >= Config.bridge_egg_max_blocks) {
                                this.cancel();
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private int placeCross(Location center, Game game, Player player) {
        int[][] offsets = {{-1, 0, 0}, {0, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}};
        int n = 0;
        for (int[] off : offsets) {
            Location blockLoc = center.clone().add(off[0], off[1], off[2]);
            Block block = blockLoc.getBlock();
            if (BridgeEgg.canPlace(block, player, game, blockLoc)) {
                block.setType(Material.SANDSTONE);
                game.getRegion().addPlacedBlock(block, null);
                n++;
                if (BridgeEgg.sound != null) {
                    player.playSound(blockLoc, BridgeEgg.sound, 1.0f, 1.0f);
                }
            }
        }
        return n;
    }
}
