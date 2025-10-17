package myplg.myplg;

public enum GameMode {
    SOLO("ソロ", 1),  // 1 player per team
    DUEL("デュオ", 2); // 2 players per team

    private final String displayName;
    private final int maxPlayersPerTeam;

    GameMode(String displayName, int maxPlayersPerTeam) {
        this.displayName = displayName;
        this.maxPlayersPerTeam = maxPlayersPerTeam;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxPlayersPerTeam() {
        return maxPlayersPerTeam;
    }
}
