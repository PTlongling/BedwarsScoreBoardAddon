package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * 友方破坏踢出功能
 * 当玩家破坏队友脚下的方块时发出警告，超过次数后踢出
 */
public class FriendlyBreak implements Listener {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    @Getter
    private final Map<String, Integer> violationCounts;

    public FriendlyBreak(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        this.violationCounts = new HashMap<>();

        if (!Config.friendlybreak_enabled) {
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!Config.friendlybreak_enabled) {
            return;
        }

        Player breaker = event.getPlayer();

        // 检查破坏者是否在游戏中且存活
        if (!arena.isAlivePlayer(breaker)) {
            return;
        }

        Team breakerTeam = game.getPlayerTeam(breaker);
        if (breakerTeam == null) {
            return;
        }

        // 检查是否有队友站在被破坏的方块上
        for (Player teammate : breakerTeam.getPlayers()) {
            // 跳过自己
            if (teammate.equals(breaker)) {
                continue;
            }

            // 检查队友是否存活
            if (BedwarsUtil.isSpectator(game, teammate)) {
                continue;
            }

            // 检查队友是否站在这个方块上
            if (!teammate.getLocation().getBlock().getRelative(BlockFace.DOWN).equals(event.getBlock())) {
                continue;
            }

            // 找到受害者，处理违规
            handleViolation(breaker, teammate, breakerTeam);

            // 只处理第一个受害者，避免重复计数
            break;
        }
    }

    private void handleViolation(Player breaker, Player victim, Team team) {
        String breakerName = breaker.getName();
        int count = violationCounts.getOrDefault(breakerName, 0) + 1;
        violationCounts.put(breakerName, count);

        int maxCount = Config.friendlybreak_max_count;
        int remaining = maxCount - count;

        // 发送警告给受害者
        if (!Config.friendlybreak_victim_message.isEmpty()) {
            String victimMsg = Config.friendlybreak_victim_message
                    .replace("{player}", breaker.getName())
                    .replace("{victim}", victim.getName());
            victim.sendMessage(victimMsg);
        }

        // 检查是否应该发送警告
        if (Config.friendlybreak_warning_thresholds.contains(count)) {
            String warningMsg = Config.friendlybreak_warning_message
                    .replace("{player}", breaker.getName())
                    .replace("{victim}", victim.getName())
                    .replace("{count}", String.valueOf(count))
                    .replace("{max_count}", String.valueOf(maxCount))
                    .replace("{remaining}", String.valueOf(remaining));
            breaker.sendMessage(warningMsg);
        }

        // 检查是否超过最大次数，需要踢出
        if (count > maxCount) {
            violationCounts.remove(breakerName);
            kickPlayer(breaker, team, count);
        }
    }

    private void kickPlayer(Player player, Team team, int violationCount) {
        // 发送踢出消息给玩家
        if (!Config.friendlybreak_kick_message.isEmpty()) {
            player.sendMessage(Config.friendlybreak_kick_message);
        }

        // 广播踢出消息
        String broadcastMsg = Config.friendlybreak_broadcast_message
                .replace("{player}", player.getName())
                .replace("{count}", String.valueOf(violationCount))
                .replace("{team}", team.getName())
                .replace("{teamcolor}", team.getChatColor().toString());

        if (Config.friendlybreak_broadcast_to_game) {
            // 向所有游戏玩家广播
            for (Player gamePlayer : game.getPlayers()) {
                gamePlayer.sendMessage(broadcastMsg);
            }
        } else {
            // 仅向队伍广播
            for (Player teammate : team.getPlayers()) {
                if (!teammate.equals(player)) {
                    teammate.sendMessage(broadcastMsg);
                }
            }
        }

        // 延迟踢出，确保消息送达
        new BukkitRunnable() {
            @Override
            public void run() {
                player.kickPlayer(Config.friendlybreak_kick_reason);
            }
        }.runTaskLater(Main.getInstance(), 5L);
    }

    public void onEnd() {
        HandlerList.unregisterAll(this);
        violationCounts.clear();
    }
}
