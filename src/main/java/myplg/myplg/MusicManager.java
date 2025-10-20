package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicManager {
    private final PvPGame plugin;
    private final Map<Player, BukkitTask> lobbyMusicTasks;
    private final Map<Player, Integer> lobbyMusicIndex;
    private List<String> lobbySongs;
    private List<String> gameSongs;
    private BukkitTask gameMusicTask;
    private int currentGameSongIndex = 0;

    public MusicManager(PvPGame plugin) {
        this.plugin = plugin;
        this.lobbyMusicTasks = new HashMap<>();
        this.lobbyMusicIndex = new HashMap<>();
        loadSongs();
    }

    /**
     * Load song names from resourcepack
     * These must match the sounds.json definitions in the resourcepack
     */
    private void loadSongs() {
        lobbySongs = new ArrayList<>();
        gameSongs = new ArrayList<>();

        // Lobby songs (must match resourcepack sounds.json)
        lobbySongs.add("lobbyBGM1");
        lobbySongs.add("lobbyBGM2");
        lobbySongs.add("lobbyBGM3");
        lobbySongs.add("lobbyBGM4");
        lobbySongs.add("lobbyBGM5");

        // Game songs (must match resourcepack sounds.json)
        gameSongs.add("gameBGM");

        plugin.getLogger().info("=== Music System Loaded ===");
        plugin.getLogger().info("Lobby songs: " + String.join(", ", lobbySongs));
        plugin.getLogger().info("Game songs: " + String.join(", ", gameSongs));
        plugin.getLogger().info("Note: Music files must be in the client-side resourcepack!");
        plugin.getLogger().info("===========================");
    }

    /**
     * Start playing lobby music for a player
     * Will loop through all songs in the lobby folder continuously
     */
    public void startLobbyMusic(Player player) {
        if (lobbySongs.isEmpty()) {
            plugin.getLogger().warning("No lobby songs found!");
            return;
        }

        // Stop any existing lobby music for this player
        stopLobbyMusic(player);

        // Initialize player's music index
        if (!lobbyMusicIndex.containsKey(player)) {
            lobbyMusicIndex.put(player, 0);
        }

        // Start playing the first song
        playNextLobbySong(player);
    }

    /**
     * Play the next lobby song for a player
     */
    private void playNextLobbySong(Player player) {
        if (lobbySongs.isEmpty() || !player.isOnline()) {
            return;
        }

        int index = lobbyMusicIndex.getOrDefault(player, 0);
        String songName = lobbySongs.get(index);

        // Stop all vanilla Minecraft music for this player
        stopVanillaMusic(player);

        // Play the song using custom sound with VOICE category
        // VOICE category is not affected by Music volume slider and won't conflict with vanilla music
        String soundName = "lobby." + songName.toLowerCase();
        player.playSound(player.getLocation(), soundName, SoundCategory.VOICE, 0.2f, 1.0f); // 80%音量ダウン (1.0 -> 0.2)

        plugin.getLogger().info("Playing lobby song for " + player.getName() + ": " + songName + " (sound: " + soundName + ")");

        // Get song duration (you'll need to set this based on your actual song lengths)
        // For now, using a default of 3 minutes (3600 ticks)
        long songDuration = 3600L;

        // Schedule next song after this one finishes
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Move to next song
            int nextIndex = (index + 1) % lobbySongs.size();
            lobbyMusicIndex.put(player, nextIndex);

            // Play next song
            playNextLobbySong(player);
        }, songDuration);

        lobbyMusicTasks.put(player, task);
    }

    /**
     * Stop lobby music for a player
     */
    public void stopLobbyMusic(Player player) {
        // Cancel scheduled task
        BukkitTask task = lobbyMusicTasks.remove(player);
        if (task != null) {
            task.cancel();
        }

        // Stop all sounds for the player
        player.stopAllSounds();

        // Remove index tracking
        lobbyMusicIndex.remove(player);
    }

    /**
     * Stop lobby music for all players
     */
    public void stopAllLobbyMusic() {
        for (Player player : new ArrayList<>(lobbyMusicTasks.keySet())) {
            stopLobbyMusic(player);
        }
    }

    /**
     * Start game BGM - plays to all online players
     */
    public void startGameMusic() {
        if (gameSongs.isEmpty()) {
            plugin.getLogger().warning("No game songs found!");
            return;
        }

        // Stop any existing game music
        stopGameMusic();

        currentGameSongIndex = 0;
        playNextGameSong();
    }

    /**
     * Play the next game song for all players (except those in lobby)
     */
    private void playNextGameSong() {
        if (gameSongs.isEmpty()) {
            return;
        }

        String songName = gameSongs.get(currentGameSongIndex);

        // Play for all online players who are NOT in lobby
        // Convert to lowercase for Minecraft sound system
        String soundName = "gamebgm." + songName.toLowerCase();
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip players in lobby - they should only hear lobby music
            if (isInLobby(player)) {
                continue;
            }

            // Stop vanilla Minecraft music
            stopVanillaMusic(player);

            // Play custom BGM with VOICE category (not affected by Music volume slider)
            player.playSound(player.getLocation(), soundName, SoundCategory.VOICE, 0.2f, 1.0f); // 80%音量ダウン (1.0 -> 0.2)
        }

        plugin.getLogger().info("Playing game song: " + songName + " (sound: " + soundName + ")");

        // Get song duration (default 3 minutes)
        long songDuration = 3600L;

        // Schedule next song
        gameMusicTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            currentGameSongIndex = (currentGameSongIndex + 1) % gameSongs.size();
            playNextGameSong();
        }, songDuration);
    }

    /**
     * Stop game music
     */
    public void stopGameMusic() {
        if (gameMusicTask != null) {
            gameMusicTask.cancel();
            gameMusicTask = null;
        }

        // Stop music for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.stopAllSounds();
        }
    }

    /**
     * Check if player is in lobby world
     */
    public boolean isInLobby(Player player) {
        return player.getWorld().getName().equalsIgnoreCase("lobby");
    }

    /**
     * Handle player joining - start lobby music if in lobby
     */
    public void onPlayerJoin(Player player) {
        // Start lobby music after a short delay to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isInLobby(player)) {
                startLobbyMusic(player);
            }
        }, 20L); // 1 second delay
    }

    /**
     * Handle player quitting - clean up
     */
    public void onPlayerQuit(Player player) {
        stopLobbyMusic(player);
    }

    /**
     * Stop vanilla Minecraft music for a player
     * This prevents the default Minecraft music from overlapping with custom BGM
     */
    private void stopVanillaMusic(Player player) {
        // Stop all background music using wildcard patterns
        // This is more efficient and avoids deprecated API usage
        try {
            // Stop all music.* sounds (covers all vanilla Minecraft background music)
            player.stopSound("music.game", SoundCategory.MUSIC);
            player.stopSound("music.creative", SoundCategory.MUSIC);
            player.stopSound("music.credits", SoundCategory.MUSIC);
            player.stopSound("music.end", SoundCategory.MUSIC);
            player.stopSound("music.menu", SoundCategory.MUSIC);
            player.stopSound("music.nether.crimson_forest", SoundCategory.MUSIC);
            player.stopSound("music.nether.nether_wastes", SoundCategory.MUSIC);
            player.stopSound("music.nether.soul_sand_valley", SoundCategory.MUSIC);
            player.stopSound("music.nether.basalt_deltas", SoundCategory.MUSIC);
            player.stopSound("music.nether.warped_forest", SoundCategory.MUSIC);
            player.stopSound("music.overworld.deep_dark", SoundCategory.MUSIC);
            player.stopSound("music.overworld.dripstone_caves", SoundCategory.MUSIC);
            player.stopSound("music.overworld.grove", SoundCategory.MUSIC);
            player.stopSound("music.overworld.jagged_peaks", SoundCategory.MUSIC);
            player.stopSound("music.overworld.lush_caves", SoundCategory.MUSIC);
            player.stopSound("music.overworld.swamp", SoundCategory.MUSIC);
            player.stopSound("music.overworld.jungle_and_forest", SoundCategory.MUSIC);
            player.stopSound("music.overworld.old_growth_taiga", SoundCategory.MUSIC);
            player.stopSound("music.overworld.meadow", SoundCategory.MUSIC);
            player.stopSound("music.overworld.cherry_grove", SoundCategory.MUSIC);
            player.stopSound("music.overworld.desert", SoundCategory.MUSIC);
            player.stopSound("music.overworld.badlands", SoundCategory.MUSIC);
            player.stopSound("music.overworld.snowy_slopes", SoundCategory.MUSIC);
            player.stopSound("music.overworld.frozen_peaks", SoundCategory.MUSIC);
            player.stopSound("music.overworld.stony_peaks", SoundCategory.MUSIC);

            // Stop music discs (without using deprecated Sound enum)
            player.stopSound("music_disc.13", SoundCategory.MUSIC);
            player.stopSound("music_disc.cat", SoundCategory.MUSIC);
            player.stopSound("music_disc.blocks", SoundCategory.MUSIC);
            player.stopSound("music_disc.chirp", SoundCategory.MUSIC);
            player.stopSound("music_disc.far", SoundCategory.MUSIC);
            player.stopSound("music_disc.mall", SoundCategory.MUSIC);
            player.stopSound("music_disc.mellohi", SoundCategory.MUSIC);
            player.stopSound("music_disc.stal", SoundCategory.MUSIC);
            player.stopSound("music_disc.strad", SoundCategory.MUSIC);
            player.stopSound("music_disc.ward", SoundCategory.MUSIC);
            player.stopSound("music_disc.11", SoundCategory.MUSIC);
            player.stopSound("music_disc.wait", SoundCategory.MUSIC);
            player.stopSound("music_disc.otherside", SoundCategory.MUSIC);
            player.stopSound("music_disc.5", SoundCategory.MUSIC);
            player.stopSound("music_disc.pigstep", SoundCategory.MUSIC);
            player.stopSound("music_disc.relic", SoundCategory.MUSIC);
            player.stopSound("music_disc.creator", SoundCategory.MUSIC);
            player.stopSound("music_disc.creator_music_box", SoundCategory.MUSIC);
            player.stopSound("music_disc.precipice", SoundCategory.MUSIC);
        } catch (Exception e) {
            // If any sound name is invalid, just continue
            plugin.getLogger().warning("Failed to stop some vanilla music: " + e.getMessage());
        }
    }

    /**
     * Cleanup all music tasks
     */
    public void cleanup() {
        stopAllLobbyMusic();
        stopGameMusic();
    }
}
