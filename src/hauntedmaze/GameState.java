package hauntedmaze;

import hauntedmaze.ai.GhostBrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * All the rules of the game, with no graphics. The game is turn based:
 * every time the player makes a move, the ghost may make a move.
 * Keeping the rules separate from Swing means they can be unit-tested and
 * simulated thousands of times in a few seconds.
 */
public final class GameState {
    public static final int MAZE_ROWS = 21;
    public static final int MAZE_COLS = 21;
    public static final int CANDY_COUNT = 7;
    public static final int PUMPKIN_COUNT = 3;

    public enum Status { PLAYING, WON, CAUGHT }

    /** Outcome of knocking on a trick-or-treat door. */
    public static final class TotResult {
        public final boolean treat;
        public final String message;

        TotResult(boolean treat, String message) {
            this.treat = treat;
            this.message = message;
        }
    }

    public final Maze maze;
    public final Difficulty difficulty;
    public final GhostBrain brain;

    private final Random rng;
    private final Pos exit;
    private final Set<Pos> candies = new HashSet<>();
    private final Set<Pos> pumpkins = new HashSet<>();
    private Pos player;
    private Pos ghost;
    private int[] lastDir = null;
    private int collected = 0;
    private int steps = 0;
    private int ghostFrozen = 0;
    private int ghostBoost = 0;
    private int bonusPoints = 0;
    private boolean totPending = false;
    private Status status = Status.PLAYING;
    private String message = "Collect all the candy, then run to the exit door!";

    private GameState(Maze maze, Difficulty difficulty, GhostBrain brain, Random rng,
                      Pos player, Pos ghost, Pos exit) {
        this.maze = maze;
        this.difficulty = difficulty;
        this.brain = brain;
        this.rng = rng;
        this.player = player;
        this.ghost = ghost;
        this.exit = exit;
    }

    /** Builds a fresh random level. */
    public static GameState newGame(Difficulty difficulty, GhostBrain brain, Random rng) {
        Maze maze = Maze.generate(MAZE_ROWS, MAZE_COLS, rng);
        Pos start = new Pos(1, 1);
        int[][] fromStart = maze.distancesFrom(start);
        List<Pos> cells = maze.openCells();

        // The exit is the tile that is furthest from the player.
        Pos exit = start;
        int maxDist = 0;
        for (Pos p : cells) {
            if (fromStart[p.r][p.c] > maxDist) {
                maxDist = fromStart[p.r][p.c];
                exit = p;
            }
        }
        int[][] fromExit = maze.distancesFrom(exit);

        // The ghost starts roughly half way between the player and the exit.
        List<Pos> ghostSpots = new ArrayList<>();
        for (Pos p : cells) {
            int d = fromStart[p.r][p.c];
            if (d >= maxDist * 0.4 && d <= maxDist * 0.7 && fromExit[p.r][p.c] >= 6) {
                ghostSpots.add(p);
            }
        }
        if (ghostSpots.isEmpty()) {
            for (Pos p : cells) {
                if (fromStart[p.r][p.c] >= 8 && !p.equals(exit)) ghostSpots.add(p);
            }
        }
        Pos ghost = ghostSpots.get(rng.nextInt(ghostSpots.size()));

        GameState gs = new GameState(maze, difficulty, brain, rng, start, ghost, exit);

        // Candy and pumpkins go on random tiles, away from the start/ghost/exit.
        List<Pos> free = new ArrayList<>();
        for (Pos p : cells) {
            if (fromStart[p.r][p.c] >= 4 && !p.equals(exit) && !p.equals(ghost)) free.add(p);
        }
        Collections.shuffle(free, rng);
        for (int i = 0; i < CANDY_COUNT && i < free.size(); i++) {
            gs.candies.add(free.get(i));
        }
        for (int i = CANDY_COUNT; i < CANDY_COUNT + PUMPKIN_COUNT && i < free.size(); i++) {
            gs.pumpkins.add(free.get(i));
        }
        return gs;
    }

    // ---------------------------------------------------------------- moves

    /**
     * Moves the player one tile. Returns false (and uses no turn) if the way
     * is blocked by a wall or the game is already over.
     */
    public boolean movePlayer(int dr, int dc) {
        if (status != Status.PLAYING) return false;
        Pos next = player.plus(dr, dc);
        if (!maze.isOpen(next)) return false;

        player = next;
        lastDir = new int[]{dr, dc};
        steps++;
        message = "";

        if (candies.remove(next)) {
            collected++;
            message = candies.isEmpty() ? "All candy collected! The exit door is open!" : "Yum! Candy collected.";
        }
        if (pumpkins.remove(next)) {
            totPending = true;
            message = "A cursed pumpkin grins at you...";
        }
        if (next.equals(ghost)) {
            end(Status.CAUGHT);
            return true;
        }
        if (next.equals(exit)) {
            if (exitOpen()) {
                end(Status.WON);
                return true;
            }
            message = "The door is locked. Collect all the candy first!";
        }
        ghostTurn();
        return true;
    }

    private void ghostTurn() {
        if (ghostFrozen > 0) {
            ghostFrozen--;
            return;
        }
        int moves = rng.nextDouble() < difficulty.ghostMoveChance ? 1 : 0;
        if (ghostBoost > 0) {
            moves = 2;
            ghostBoost--;
        }
        for (int i = 0; i < moves && status == Status.PLAYING; i++) {
            Pos target = brain.nextMove(this, rng);
            if (target != null && maze.isOpen(target) && target.manhattan(ghost) <= 1) {
                ghost = target;
            }
            if (ghost.equals(player)) {
                end(Status.CAUGHT);
            }
        }
    }

    private void end(Status result) {
        status = result;
        message = result == Status.WON ? "You escaped Blackwood Maze!" : "The ghost got you!";
        brain.onGameEnd(this, result == Status.CAUGHT);
    }

    /**
     * Used only by simulations that run out of time: the player was never
     * caught, so a learning ghost is told it failed.
     */
    public void abandon() {
        if (status == Status.PLAYING) {
            brain.onGameEnd(this, false);
        }
    }

    // -------------------------------------------------------- trick or treat

    public boolean isTrickOrTreatPending() {
        return totPending;
    }

    /** Called after the player knocks on a door. 60% treat, 40% trick. */
    public TotResult resolveTrickOrTreat() {
        totPending = false;
        if (rng.nextDouble() < 0.6) {
            if (rng.nextBoolean()) {
                ghostFrozen += 4;
                message = "TREAT! The ghost is frozen for 4 turns.";
                return new TotResult(true, message);
            }
            bonusPoints += 25;
            message = "TREAT! A bag of candy: +25 points.";
            return new TotResult(true, message);
        }
        ghostBoost += 4;
        message = "TRICK! The ghost gets a burst of speed for 4 turns!";
        return new TotResult(false, message);
    }

    // ------------------------------------------------------------- read-only

    public boolean exitOpen() {
        return candies.isEmpty();
    }

    public Status status() { return status; }
    public Pos player() { return player; }
    public Pos ghost() { return ghost; }
    public Pos exit() { return exit; }
    public int steps() { return steps; }
    public int candiesCollected() { return collected; }
    public int candiesTotal() { return collected + candies.size(); }
    public int ghostFrozenTurns() { return ghostFrozen; }
    public int ghostBoostTurns() { return ghostBoost; }
    public String message() { return message; }
    public int[] lastDirection() { return lastDir == null ? null : lastDir.clone(); }
    public Set<Pos> candies() { return Collections.unmodifiableSet(candies); }
    public Set<Pos> pumpkins() { return Collections.unmodifiableSet(pumpkins); }

    /**
     * Score: 10 per candy + treat bonuses, plus (if you escaped) 100 and a
     * speed bonus of up to 300 for using few steps, times the difficulty.
     */
    public int score() {
        int base = collected * 10 + bonusPoints;
        if (status == Status.WON) {
            base += 100 + Math.max(0, 300 - steps);
        }
        return (int) Math.round(base * difficulty.scoreMultiplier);
    }
}
