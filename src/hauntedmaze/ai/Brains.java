package hauntedmaze.ai;

import hauntedmaze.Difficulty;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Creates the right ghost brain for each difficulty level. */
public final class Brains {
    /** Where the learning ghost keeps its memory between games. */
    public static final Path MEMORY_FILE = Paths.get("ghost_memory.txt");
    public static final int PRETRAIN_EPISODES = 1000;

    private static QLearningBrain learner;

    private Brains() {
    }

    public static GhostBrain forDifficulty(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return new RandomBrain();
            case MEDIUM:
                return new AStarBrain();
            default:
                return learningGhost();
        }
    }

    /**
     * The single learning ghost, shared by every Hard game so it keeps
     * improving. First use: load its memory file, or practise for a moment.
     */
    public static synchronized QLearningBrain learningGhost() {
        if (learner == null) {
            learner = new QLearningBrain();
            if (!learner.load(MEMORY_FILE)) {
                learner.pretrain(PRETRAIN_EPISODES, 12345L);
                saveLearner();
            }
        }
        return learner;
    }

    public static synchronized void saveLearner() {
        if (learner == null) return;
        try {
            learner.save(MEMORY_FILE);
        } catch (IOException ignored) {
            // Not being able to save is not fatal: the ghost just forgets next time.
        }
    }
}
