package hauntedmaze;

/** An immutable (row, column) position on the maze grid. */
public final class Pos {
    public final int r;
    public final int c;

    public Pos(int r, int c) {
        this.r = r;
        this.c = c;
    }

    public Pos plus(int dr, int dc) {
        return new Pos(r + dr, c + dc);
    }

    /** Straight-line grid distance, used as the A* heuristic. */
    public int manhattan(Pos other) {
        return Math.abs(r - other.r) + Math.abs(c - other.c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pos)) return false;
        Pos p = (Pos) o;
        return r == p.r && c == p.c;
    }

    @Override
    public int hashCode() {
        return 31 * r + c;
    }

    @Override
    public String toString() {
        return "(" + r + "," + c + ")";
    }
}
