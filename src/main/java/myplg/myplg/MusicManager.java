package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MusicManager {
    private final PvPGame plugin;
    private final Map<UUID, BukkitTask> lobbyMusicTasks;
    private final Map<UUID, Long> lobbyMusicCooldown; // Track when lobby music can play next
    private List<String> lobbySongs;
    private List<String> gameSongs;
    private BukkitTask gameMusicTask;
    private int currentGameSongIndex = 0;
    private final Random random;

    // Game BGM duration: 24:28 = 1468 seconds = 29360 ticks
    private static final long GAME_BGM_DURATION_TICKS = 29360L;

    // Lobby BGM cooldown: 5 minutes = 300 seconds = 6000 ticks
    private static final long LOBBY_BGM_COOLDOWN_TICKS = 6000L;

    // Flag to track if game music is currently playing
    private boolean isGameMusicPlaying = false;
    private long gameMusicStartTime = 0;

    public MusicManager(PvPGame plugin) {
        this.plugin = plugin;
        this.lobbyMusicTasks = new HashMap<>();
        this.lobbyMusicCooldown = new HashMap<>();
        this.random = new Random();
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
        plugin.getLogger().info("Game BGM duration: 24:28 (29360 ticks)");
        plugin.getLogger().info("Lobby BGM cooldown: 5 minutes (6000 ticks)");
        plugin.getLogger().info("===========================");
    }

    /**
     * Start playing lobby music for a player
     * Will play one random song, then wait 5 minutes before playing another
     */
    public void startLobbyMusic(Player player) {
        if (lobbySongs.isEmpty()) {
            plugin.getLogger().warning("No lobby songs found!");
            return;
        }

        // Stop any existing lobby music for this player to prevent duplicates
        stopLobbyMusic(player);

        // Check if player is actually in lobby before starting music
        if (!isInLobby(player)) {
            plugin.getLogger().info("Player " + player.getName() + " is not in lobby, skipping music start");
            return;
        }

        // Check cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long cooldownEnd = lobbyMusicCooldown.get(playerId);

        if (cooldownEnd != null && currentTime < cooldownEnd) {
            // Still in cooldown, schedule next check
            long remainingMs = cooldownEnd - currentTime;
            long remainingTicks = remainingMs / 50; // Convert ms to ticks

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && isInLobby(player)) {
                    startLobbyMusic(player);
                }
            }, Math.max(remainingTicks, 20L));

            lobbyMusicTasks.put(playerId, task);
            return;
        }

        // Start playing a random song
        playLobbySong(player);
    }

    /**
     * Play a random lobby song for a player, then wait 5 minutes
     */
    private void playLobbySong(Player player) {
        if (lobbySongs.isEmpty() || !player.isOnline()) {
            return;
        }

        // Check if player is still in lobby
        if (!isInLobby(player)) {
            plugin.getLogger().info("Player " + player.getName() + " left lobby, stopping music");
            stopLobbyMusic(player);
            return;
        }

        UUID playerId = player.getUniqueId();

        // Select a random song
        int randomIndex = random.nextInt(lobbySongs.size());
        String songName = lobbySongs.get(randomIndex);

        // Stop any existing sounds for this player
        stopPlayerSounds(player);

        // Play the song using custom sound with VOICE category
        String soundName = "lobby." + songName.toLowerCase();
        player.playSound(player.getLocation(), soundName, SoundCategory.VOICE, 0.2f, 1.0f);

        plugin.getLogger().info("Playing lobby song for " + player.getName() + ": " + songName);

        // Set cooldown (5 minutes from now)
        lobbyMusicCooldown.put(playerId, System.currentTimeMillis() + (LOBBY_BGM_COOLDOWN_TICKS * 50));

        // Schedule next song after 5 minutes
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isInLobby(player)) {
                playLobbySong(player);
            }
        }, LOBBY_BGM_COOLDOWN_TICKS);

        lobbyMusicTasks.put(playerId, task);
    }

    /**
     * Stop lobby music for a player
     */
    public void stopLobbyMusic(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancel scheduled task
        BukkitTask task = lobbyMusicTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        // Stop lobby sounds for this player
        stopPlayerLobbySounds(player);

        // Clear cooldown
        lobbyMusicCooldown.remove(playerId);
    }

    /**
     * Stop lobby music for all players
     */
    public void stopAllLobbyMusic() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopLobbyMusic(player);
        }
        lobbyMusicTasks.clear();
        lobbyMusicCooldown.clear();
    }

    /**
     * Start game BGM - plays to all online players (except lobby)
     * Only plays once and waits for the full 24:28 duration before replaying
     */
    public void startGameMusic() {
        if (gameSongs.isEmpty()) {
            plugin.getLogger().warning("No game songs found!");
            return;
        }

        // Check if game music is already playing
        if (isGameMusicPlaying) {
            long elapsed = System.currentTimeMillis() - gameMusicStartTime;
            long durationMs = GAME_BGM_DURATION_TICKS * 50; // Convert ticks to ms

            if (elapsed < durationMs) {
                plugin.getLogger().info("Game BGM is already playing, skipping start");
                return;
            }
        }

        // Stop any existing game music task
        if (gameMusicTask != null) {
            gameMusicTask.cancel();
            gameMusicTask = null;
        }

        isGameMusicPlaying = true;
        gameMusicStartTime = System.currentTimeMillis();
        currentGameSongIndex = 0;

        playGameSong();
    }

    /**
     * Play the game song for all players (except those in lobby)
     */
    private void playGameSong() {
        if (gameSongs.isEmpty()) {
            return;
        }

        String songName = gameSongs.get(currentGameSongIndex);
        String soundName = "gamebgm." + songName.toLowerCase();

        // Play for all online players who are NOT in lobby
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip players in lobby - they should only hear lobby music
            if (isInLobby(player)) {
                continue;
            }

            // Stop any existing sounds first
            stopPlayerSounds(player);

            // Play custom BGM with VOICE category
            player.playSound(player.getLocation(), soundName, SoundCategory.VOICE, 0.2f, 1.0f);
        }

        plugin.getLogger().info("Playing game BGM: " + songName + " (duration: 24:28)");

        // Schedule next play after full duration (24:28)
        gameMusicTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Update start time for next iteration
            gameMusicStartTime = System.currentTimeMillis();
            currentGameSongIndex = (currentGameSongIndex + 1) % gameSongs.size();
            playGameSong();
        }, GAME_BGM_DURATION_TICKS);
    }

    /**
     * Stop game music
     */
    public void stopGameMusic() {
        isGameMusicPlaying = false;
        gameMusicStartTime = 0;

        if (gameMusicTask != null) {
            gameMusicTask.cancel();
            gameMusicTask = null;
        }

        // Stop game BGM sounds for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopPlayerGameSounds(player);
        }

        plugin.getLogger().info("Game BGM stopped");
    }

    /**
     * Stop only lobby BGM sounds for a player
     */
    private void stopPlayerLobbySounds(Player player) {
        for (String song : lobbySongs) {
            String soundName = "lobby." + song.toLowerCase();
            player.stopSound(soundName, SoundCategory.VOICE);
        }
    }

    /**
     * Stop only game BGM sounds for a player
     */
    private void stopPlayerGameSounds(Player player) {
        for (String song : gameSongs) {
            String soundName = "gamebgm." + song.toLowerCase();
            player.stopSound(soundName, SoundCategory.VOICE);
        }
    }

    /**
     * Stop all custom BGM sounds for a player (both lobby and game)
     */
    private void stopPlayerSounds(Player player) {
        stopPlayerLobbySounds(player);
        stopPlayerGameSounds(player);
        stopVanillaMusic(player);
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
            if (player.isOnline() && isInLobby(player)) {
                // Make sure game music is stopped for this player
                stopPlayerGameSounds(player);
                startLobbyMusic(player);
            } else if (player.isOnline() && isGameMusicPlaying) {
                // Player joined during game, play game music for them
                String songName = gameSongs.get(currentGameSongIndex);
                String soundName = "gamebgm." + songName.toLowerCase();
                player.playSound(player.getLocation(), soundName, SoundCategory.VOICE, 0.2f, 1.0f);
            }
        }, 20L); // 1 second delay
    }

    /**
     * Handle player quitting - clean up
     */
    public void onPlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();

        BukkitTask task = lobbyMusicTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        lobbyMusicCooldown.remove(playerId);
    }

    /**
     * Handle player changing worlds
     */
    public void onPlayerChangeWorld(Player player, String fromWorld, String toWorld) {
        boolean wasInLobby = fromWorld.equalsIgnoreCase("lobby");
        boolean isNowInLobby = toWorld.equalsIgnoreCase("lobby");

        if (wasInLobby && !isNowInLobby) {
            // Left lobby - stop lobby music, start game music if playing
            stopLobbyMusic(player);
            stopPlayerLobbySounds(player);

            if (isGameMusicPlaying) {
                String songName = gameSongs.get(currentGameSongIndex);
                String soundName = "gamebgm." + songName.toLowerCase();
                player.playSound(player.getLocation(), soundName, SoundCategory.VOICE, 0.2f, 1.0f);
            }
        } else if (!wasInLobby && isNowInLobby) {
            // Entered lobby - stop game music, start lobby music
            stopPlayerGameSounds(player);
            startLobbyMusic(player);
        }
    }

    /**
     * Stop vanilla Minecraft music for a player
     */
    private void stopVanillaMusic(Player player) {
        try {
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
        } catch (Exception e) {
            // Ignore errors
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
