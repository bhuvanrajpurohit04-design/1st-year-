package hauntedmaze;

import hauntedmaze.ai.Brains;
import hauntedmaze.ai.QLearningBrain;

import java.io.IOException;

/**
 * Trains the learning ghost from scratch and saves its memory file, so that
 * the very first Hard game starts instantly.
 * Run with:  java -cp out hauntedmaze.TrainGhost
 */
public final class TrainGhost {
    private TrainGhost() {
    }

    public static void main(String[] args) throws IOException {
        QLearningBrain ghost = new QLearningBrain();
        System.out.println("Training the ghost for " + Brains.PRETRAIN_EPISODES + " practice games...");
        ghost.pretrain(Brains.PRETRAIN_EPISODES, 12345L);
        ghost.save(Brains.MEMORY_FILE);
        System.out.println("Saved the ghost's memory to " + Brains.MEMORY_FILE.toAbsolutePath());
    }
}
