package hauntedmaze.ai;

import hauntedmaze.GameState;
import hauntedmaze.Maze;
import hauntedmaze.Pos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple simulated player. It is used to train the learning ghost and to
 * measure how good each ghost brain is, without a human having to play
 * hundreds of games.
 *
 * Behaviour: collect the nearest candy, then run to the exit; if the ghost
 * gets within 4 steps, run away from it; 8% of the time make a random move.
 */
public final class RunnerBot {
    private RunnerBot() {
    }

    /** Returns {dr, dc}, or null if there is no legal move. */
    public static int[] chooseMove(GameState gs, Random rng) {
        Maze maze = gs.maze;
        Pos me = gs.player();

        List<int[]> legal = new ArrayList<>();
        for (int[] d : Maze.DIRS) {
            Pos n = me.plus(d[0], d[1]);
            if (maze.isOpen(n) && !n.equals(gs.ghost())) legal.add(d);
        }
        if (legal.isEmpty()) return null;
        if (rng.nextDouble() < 0.08) return legal.get(rng.nextInt(legal.size()));

        // Run away when the ghost is close.
        int[][] fromGhost = maze.distancesFrom(gs.ghost());
        int ghostDist = fromGhost[me.r][me.c];
        if (ghostDist >= 0 && ghostDist <= 4) {
            return pick(legal, me, fromGhost, true, rng);
        }

        // Otherwise head for the nearest candy, or the exit when it is open.
        Pos target = gs.exitOpen() ? gs.exit() : nearestCandy(gs);
        int[][] toTarget = maze.distancesFrom(target);
        return pick(legal, me, toTarget, false, rng);
    }

    private static Pos nearestCandy(GameState gs) {
        int[][] fromMe = gs.maze.distancesFrom(gs.player());
        Pos best = gs.exit();
        int bestDist = Integer.MAX_VALUE;
        for (Pos candy : gs.candies()) {
            int d = fromMe[candy.r][candy.c];
            if (d >= 0 && d < bestDist) {
                bestDist = d;
                best = candy;
            }
        }
        return best;
    }

    /** Picks the legal move with the largest (or smallest) value in dist; ties are random. */
    private static int[] pick(List<int[]> legal, Pos me, int[][] dist, boolean maximise, Random rng) {
        List<int[]> best = new ArrayList<>();
        int bestValue = maximise ? -1 : Integer.MAX_VALUE;
        for (int[] d : legal) {
            Pos n = me.plus(d[0], d[1]);
            int v = dist[n.r][n.c];
            if (v < 0) continue;
            if ((maximise && v > bestValue) || (!maximise && v < bestValue)) {
                bestValue = v;
                best.clear();
                best.add(d);
            } else if (v == bestValue) {
                best.add(d);
            }
        }
        if (best.isEmpty()) return legal.get(rng.nextInt(legal.size()));
        return best.get(rng.nextInt(best.size()));
    }
}
