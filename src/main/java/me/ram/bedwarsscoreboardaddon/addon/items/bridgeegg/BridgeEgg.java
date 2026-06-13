package me.ram.bedwarsscoreboardaddon.addon.items.bridgeegg;

import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.TakeItemUtil;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class BridgeEgg implements Listener {

    public static Sound sound;
    private final Map<Integer, SetBlock> modeMap;

    public BridgeEgg() {
        modeMap = new HashMap<>();
        modeMap.put(1, new Original());
        modeMap.put(2, new HuaYuTing());
        sound = getSound("BLOCK_STONE_BREAK", "DIG_STONE");
    }

    public static boolean canPlace(Block block, Player player, Game game, Location location) {
        return block.getType() == Material.AIR && BedwarsUtil.isCanPlace(game, location);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!Config.bridge_egg_enabled) return;
        SetBlock strategy = modeMap.get(Config.bridge_egg_mode);
        if (strategy == null) return;

        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

        Player player = e.getPlayer();
        ItemStack handItem = e.getItem();
        if (handItem == null || handItem.getType() != Material.EGG) return;

        ItemMeta meta = handItem.getItemMeta();
        if (meta == null) return;
        String displayName = meta.hasDisplayName() ? ColorUtil.removeColor(meta.getDisplayName()) : "";
        String cfgName = Config.bridge_egg_name == null ? "" : Config.bridge_egg_name;
        if (!(cfgName.equals(displayName) || (displayName.isEmpty() && cfgName.isEmpty()))) return;

        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) return;

        e.setCancelled(true);
        Egg egg = player.launchProjectile(Egg.class);
        try { egg.setBounce(false); } catch (NoSuchMethodError ignored) {}
        egg.setShooter(player);
        strategy.start(game, egg, player);
        TakeItemUtil.takeItem(player, handItem);
    }

    private static Sound getSound(String modern, String legacy) {
        try {
            return Sound.valueOf(modern);
        } catch (IllegalArgumentException e) {
            try {
                return Sound.valueOf(legacy);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }
}
