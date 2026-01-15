package myplg.myplg;

public enum GameMode {
    SOLO("1vs1", 1),      // 1 player per team
    DUO("2vs2", 2),       // 2 players per team
    TRIPLE("3vs3", 3),    // 3 players per team
    CUSTOM("カスタム", -1), // Custom mode (no limit)
    DEBUG("デバッグ", 1);   // Debug mode (1 player, free shops)

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
