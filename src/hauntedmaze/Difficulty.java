package hauntedmaze;

/**
 * The three difficulty levels. Each level gives the ghost a different brain
 * and a different speed (the chance that the ghost moves on a given turn).
 */
public enum Difficulty {
    EASY("Easy", "Random ghost", 0.50, 1.0),
    MEDIUM("Medium", "A* hunter ghost", 0.65, 1.5),
    HARD("Hard", "Learning ghost (Q-learning)", 0.80, 2.0);

    public final String label;
    public final String brainName;
    /** Probability that the ghost gets to move after the player's move. */
    public final double ghostMoveChance;
    public final double scoreMultiplier;

    Difficulty(String label, String brainName, double ghostMoveChance, double scoreMultiplier) {
        this.label = label;
        this.brainName = brainName;
        this.ghostMoveChance = ghostMoveChance;
        this.scoreMultiplier = scoreMultiplier;
    }
}
