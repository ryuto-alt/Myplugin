package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {
    private final PvPGame plugin;
    private final Map<String, Team> teams;
    private final Map<UUID, String> playerTeams;
    private boolean gameRunning;
    private myplg.myplg.GameMode currentGameMode;

    public GameManager(PvPGame plugin) {
        this.plugin = plugin;
        this.teams = new HashMap<>();
        this.playerTeams = new HashMap<>();
        this.gameRunning = false;
        this.currentGameMode = myplg.myplg.GameMode.SOLO; // Default mode
    }

    public void addTeam(Team team) {
        addTeam(team, true);
    }

    public void addTeam(Team team, boolean save) {
        teams.put(team.getName(), team);
        if (save) {
            plugin.getTeamDataManager().saveTeams();
        }
    }

    public Team getTeam(String name) {
        return teams.get(name);
    }

    public Map<String, Team> getTeams() {
        return teams;
    }

    public String getPlayerTeam(UUID playerUUID) {
        return playerTeams.get(playerUUID);
    }

    public void setPlayerTeam(UUID playerUUID, String teamName) {
        playerTeams.put(playerUUID, teamName);
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void setGameRunning(boolean running) {
        this.gameRunning = running;
    }

    public myplg.myplg.GameMode getGameMode() {
        return currentGameMode;
    }

    public void setGameMode(myplg.myplg.GameMode mode) {
        this.currentGameMode = mode;
        plugin.getLogger().info("Game mode set to: " + mode.getDisplayName());
    }

    public void assignPlayersToTeams(List<Player> players) {
        if (teams.isEmpty()) {
            plugin.getLogger().warning("No teams available for assignment!");
            return;
        }

        // Clear existing team assignments
        for (Team team : teams.values()) {
            team.getMembers().clear();
        }
        playerTeams.clear();

        // Shuffle players for random assignment
        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        // Get teams as list
        List<Team> teamList = new ArrayList<>(teams.values());

        plugin.getLogger().info("Assigning " + shuffledPlayers.size() + " players to " + teamList.size() + " teams in " + currentGameMode.getDisplayName() + " mode");

        int maxPlayersPerTeam = currentGameMode.getMaxPlayersPerTeam();
        int teamIndex = 0;
        int teamPlayerCount = 0;

        for (Player player : shuffledPlayers) {
            // Skip to next team if current team is full
            while (teamPlayerCount >= maxPlayersPerTeam && teamIndex < teamList.size() - 1) {
                teamIndex++;
                teamPlayerCount = 0;
            }

            // If we've filled all teams, stop assignment
            if (teamIndex >= teamList.size()) {
                plugin.getLogger().warning("Not enough teams for all players! Player " + player.getName() + " not assigned.");
                player.sendMessage("§c全てのチームが満員です！観戦モードになります。");
                continue;
            }

            Team team = teamList.get(teamIndex);
            team.addMember(player.getUniqueId());
            playerTeams.put(player.getUniqueId(), team.getName());
            teamPlayerCount++;

            plugin.getLogger().info("Assigned " + player.getName() + " to team " + team.getName() + " (Player " + teamPlayerCount + "/" + maxPlayersPerTeam + ")");
            player.sendMessage("§aあなたは §e" + team.getName() + " §aチームに配属されました！");

            // Move to next team if current team is full
            if (teamPlayerCount >= maxPlayersPerTeam) {
                teamIndex++;
                teamPlayerCount = 0;
            }
        }

        // Log final team assignments
        for (Team team : teamList) {
            plugin.getLogger().info("Team " + team.getName() + ": " + team.getMembers().size() + " players");
        }
    }

    public void teleportPlayerToTeamSpawn(Player player) {
        String teamName = getPlayerTeam(player.getUniqueId());
        if (teamName != null) {
            Team team = getTeam(teamName);
            if (team != null) {
                player.teleport(team.getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    public void renameTeam(String oldName, String newName) {
        Team team = teams.get(oldName);
        if (team == null) {
            return;
        }

        // Remove old entry and add with new name
        teams.remove(oldName);

        // Create new team with updated name but same data
        Team newTeam = new Team(newName, team.getBedBlock(), team.getSpawnLocation());
        for (UUID memberUUID : team.getMembers()) {
            newTeam.addMember(memberUUID);
        }

        teams.put(newName, newTeam);

        // Update player team mappings
        for (UUID memberUUID : newTeam.getMembers()) {
            playerTeams.put(memberUUID, newName);
        }

        // Save changes
        plugin.getTeamDataManager().saveTeams();
    }

    public void reset() {
        teams.clear();
        playerTeams.clear();
        gameRunning = false;
    }
}
