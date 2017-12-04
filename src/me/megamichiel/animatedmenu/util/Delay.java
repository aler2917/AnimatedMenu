package me.megamichiel.animatedmenu.util;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.placeholder.ctx.ParsingContext;
import me.megamichiel.animationlib.util.ParsingNagger;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Delay implements Runnable {

    private final String id;

    private final Map<UUID, Long> delays = new HashMap<>();
    private final StringBundle delayMessage;
    private final long delay;
    private long result;

    public Delay(AnimatedMenuPlugin plugin, String id, String delayMessage, long delay) {
        this.id = id;

        if (delayMessage != null) {
            this.delayMessage = StringBundle.parse(new MessageNagger(plugin), delayMessage).colorAmpersands();
        } else this.delayMessage = null;
        this.delay = delay;

        // Remove expired delays every minute to keep the map at a reasonable size
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, 1200L, 1200L);
    }

    public long getDelay() {
        return delay;
    }

    public String getId() {
        return id;
    }

    public Map<UUID, Long> save() {
        if (delays.isEmpty()) {
            return Collections.emptyMap();
        }
        long time = System.currentTimeMillis(), delay;
        Map<UUID, Long> save = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : delays.entrySet()) {
            if (time < (delay = entry.getValue())) {
                save.put(entry.getKey(), delay);
            }
        }
        return save;
    }

    public void load(Map<UUID, Long> map) {
        delays.putAll(map);
    }

    public boolean test(Player player) {
        long time = System.currentTimeMillis();

        AtomicBoolean bool = new AtomicBoolean();
        delays.compute(player.getUniqueId(), (id, timer) -> {
            if (timer == null || time >= timer) {
                bool.set(true);
                return time + delay;
            } else if (delayMessage != null) {
                result = timer - time;
                player.sendMessage(delayMessage.toString(player));
            }
            return timer;
        });

        return bool.get();
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        delays.values().removeIf(timer -> time >= timer);
    }

    private class MessageNagger implements ParsingNagger, ParsingContext {

        private final Nagger nagger;

        private MessageNagger(Nagger nagger) {
            this.nagger = nagger;
        }

        @Override
        public ParsingContext context() {
            return this;
        }

        @Override
        public void nag(String s) {
            nagger.nag(s);
        }

        @Override
        public void nag(Throwable throwable) {
            nagger.nag(throwable);
        }

        @Override
        public IPlaceholder<?> parse(String s) {
            switch (s) {
                case "hoursleft":   return (n, p) ->  result / 3600_000;
                case "minutesleft": return (n, p) -> (result /   60_000) % 60;
                case "secondsleft": return (n, p) -> (result /    1_000) % 60;
                case "ticksleft":   return (n, p) -> (result /       50) % 20;
                default:
                    return nagger instanceof ParsingNagger ? ((ParsingNagger) nagger).context().parse(s) : null;
            }
        }
    }
}
