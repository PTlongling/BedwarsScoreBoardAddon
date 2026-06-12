package me.ram.bedwarsscoreboardaddon.utils;

import io.github.bedwarsrel.BedwarsRel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TakeItemUtil {

    public static void takeItem(Player player, ItemStack stack) {
        if (BedwarsRel.getInstance().getCurrentVersion().startsWith("v1_8")) {
            ItemStack itemInHand = player.getInventory().getItemInHand();
            if (itemInHand != null && itemInHand.getType() == stack.getType()) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
                player.getInventory().setItemInHand(itemInHand);
                return;
            }
        } else {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (main != null && main.getType() == stack.getType()) {
                main.setAmount(main.getAmount() - 1);
                player.getInventory().setItemInMainHand(main);
                return;
            }
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off != null && off.getType() == stack.getType()) {
                off.setAmount(off.getAmount() - 1);
                player.getInventory().setItemInOffHand(off);
                return;
            }
        }
        ItemMeta meta = stack.getItemMeta();
        ItemStack remove = new ItemStack(stack.getType(), 1);
        remove.setItemMeta(meta);
        player.getInventory().removeItem(remove);
    }
}
