package hauntedmaze.ai;

import hauntedmaze.GameState;
import hauntedmaze.Pos;

import java.util.List;
import java.util.Random;

/** Medium ghost: always takes the first step of the shortest path to the player. */
public final class AStarBrain implements GhostBrain {
    @Override
    public String name() {
        return "A* hunter";
    }

    @Override
    public Pos nextMove(GameState state, Random rng) {
        List<Pos> path = AStar.findPath(state.maze, state.ghost(), state.player());
        return path.size() >= 2 ? path.get(1) : state.ghost();
    }
}
