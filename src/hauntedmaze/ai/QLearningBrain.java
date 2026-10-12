package hauntedmaze.ai;

import hauntedmaze.Difficulty;
import hauntedmaze.GameState;
import hauntedmaze.Pos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Hard ghost: a tabular Q-learning agent that learns WHICH HUNTING STRATEGY
 * works best in each situation.
 *
 * Actions (strategies):
 *   CHASE  - walk the shortest path to the player
 *   AMBUSH - walk to the tile ahead of the player (where they are heading)
 *   GUARD  - go to what the player wants: the exit, or the nearest candy
 *   PATROL - wander randomly
 *
 * State (24 combinations):
 *   how far the player is (4 bins) x is the exit open (2) x is the player
 *   getting closer / further away / unchanged since the ghost's last move (3)
 *
 * Learning rule (Q-learning):
 *   Q(s,a) <- Q(s,a) + alpha * ( reward + gamma * max Q(s',a') - Q(s,a) )
 *
 * Rewards: -1 for every step, +2 more when the ghost got closer, +50 when it
 * catches the player, -50 when the player escapes. The table is saved to a
 * file, so the ghost remembers what it learned between games.
 */
public final class QLearningBrain implements GhostBrain {
    public static final int CHASE = 0;
    public static final int AMBUSH = 1;
    public static final int GUARD = 2;
    public static final int PATROL = 3;
    public static final int ACTIONS = 4;
    public static final int STATES = 4 * 2 * 3;
    public static final String[] ACTION_NAMES = {"CHASE", "AMBUSH", "GUARD", "PATROL"};

    private final double[][] q = new double[STATES][ACTIONS];
    private double alpha = 0.15;
    private double gamma = 0.9;
    private double epsilon = 0.05; // chance of trying a random strategy
    private boolean learning = true;

    private int lastState = -1;
    private int lastAction = -1;
    private int lastDist = -1;

    @Override
    public String name() {
        return "Q-learning";
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public void setLearning(boolean learning) {
        this.learning = learning;
    }

    public double qValue(int state, int action) {
        return q[state][action];
    }

    // ----------------------------------------------------------- decisions

    @Override
    public Pos nextMove(GameState state, Random rng) {
        Pos ghost = state.ghost();
        Pos player = state.player();
        int dist = state.maze.distancesFrom(ghost)[player.r][player.c];
        int s = encode(dist, state.exitOpen(), lastDist);

        if (lastState >= 0) {
            double reward = -1.0 + (lastDist >= 0 && dist < lastDist ? 2.0 : 0.0);
            learn(lastState, lastAction, reward, s, false);
        }
        int action = chooseAction(s, rng);
        lastState = s;
        lastAction = action;
        lastDist = dist;
        return stepFor(action, state, rng);
    }

    @Override
    public void onGameEnd(GameState state, boolean ghostCaughtPlayer) {
        if (lastState >= 0) {
            learn(lastState, lastAction, ghostCaughtPlayer ? 50.0 : -50.0, 0, true);
        }
        lastState = -1;
        lastAction = -1;
        lastDist = -1;
    }

    /** Turns the situation into one of the 24 state numbers. */
    public static int encode(int dist, boolean exitOpen, int previousDist) {
        int distBin = dist <= 2 ? 0 : dist <= 5 ? 1 : dist <= 10 ? 2 : 3;
        int trend = previousDist < 0 ? 2 : dist < previousDist ? 0 : dist > previousDist ? 1 : 2;
        return (distBin * 2 + (exitOpen ? 1 : 0)) * 3 + trend;
    }

    /** Epsilon-greedy: usually the best known strategy, sometimes a random one. */
    public int chooseAction(int s, Random rng) {
        if (rng.nextDouble() < epsilon) {
            return rng.nextInt(ACTIONS);
        }
        double best = q[s][0];
        for (int a = 1; a < ACTIONS; a++) best = Math.max(best, q[s][a]);
        int[] ties = new int[ACTIONS];
        int n = 0;
        for (int a = 0; a < ACTIONS; a++) {
            if (q[s][a] >= best - 1e-9) ties[n++] = a;
        }
        return ties[rng.nextInt(n)];
    }

    /** The Q-learning update. Public so it can be unit-tested. */
    public void learn(int s, int a, double reward, int sNext, boolean terminal) {
        if (!learning) return;
        double bestNext = 0.0;
        if (!terminal) {
            bestNext = q[sNext][0];
            for (int i = 1; i < ACTIONS; i++) bestNext = Math.max(bestNext, q[sNext][i]);
        }
        q[s][a] += alpha * (reward + gamma * bestNext - q[s][a]);
    }

    // --------------------------------------------------------- strategies

    private Pos stepFor(int action, GameState state, Random rng) {
        if (action == PATROL) return randomNeighbor(state, rng);
        Pos target = state.player();
        if (action == AMBUSH) target = ambushTile(state);
        if (action == GUARD) target = guardTile(state);

        List<Pos> path = AStar.findPath(state.maze, state.ghost(), target);
        if (path.size() < 2) {
            // Already at the target tile: fall back to chasing the player.
            path = AStar.findPath(state.maze, state.ghost(), state.player());
        }
        return path.size() >= 2 ? path.get(1) : randomNeighbor(state, rng);
    }

    /** Up to 4 tiles ahead of the player in the direction they last moved. */
    private Pos ambushTile(GameState state) {
        int[] dir = state.lastDirection();
        Pos cur = state.player();
        if (dir == null) return cur;
        for (int i = 0; i < 4; i++) {
            Pos next = cur.plus(dir[0], dir[1]);
            if (!state.maze.isOpen(next)) break;
            cur = next;
        }
        return cur;
    }

    /** The exit if it is open, otherwise the candy closest to the player. */
    private Pos guardTile(GameState state) {
        if (state.exitOpen()) return state.exit();
        Pos best = state.player();
        int bestDist = Integer.MAX_VALUE;
        for (Pos candy : state.candies()) {
            int d = candy.manhattan(state.player());
            if (d < bestDist) {
                bestDist = d;
                best = candy;
            }
        }
        return best;
    }

    private Pos randomNeighbor(GameState state, Random rng) {
        List<Pos> options = state.maze.neighbors(state.ghost());
        return options.isEmpty() ? state.ghost() : options.get(rng.nextInt(options.size()));
    }

    // ------------------------------------------------------------ training

    /**
     * Practises against a simulated player (RunnerBot) on many random mazes,
     * so the ghost is already clever in its very first real game.
     */
    public void pretrain(int episodes, long seed) {
        boolean oldLearning = learning;
        double oldEpsilon = epsilon;
        learning = true;
        for (int e = 0; e < episodes; e++) {
            // Start bold and curious, finish careful: both the learning rate
            // (alpha) and the exploration rate (epsilon) shrink as practice goes on.
            double progress = (double) e / Math.max(1, episodes);
            alpha = 0.25 - 0.20 * progress;
            epsilon = 0.30 - 0.25 * progress;
            Random rng = new Random(seed + e);
            GameState gs = GameState.newGame(Difficulty.HARD, this, rng);
            int turns = 0;
            while (gs.status() == GameState.Status.PLAYING && turns < 300) {
                int[] move = RunnerBot.chooseMove(gs, rng);
                if (move != null) gs.movePlayer(move[0], move[1]);
                if (gs.isTrickOrTreatPending()) gs.resolveTrickOrTreat();
                turns++;
            }
            gs.abandon();
        }
        alpha = 0.10; // while playing real games it keeps learning, but gently
        learning = oldLearning;
        epsilon = oldEpsilon;
    }

    // --------------------------------------------------------- persistence

    public void save(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("QTABLE ").append(STATES).append(' ').append(ACTIONS).append('\n');
        for (int s = 0; s < STATES; s++) {
            for (int a = 0; a < ACTIONS; a++) {
                if (a > 0) sb.append(' ');
                sb.append(String.format(Locale.ROOT, "%.6f", q[s][a]));
            }
            sb.append('\n');
        }
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Returns true if a valid saved table was loaded. */
    public boolean load(Path file) {
        try {
            if (!Files.exists(file)) return false;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.size() != STATES + 1 || !lines.get(0).equals("QTABLE " + STATES + " " + ACTIONS)) {
                return false;
            }
            double[][] loaded = new double[STATES][ACTIONS];
            for (int s = 0; s < STATES; s++) {
                String[] parts = lines.get(s + 1).trim().split("\\s+");
                if (parts.length != ACTIONS) return false;
                for (int a = 0; a < ACTIONS; a++) loaded[s][a] = Double.parseDouble(parts[a]);
            }
            for (int s = 0; s < STATES; s++) System.arraycopy(loaded[s], 0, q[s], 0, ACTIONS);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** A readable summary of what the ghost has learned (best strategy per state). */
    public String describePolicy() {
        StringBuilder sb = new StringBuilder();
        String[] dist = {"very close", "close", "medium", "far"};
        for (int d = 0; d < 4; d++) {
            for (int ex = 0; ex < 2; ex++) {
                for (int t = 0; t < 3; t++) {
                    int s = (d * 2 + ex) * 3 + t;
                    int best = 0;
                    for (int a = 1; a < ACTIONS; a++) if (q[s][a] > q[s][best]) best = a;
                    sb.append(String.format("player %-10s exit %-6s trend %-9s -> %s%n",
                            dist[d], ex == 1 ? "open" : "closed",
                            t == 0 ? "closing" : t == 1 ? "escaping" : "steady", ACTION_NAMES[best]));
                }
            }
        }
        return sb.toString();
    }
}
