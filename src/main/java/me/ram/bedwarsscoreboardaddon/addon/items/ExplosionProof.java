package me.ram.bedwarsscoreboardaddon.addon.items;

import me.ram.bedwarsscoreboardaddon.config.Config;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.List;

public class ExplosionProof implements Listener {

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (!Config.explosion_proof_enabled) return;
        if (!(e.getEntity() instanceof TNTPrimed)) return;

        Location loc = e.getEntity().getLocation().getBlock().getLocation().add(0.5, 0.5, 0.5);
        Game game = BedwarsRel.getInstance().getGameManager().getGameByLocation(loc);
        if (game == null) return;

        boolean isLightTNT = e.getEntity().hasMetadata("BWLightTNT");

        List<Block> keep = new ArrayList<>();
        for (Block block : e.blockList()) {
            if (block.getType() == Material.GLASS || block.getType() == Material.STAINED_GLASS) {
                continue;
            }
            if (isLightTNT && !game.getRegion().isPlacedBlock(block)) {
                continue;
            }
            keep.add(block);
        }
        e.blockList().clear();
        e.blockList().addAll(keep);
    }
}
