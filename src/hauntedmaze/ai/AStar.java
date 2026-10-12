package hauntedmaze.ai;

import hauntedmaze.Maze;
import hauntedmaze.Pos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A* shortest-path search on the maze grid.
 *
 * For every tile A* tracks g = steps taken so far and h = the Manhattan
 * distance still to go (an estimate that never over-estimates on a grid, so
 * the result is guaranteed to be a shortest path). It always expands the
 * tile with the smallest f = g + h.
 */
public final class AStar {
    private AStar() {
    }

    private static final class Node {
        final Pos pos;
        final int g;
        final int h;

        Node(Pos pos, int g, int h) {
            this.pos = pos;
            this.g = g;
            this.h = h;
        }

        int f() {
            return g + h;
        }
    }

    /**
     * Returns the path from start to goal, both included, or an empty list
     * when the goal cannot be reached.
     */
    public static List<Pos> findPath(Maze maze, Pos start, Pos goal) {
        if (!maze.isOpen(start) || !maze.isOpen(goal)) return Collections.emptyList();

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingInt(Node::f).thenComparingInt(n -> n.h));
        Map<Pos, Integer> bestG = new HashMap<>();
        Map<Pos, Pos> cameFrom = new HashMap<>();

        bestG.put(start, 0);
        open.add(new Node(start, 0, start.manhattan(goal)));

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (cur.g > bestG.get(cur.pos)) continue; // an outdated queue entry
            if (cur.pos.equals(goal)) return rebuild(cameFrom, goal);

            for (Pos next : maze.neighbors(cur.pos)) {
                int g = cur.g + 1;
                Integer known = bestG.get(next);
                if (known == null || g < known) {
                    bestG.put(next, g);
                    cameFrom.put(next, cur.pos);
                    open.add(new Node(next, g, next.manhattan(goal)));
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<Pos> rebuild(Map<Pos, Pos> cameFrom, Pos goal) {
        List<Pos> path = new ArrayList<>();
        Pos cur = goal;
        path.add(cur);
        while (cameFrom.containsKey(cur)) {
            cur = cameFrom.get(cur);
            path.add(cur);
        }
        Collections.reverse(path);
        return path;
    }
}
