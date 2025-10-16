package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class TimeControlListener {
    private final PvPGame plugin;

    public TimeControlListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    public void startTimeControl() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    // Set time to day (1000 ticks = early morning)
                    if (world.getTime() > 12000 || world.getTime() < 1000) {
                        world.setTime(1000);
                    }

                    // Set weather to clear
                    if (world.hasStorm() || world.isThundering()) {
                        world.setStorm(false);
                        world.setThundering(false);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // Check every 5 seconds (100 ticks)
    }
}
