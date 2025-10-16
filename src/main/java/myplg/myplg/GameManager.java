package myplg.myplg;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {
    private final PvPGame plugin;
    private final Map<String, Team> teams;
    private final Map<UUID, String> playerTeams;
    private boolean gameRunning;

    public GameManager(PvPGame plugin) {
        this.plugin = plugin;
        this.teams = new HashMap<>();
        this.playerTeams = new HashMap<>();
        this.gameRunning = false;
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

    public void assignPlayersToTeams(List<Player> players) {
        if (teams.isEmpty()) {
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

        // Get teams as list for round-robin assignment
        List<Team> teamList = new ArrayList<>(teams.values());

        // Assign players to teams in round-robin fashion
        int teamIndex = 0;
        for (Player player : shuffledPlayers) {
            Team team = teamList.get(teamIndex);
            team.addMember(player.getUniqueId());
            playerTeams.put(player.getUniqueId(), team.getName());

            teamIndex = (teamIndex + 1) % teamList.size();
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
