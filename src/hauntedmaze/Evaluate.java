package hauntedmaze;

import hauntedmaze.ai.AStarBrain;
import hauntedmaze.ai.Brains;
import hauntedmaze.ai.QLearningBrain;
import hauntedmaze.ai.RandomBrain;

/**
 * Measures how good each ghost brain is, using simulated players.
 * Run with:  java -cp out hauntedmaze.Evaluate
 */
public final class Evaluate {
    private Evaluate() {
    }

    public static void main(String[] args) {
        int games = args.length > 0 ? Integer.parseInt(args[0]) : 300;

        System.out.println("=== 1. The three difficulty levels (" + games + " simulated games each) ===");
        QLearningBrain learner = new QLearningBrain();
        learner.pretrain(Brains.PRETRAIN_EPISODES, 12345L);
        learner.setLearning(false);
        learner.setEpsilon(0.05);
        System.out.println("Easy   (random ghost)  : " + Simulation.run(Difficulty.EASY, new RandomBrain(), games, 1000));
        System.out.println("Medium (A* ghost)      : " + Simulation.run(Difficulty.MEDIUM, new AStarBrain(), games, 1000));
        System.out.println("Hard   (Q-learning)    : " + Simulation.run(Difficulty.HARD, learner, games, 1000));

        System.out.println();
        System.out.println("=== 2. Which brain is best at the same speed? (Hard speed, " + games + " games) ===");
        QLearningBrain untrained = new QLearningBrain();
        untrained.setLearning(false);
        untrained.setEpsilon(0.05);
        System.out.println("Random ghost           : " + Simulation.run(Difficulty.HARD, new RandomBrain(), games, 5000));
        System.out.println("Q-learning, untrained  : " + Simulation.run(Difficulty.HARD, untrained, games, 5000));
        System.out.println("Q-learning, trained    : " + Simulation.run(Difficulty.HARD, learner, games, 5000));
        System.out.println("A* hunter              : " + Simulation.run(Difficulty.HARD, new AStarBrain(), games, 5000));

        System.out.println();
        System.out.println("=== 3. What the learning ghost learned ===");
        System.out.print(learner.describePolicy());
    }
}
