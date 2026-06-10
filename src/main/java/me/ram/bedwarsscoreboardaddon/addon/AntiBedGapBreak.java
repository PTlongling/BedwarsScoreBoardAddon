package me.ram.bedwarsscoreboardaddon.addon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 防隔空破床功能
 * 通过检查玩家视线来防止隔着方块破坏床
 */
public class AntiBedGapBreak {

    private static final Set<Material> TRANSPARENT_MATERIALS = new HashSet<>();

    static {
        TRANSPARENT_MATERIALS.add(Material.AIR);
        TRANSPARENT_MATERIALS.add(Material.WATER);
        TRANSPARENT_MATERIALS.add(Material.STATIONARY_WATER);
        TRANSPARENT_MATERIALS.add(Material.LAVA);
        TRANSPARENT_MATERIALS.add(Material.STATIONARY_LAVA);
        TRANSPARENT_MATERIALS.add(Material.GLASS);
        TRANSPARENT_MATERIALS.add(Material.STAINED_GLASS);
        TRANSPARENT_MATERIALS.add(Material.THIN_GLASS);
        TRANSPARENT_MATERIALS.add(Material.STAINED_GLASS_PANE);
    }

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private PacketListener packetListener;

    public AntiBedGapBreak(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        if (!Config.antibedgapbreak_enabled) {
            return;
        }
        registerPacketListener();
    }

    public void onEnd() {
        if (packetListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
            packetListener = null;
        }
    }

    private void registerPacketListener() {
        packetListener = new PacketAdapter(Main.getInstance(), ListenerPriority.HIGHEST, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent e) {
                if (!Config.antibedgapbreak_enabled) {
                    return;
                }

                if (e.getPacketType() != PacketType.Play.Client.BLOCK_DIG) {
                    return;
                }

                Player player = e.getPlayer();
                if (player == null) {
                    return;
                }

                // 检查游戏状态和玩家状态
                if (game.getState() != GameState.RUNNING || BedwarsUtil.isSpectator(game, player)) {
                    return;
                }

                PacketContainer packet = e.getPacket();
                EnumWrappers.PlayerDigType digType = packet.getPlayerDigTypes().read(0);

                // 仅拦截完成破坏的数据包
                if (digType != EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
                    return;
                }

                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                Location blockLocation = pos.toLocation(player.getWorld());
                Block brokenBlock = blockLocation.getBlock();

                // 只检测床方块
                if (brokenBlock.getType() != game.getTargetMaterial()) {
                    return;
                }

                // 使用 Bukkit 原生方法获取视线内的方块
                List<Block> lineOfSight = player.getLineOfSight(TRANSPARENT_MATERIALS, 6);

                if (lineOfSight.isEmpty()) {
                    return;
                }

                // 获取玩家视线中最后一个可见的方块
                Block lastVisibleBlock = lineOfSight.get(lineOfSight.size() - 1);

                // 检查视线终点是否是被破坏的床或其相邻床块
                if (isSameBed(brokenBlock, lastVisibleBlock)) {
                    return;
                }

                // 阻止破坏并通知玩家
                e.setCancelled(true);
                brokenBlock.getState().update(true);

                if (!Config.antibedgapbreak_message.isEmpty()) {
                    player.sendMessage(Config.antibedgapbreak_message);
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
    }

    /**
     * 检查两个方块是否属于同一张床
     * 床是由两个相邻方块组成的
     */
    private boolean isSameBed(Block block1, Block block2) {
        if (block1.equals(block2)) {
            return true;
        }

        // 如果视线终点也是床，检查是否相邻（床的另一半）
        if (block2.getType() == game.getTargetMaterial()) {
            int dx = Math.abs(block1.getX() - block2.getX());
            int dy = Math.abs(block1.getY() - block2.getY());
            int dz = Math.abs(block1.getZ() - block2.getZ());

            // 床的两半在同一水平面上相邻
            return dy == 0 && (dx + dz) == 1;
        }

        return false;
    }
}
