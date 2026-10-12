package hauntedmaze.ai;

import hauntedmaze.GameState;
import hauntedmaze.Pos;

import java.util.Random;

/** The "mind" of the ghost. Different difficulty levels plug in different brains. */
public interface GhostBrain {
    /** Short name shown on the screen and in reports. */
    String name();

    /** Returns the tile the ghost steps to (a neighbour of its current tile). */
    Pos nextMove(GameState state, Random rng);

    /** Called once when the game ends. Learning brains use it to learn. */
    default void onGameEnd(GameState state, boolean ghostCaughtPlayer) {
    }
}
