package me.ram.bedwarsscoreboardaddon.addon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.Utils;

public class RandomEvents {

    public static final RandomEvents PLAYERS_SPEED = new RandomEvents("全员速度提升", PotionEffectType.SPEED, "§4§l全体90秒速度加成");
    public static final RandomEvents PLAYERS_JUMP_BOOST = new RandomEvents("全员跳跃提升", PotionEffectType.JUMP, "§4§l全体90秒跳跃II加成");
    public static final RandomEvents PLAYERS_STRENGTH = new RandomEvents("全员力量提升", PotionEffectType.INCREASE_DAMAGE, "§4§l全体90秒力量加成");

    @Getter
    private final String eventName;
    @Getter
    private final PotionEffectType effectType;
    @Getter
    private final String subtitle;

    private final Arena arena;
    private List<RandomEvents> eventQueue;
    private BukkitTask task;

    // Constructor for static event constants
    private RandomEvents(String eventName, PotionEffectType effectType, String subtitle) {
        this.eventName = eventName;
        this.effectType = effectType;
        this.subtitle = subtitle;
        this.arena = null;
    }

    // Constructor for arena instance
    public RandomEvents(Arena arena) {
        this.eventName = null;
        this.effectType = null;
        this.subtitle = null;
        this.arena = arena;

        if (!Config.random_event_enabled) {
            return;
        }

        eventQueue = new ArrayList<>();
        eventQueue.add(PLAYERS_SPEED);
        eventQueue.add(PLAYERS_JUMP_BOOST);
        eventQueue.add(PLAYERS_STRENGTH);
        Collections.shuffle(eventQueue);

        long intervalTicks = (long) Config.random_event_interval * 20L;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                triggerNextEvent();
            }
        }.runTaskTimer(Main.getInstance(), intervalTicks, intervalTicks);
        arena.addGameTask(task);
    }

    public void onEnd() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void triggerNextEvent() {
        if (eventQueue == null || eventQueue.isEmpty()) {
            return;
        }
        RandomEvents event = eventQueue.remove(0);
        eventQueue.add(event);

        for (Player player : arena.getGame().getPlayers()) {
            Utils.sendTitle(player, 0, 60, 0, "§r", event.getSubtitle());
            if (event.getEffectType() != null) {
                player.addPotionEffect(new PotionEffect(event.getEffectType(), 90 * 20, 0));
            }
        }
    }
}
