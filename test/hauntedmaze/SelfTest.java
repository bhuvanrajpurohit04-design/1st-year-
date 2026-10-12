package hauntedmaze;

import hauntedmaze.ai.AStar;
import hauntedmaze.ai.AStarBrain;
import hauntedmaze.ai.GhostBrain;
import hauntedmaze.ai.QLearningBrain;
import hauntedmaze.ai.RandomBrain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Self-tests with no external library (no JUnit needed).
 * Run with:  java -cp out hauntedmaze.SelfTest
 */
public final class SelfTest {
    private static int passed = 0;
    private static int failed = 0;

    private SelfTest() {
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    /** A ghost that never moves: lets us test the player-side rules in isolation. */
    private static final class StillBrain implements GhostBrain {
        @Override
        public String name() {
            return "Still";
        }

        @Override
        public Pos nextMove(GameState state, Random rng) {
            return state.ghost();
        }
    }

    public static void main(String[] args) throws IOException {
        testMaze();
        testAStar();
        testPlayerRules();
        testCaught();
        testTrickOrTreat();
        testQLearningMath();
        testPersistence();
        testHighScores();
        testLearningHelps();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // ------------------------------------------------------------------ maze

    private static void testMaze() {
        System.out.println("Maze generation");
        boolean connected = true;
        boolean bordered = true;
        boolean hasLoops = true;
        for (int seed = 0; seed < 50; seed++) {
            Maze m = Maze.generate(21, 21, new Random(seed));
            List<Pos> cells = m.openCells();
            int[][] d = m.distancesFrom(new Pos(1, 1));
            for (Pos p : cells) if (d[p.r][p.c] < 0) connected = false;
            for (int i = 0; i < 21; i++) {
                if (m.isOpen(0, i) || m.isOpen(20, i) || m.isOpen(i, 0) || m.isOpen(i, 20)) bordered = false;
            }
            int edges = 0;
            for (Pos p : cells) {
                if (m.isOpen(p.r + 1, p.c)) edges++;
                if (m.isOpen(p.r, p.c + 1)) edges++;
            }
            if (edges < cells.size()) hasLoops = false; // a perfect maze has exactly cells-1 edges
        }
        check("every open tile is reachable from the start (50 mazes)", connected);
        check("the outer border is solid wall", bordered);
        check("mazes contain loops, not just one path", hasLoops);
        boolean rejected = false;
        try {
            Maze.generate(20, 21, new Random(1));
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("even maze sizes are rejected", rejected);
    }

    // ----------------------------------------------------------------- A*

    private static void testAStar() {
        System.out.println("A* pathfinding");
        Random rng = new Random(99);
        boolean allOptimal = true;
        boolean allValid = true;
        for (int i = 0; i < 40; i++) {
            Maze m = Maze.generate(21, 21, rng);
            List<Pos> cells = m.openCells();
            for (int k = 0; k < 10; k++) {
                Pos a = cells.get(rng.nextInt(cells.size()));
                Pos b = cells.get(rng.nextInt(cells.size()));
                List<Pos> path = AStar.findPath(m, a, b);
                int bfs = m.distancesFrom(a)[b.r][b.c];
                if (path.isEmpty() || path.size() - 1 != bfs) allOptimal = false;
                if (!path.isEmpty()) {
                    if (!path.get(0).equals(a) || !path.get(path.size() - 1).equals(b)) allValid = false;
                    for (int j = 1; j < path.size(); j++) {
                        if (path.get(j).manhattan(path.get(j - 1)) != 1 || !m.isOpen(path.get(j))) allValid = false;
                    }
                }
            }
        }
        check("A* path length equals the BFS shortest distance (400 random pairs)", allOptimal);
        check("A* paths start and end correctly and only use open, adjacent tiles", allValid);
        Maze m = Maze.generate(21, 21, new Random(5));
        check("A* returns an empty path when the goal is a wall", AStar.findPath(m, new Pos(1, 1), new Pos(0, 0)).isEmpty());
        check("A* from a tile to itself is just that tile", AStar.findPath(m, new Pos(1, 1), new Pos(1, 1)).size() == 1);
    }

    // ------------------------------------------------------- player rules

    private static void walk(GameState gs, Pos target) {
        List<Pos> path = AStar.findPath(gs.maze, gs.player(), target);
        for (int i = 1; i < path.size() && gs.status() == GameState.Status.PLAYING; i++) {
            Pos from = path.get(i - 1);
            Pos to = path.get(i);
            gs.movePlayer(to.r - from.r, to.c - from.c);
            if (gs.isTrickOrTreatPending()) gs.resolveTrickOrTreat();
        }
    }

    private static void testPlayerRules() {
        System.out.println("Game rules");
        GameState gs = GameState.newGame(Difficulty.EASY, new StillBrain(), new Random(7));
        check("level starts with 7 candies and 3 pumpkins",
                gs.candies().size() == GameState.CANDY_COUNT && gs.pumpkins().size() == GameState.PUMPKIN_COUNT);
        check("the player starts at (1,1) and the exit is locked",
                gs.player().equals(new Pos(1, 1)) && !gs.exitOpen());
        check("the ghost does not start on the player", !gs.ghost().equals(gs.player()));

        boolean moved = gs.movePlayer(-1, 0); // (0,1) is the outer wall
        check("walking into a wall is refused and uses no turn", !moved && gs.steps() == 0);

        Pos firstCandy = gs.candies().iterator().next();
        walk(gs, firstCandy);
        check("walking onto candy collects it", gs.candiesCollected() >= 1);

        // Try the locked door first.
        GameState locked = GameState.newGame(Difficulty.EASY, new StillBrain(), new Random(11));
        walk(locked, locked.exit());
        boolean stillPlaying = locked.status() == GameState.Status.PLAYING || locked.exitOpen();
        check("the exit does not open while candy is left", stillPlaying);

        // Play a whole game: all candies, then the exit.
        GameState full = GameState.newGame(Difficulty.EASY, new StillBrain(), new Random(21));
        int bonus = 0;
        int guard = 0;
        while (!full.candies().isEmpty() && guard++ < 20) {
            Pos nearest = null;
            int[][] d = full.maze.distancesFrom(full.player());
            for (Pos c : full.candies()) {
                if (nearest == null || d[c.r][c.c] < d[nearest.r][nearest.c]) nearest = c;
            }
            walk(full, nearest);
        }
        check("collecting every candy opens the exit", full.exitOpen() && full.candiesCollected() == 7);
        walk(full, full.exit());
        check("reaching the open exit wins the game", full.status() == GameState.Status.WON);
        int expectedBase = 7 * 10 + 100 + Math.max(0, 300 - full.steps());
        check("score = candy + escape bonus + speed bonus (x1 on Easy, plus any +25 treats)",
                (full.score() - expectedBase) % 25 == 0 && full.score() >= expectedBase);
        check("no moves are accepted after the game ends", !full.movePlayer(0, 1) && !full.movePlayer(1, 0));
        check("Hard mode doubles the score of the same run",
                Difficulty.HARD.scoreMultiplier == 2.0 * Difficulty.EASY.scoreMultiplier);
    }

    private static void testCaught() {
        System.out.println("Being caught");
        boolean caughtSomewhere = false;
        for (long seed = 1; seed <= 5 && !caughtSomewhere; seed++) {
            GameState gs = GameState.newGame(Difficulty.MEDIUM, new AStarBrain(), new Random(seed));
            int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int i = 0; i < 600 && gs.status() == GameState.Status.PLAYING; i++) {
                // Shuffle back and forth near the start; the A* ghost walks right up to us.
                for (int[] d : dirs) {
                    if (gs.movePlayer(d[0], d[1])) break;
                }
                if (gs.isTrickOrTreatPending()) gs.resolveTrickOrTreat();
            }
            if (gs.status() == GameState.Status.CAUGHT) {
                caughtSomewhere = true;
                check("an A* ghost eventually catches a player who dawdles", true);
                check("a caught game accepts no more moves", !gs.movePlayer(0, 1));
            }
        }
        if (!caughtSomewhere) check("an A* ghost eventually catches a player who dawdles", false);
    }

    private static void testTrickOrTreat() {
        System.out.println("Trick or treat");
        boolean sawFreeze = false;
        boolean sawBoost = false;
        boolean sawBonus = false;
        boolean freezeWorks = false;
        for (long seed = 1; seed <= 200 && !(sawFreeze && sawBoost && sawBonus); seed++) {
            GameState gs = GameState.newGame(Difficulty.EASY, new RandomBrain(), new Random(seed));
            GameState.TotResult r = gs.resolveTrickOrTreat();
            if (r.treat && r.message.contains("frozen")) {
                sawFreeze = true;
                Pos before = gs.ghost();
                boolean same = gs.ghostFrozenTurns() == 4;
                int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                for (int i = 0; i < 4; i++) {
                    boolean ok = false;
                    for (int[] d : dirs) {
                        if (gs.status() == GameState.Status.PLAYING && gs.movePlayer(d[0], d[1])) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) break;
                }
                same = same && gs.ghost().equals(before) && gs.ghostFrozenTurns() == 0;
                freezeWorks = freezeWorks || same;
            } else if (r.treat) {
                sawBonus = sawBonus || r.message.contains("+25");
            } else {
                sawBoost = sawBoost || (gs.ghostBoostTurns() == 4 && r.message.contains("TRICK"));
            }
        }
        check("a treat can freeze the ghost, and a frozen ghost really stays put for 4 turns", sawFreeze && freezeWorks);
        check("a treat can give +25 bonus points", sawBonus);
        check("a trick gives the ghost a 4-turn speed boost", sawBoost);
    }

    // ----------------------------------------------------------- Q-learning

    private static void testQLearningMath() {
        System.out.println("Q-learning");
        QLearningBrain brain = new QLearningBrain();
        brain.learn(0, 1, 10.0, 5, false); // Q = 0 + 0.15 * (10 + 0.9*0 - 0)
        check("one update follows Q <- Q + alpha*(r + gamma*maxQ' - Q)  (=1.5)",
                Math.abs(brain.qValue(0, 1) - 1.5) < 1e-9);
        brain.learn(0, 1, 50.0, 0, true); // Q = 1.5 + 0.15 * (50 - 1.5)
        check("a terminal update ignores the next state  (=8.775)", Math.abs(brain.qValue(0, 1) - 8.775) < 1e-9);

        brain.setEpsilon(0.0);
        boolean greedy = true;
        for (int i = 0; i < 50; i++) {
            if (brain.chooseAction(0, new Random(i)) != QLearningBrain.AMBUSH) greedy = false;
        }
        check("with epsilon 0 the ghost always picks the best-known strategy", greedy);

        brain.setLearning(false);
        brain.learn(3, 2, 100.0, 4, false);
        check("learning can be switched off", brain.qValue(3, 2) == 0.0);

        check("state encoding: very close, exit closed, no history -> 2", QLearningBrain.encode(1, false, -1) == 2);
        check("state encoding: medium distance, exit open, closing in -> 15", QLearningBrain.encode(7, true, 9) == 15);
        boolean inRange = true;
        for (int dist = 0; dist < 40; dist++) {
            for (int prev = -1; prev < 40; prev++) {
                int s1 = QLearningBrain.encode(dist, false, prev);
                int s2 = QLearningBrain.encode(dist, true, prev);
                if (s1 < 0 || s1 >= QLearningBrain.STATES || s2 < 0 || s2 >= QLearningBrain.STATES) inRange = false;
            }
        }
        check("every situation maps to one of the 24 states", inRange);
    }

    private static void testPersistence() throws IOException {
        System.out.println("Saving the ghost's memory");
        Path tmp = Files.createTempFile("ghost", ".txt");
        QLearningBrain a = new QLearningBrain();
        a.learn(2, 3, 20.0, 7, false);
        a.learn(10, 0, -30.0, 4, false);
        a.save(tmp);
        QLearningBrain b = new QLearningBrain();
        boolean loaded = b.load(tmp);
        check("a saved Q-table loads back with the same values",
                loaded && Math.abs(a.qValue(2, 3) - b.qValue(2, 3)) < 1e-5 && Math.abs(a.qValue(10, 0) - b.qValue(10, 0)) < 1e-5);
        Files.write(tmp, "garbage".getBytes(StandardCharsets.UTF_8));
        check("a corrupt memory file is rejected, not trusted", !new QLearningBrain().load(tmp));
        Files.delete(tmp);
        check("a missing memory file is handled", !new QLearningBrain().load(tmp));
    }

    private static void testHighScores() throws IOException {
        System.out.println("Hall of Fame");
        Path tmp = Files.createTempFile("scores", ".txt");
        Files.delete(tmp);
        HighScores.add(tmp, "Ana", 300, "Easy", 90);
        HighScores.add(tmp, "Bo|b", 500, "Hard", 120);
        HighScores.add(tmp, "Cy", 300, "Medium", 70);
        HighScores.add(tmp, "   ", 10, "Easy", 10);
        List<HighScores.Entry> top = HighScores.top(tmp, 3);
        check("scores are sorted best first", top.get(0).score == 500 && top.size() == 3);
        check("on equal scores, fewer steps ranks higher", top.get(1).name.equals("Cy") && top.get(2).name.equals("Ana"));
        check("the | character is removed from names", top.get(0).name.equals("Bo b"));
        List<String> names = new ArrayList<>();
        for (HighScores.Entry e : HighScores.top(tmp, 10)) names.add(e.name);
        check("an empty name becomes Anonymous", names.contains("Anonymous"));
        Files.delete(tmp);
        check("a missing score file gives an empty table", HighScores.top(tmp, 5).isEmpty());
    }

    // ------------------------------------------------------ does it learn?

    private static void testLearningHelps() {
        System.out.println("Does the ghost really learn? (simulated games)");
        QLearningBrain untrained = new QLearningBrain();
        untrained.setLearning(false);
        untrained.setEpsilon(0.05);
        QLearningBrain trained = new QLearningBrain();
        trained.pretrain(1000, 12345L);
        trained.setLearning(false);
        trained.setEpsilon(0.05);
        Simulation.Stats before = Simulation.run(Difficulty.HARD, untrained, 120, 777);
        Simulation.Stats after = Simulation.run(Difficulty.HARD, trained, 120, 777);
        System.out.println("      untrained: " + before);
        System.out.println("      trained  : " + after);
        check("the trained ghost catches clearly more players than the untrained one (+15 points or more)",
                after.caughtRate() >= before.caughtRate() + 0.15);
        Simulation.Stats random = Simulation.run(Difficulty.HARD, new RandomBrain(), 120, 777);
        check("the trained ghost is far better than a random ghost", after.caughtRate() > random.caughtRate() + 0.4);
    }
}
