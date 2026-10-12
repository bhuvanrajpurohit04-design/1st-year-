package hauntedmaze;

import hauntedmaze.ai.GhostBrain;
import hauntedmaze.ai.RunnerBot;

import java.util.Locale;
import java.util.Random;

/** Runs many headless games (ghost brain vs RunnerBot) and gathers statistics. */
public final class Simulation {
    private Simulation() {
    }

    public static final class Stats {
        public final int games;
        public final int caught;
        public final int escaped;
        public final int timeouts;
        public final double avgSteps;

        Stats(int games, int caught, int escaped, int timeouts, double avgSteps) {
            this.games = games;
            this.caught = caught;
            this.escaped = escaped;
            this.timeouts = timeouts;
            this.avgSteps = avgSteps;
        }

        public double caughtRate() {
            return games == 0 ? 0 : (double) caught / games;
        }

        public double escapeRate() {
            return games == 0 ? 0 : (double) escaped / games;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "caught %5.1f%% | escaped %5.1f%% | timeouts %d | avg steps %.0f",
                    100 * caughtRate(), 100 * escapeRate(), timeouts, avgSteps);
        }
    }

    public static Stats run(Difficulty difficulty, GhostBrain brain, int games, long seed) {
        int caught = 0;
        int escaped = 0;
        int timeouts = 0;
        long totalSteps = 0;
        for (int g = 0; g < games; g++) {
            Random rng = new Random(seed + g);
            GameState gs = GameState.newGame(difficulty, brain, rng);
            int turns = 0;
            while (gs.status() == GameState.Status.PLAYING && turns < 400) {
                int[] move = RunnerBot.chooseMove(gs, rng);
                if (move != null) gs.movePlayer(move[0], move[1]);
                if (gs.isTrickOrTreatPending()) gs.resolveTrickOrTreat();
                turns++;
            }
            if (gs.status() == GameState.Status.CAUGHT) caught++;
            else if (gs.status() == GameState.Status.WON) escaped++;
            else {
                timeouts++;
                gs.abandon();
            }
            totalSteps += gs.steps();
        }
        return new Stats(games, caught, escaped, timeouts, games == 0 ? 0 : (double) totalSteps / games);
    }
}
