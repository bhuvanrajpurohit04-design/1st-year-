package hauntedmaze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * A grid maze. Walls and corridors are both tiles, so a maze with an odd
 * number of rows and columns always has a solid outer wall.
 *
 * Generation: depth-first "recursive backtracker" (carves a perfect maze,
 * i.e. exactly one path between any two cells), then a few extra walls are
 * knocked down to create loops so that chasing and fleeing are interesting.
 */
public final class Maze {
    /** The four moves: up, down, left, right. */
    public static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    public final int rows;
    public final int cols;
    private final boolean[][] wall;

    private Maze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.wall = new boolean[rows][cols];
        for (boolean[] row : wall) {
            java.util.Arrays.fill(row, true);
        }
    }

    public static Maze generate(int rows, int cols, Random rng) {
        if (rows % 2 == 0 || cols % 2 == 0 || rows < 5 || cols < 5) {
            throw new IllegalArgumentException("rows and cols must be odd and at least 5");
        }
        Maze m = new Maze(rows, cols);

        // 1. Carve a perfect maze with an iterative depth-first search.
        Deque<Pos> stack = new ArrayDeque<>();
        m.wall[1][1] = false;
        stack.push(new Pos(1, 1));
        while (!stack.isEmpty()) {
            Pos cur = stack.peek();
            List<int[]> options = new ArrayList<>();
            for (int[] d : DIRS) {
                int nr = cur.r + 2 * d[0];
                int nc = cur.c + 2 * d[1];
                if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && m.wall[nr][nc]) {
                    options.add(d);
                }
            }
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] d = options.get(rng.nextInt(options.size()));
            m.wall[cur.r + d[0]][cur.c + d[1]] = false;
            m.wall[cur.r + 2 * d[0]][cur.c + 2 * d[1]] = false;
            stack.push(new Pos(cur.r + 2 * d[0], cur.c + 2 * d[1]));
        }

        // 2. Knock down some walls that sit between two corridors -> loops.
        int loopsWanted = rows * cols / 28;
        for (int attempt = 0; attempt < loopsWanted * 40 && loopsWanted > 0; attempt++) {
            int r = 1 + rng.nextInt(rows - 2);
            int c = 1 + rng.nextInt(cols - 2);
            if (!m.wall[r][c]) continue;
            boolean horizontalGap = !m.wall[r][c - 1] && !m.wall[r][c + 1] && m.wall[r - 1][c] && m.wall[r + 1][c];
            boolean verticalGap = !m.wall[r - 1][c] && !m.wall[r + 1][c] && m.wall[r][c - 1] && m.wall[r][c + 1];
            if (horizontalGap || verticalGap) {
                m.wall[r][c] = false;
                loopsWanted--;
            }
        }
        return m;
    }

    public boolean isOpen(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols && !wall[r][c];
    }

    public boolean isOpen(Pos p) {
        return isOpen(p.r, p.c);
    }

    public boolean isWall(int r, int c) {
        return !isOpen(r, c);
    }

    /** Open tiles directly above, below, left and right of p. */
    public List<Pos> neighbors(Pos p) {
        List<Pos> out = new ArrayList<>(4);
        for (int[] d : DIRS) {
            if (isOpen(p.r + d[0], p.c + d[1])) {
                out.add(new Pos(p.r + d[0], p.c + d[1]));
            }
        }
        return out;
    }

    public List<Pos> openCells() {
        List<Pos> out = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!wall[r][c]) out.add(new Pos(r, c));
            }
        }
        return out;
    }

    /**
     * Breadth-first search distances (in steps) from {@code from} to every
     * tile. Walls and unreachable tiles hold -1.
     */
    public int[][] distancesFrom(Pos from) {
        int[][] dist = new int[rows][cols];
        for (int[] row : dist) java.util.Arrays.fill(row, -1);
        if (!isOpen(from)) return dist;
        Deque<Pos> queue = new ArrayDeque<>();
        dist[from.r][from.c] = 0;
        queue.add(from);
        while (!queue.isEmpty()) {
            Pos cur = queue.poll();
            for (Pos n : neighbors(cur)) {
                if (dist[n.r][n.c] < 0) {
                    dist[n.r][n.c] = dist[cur.r][cur.c] + 1;
                    queue.add(n);
                }
            }
        }
        return dist;
    }
}
