package hauntedmaze.ai;

import hauntedmaze.GameState;
import hauntedmaze.Pos;

import java.util.List;
import java.util.Random;

/** Easy ghost: wanders aimlessly, never looking at where the player is. */
public final class RandomBrain implements GhostBrain {
    @Override
    public String name() {
        return "Random";
    }

    @Override
    public Pos nextMove(GameState state, Random rng) {
        List<Pos> options = state.maze.neighbors(state.ghost());
        if (options.isEmpty()) return state.ghost();
        return options.get(rng.nextInt(options.size()));
    }
}
