# Haunted Maze: The Learning Ghost

A Halloween maze game in pure Java (Swing, no libraries). You are a witch who must collect 7 candies, open the exit door and escape a ghost. The ghost's brain depends on the difficulty, and on Hard the ghost learns.

## Run it
Needs a JDK (Java 11 or newer). Check with `java -version` and `javac -version`.
- Mac/Linux: `bash run.sh`
- Windows: double-click `run.bat`

Controls: arrow keys or WASD to move, R for a new game. Menu "Ghost > What has the learning ghost learned?" shows the ghost's policy.

## Difficulty = a different ghost brain
| Level | Ghost | How it works |
|---|---|---|
| Easy | Random | wanders; ignores the player |
| Medium | A* hunter | always steps along the shortest path to the player |
| Hard | Q-learning | learns which hunting strategy works best in each situation |

Trick or Treat: orange pumpkins open a popup. Treat = ghost frozen for 4 turns or +25 points. Trick = ghost gets a burst of speed for 4 turns.

## Architecture
```
Main -> ui/GameWindow, ui/GamePanel (Swing: drawing and keys only)
GameState   all rules, no graphics (turn based)
Maze        DFS "recursive backtracker" + extra loops
ai/         GhostBrain (interface), RandomBrain, AStarBrain + AStar, QLearningBrain, RunnerBot (simulated player)
Simulation, Evaluate   run thousands of headless games to measure each brain
HighScores  Hall of Fame in highscores.txt
```
Rules are separate from the UI, so they can be tested and simulated quickly.

## The AI (what to explain)
- **Maze:** depth-first search carves a perfect maze, then some walls are removed to make loops.
- **A*:** f = g + h, g = steps so far, h = Manhattan distance to the goal. h never overestimates on a grid, so the path is shortest (tested against BFS on 400 random pairs).
- **Q-learning:** the ghost picks a strategy (CHASE, AMBUSH, GUARD, PATROL) from 24 states (player distance x exit open x getting closer/further). Update: `Q(s,a) <- Q(s,a) + alpha*(r + gamma*max Q(s',a') - Q(s,a))`. Rewards: -1 per step, +2 when closer, +50 catch, -50 escape. Epsilon-greedy exploration. alpha and epsilon shrink during practice (annealing) so training is stable. The table is saved in `ghost_memory.txt`, so the ghost remembers between games and keeps learning (gently) while you play.
- **Evidence it learns** (simulated runner, Hard speed): untrained ghost catches about 27%, trained ghost about 63-76%, A* about 54%, random about 2%. Run `java -cp out hauntedmaze.Evaluate`.

## Tests
```
javac -d out $(find src test -name "*.java")
java -cp out hauntedmaze.SelfTest
```
41 checks: maze connectivity and loops, A* optimality, game rules, trick or treat, Q-learning maths, saving and loading memory, high scores, and "trained beats untrained". Also verified on a virtual screen with real key presses.

## Limitations (be honest)
- The simulated player (RunnerBot) is simple, so catch rates are a proxy for a human player, not the same thing.
- Q-learning here chooses between 4 strategies (small table), not individual steps, to learn fast. A deep RL agent could do more but needs far more training.
- Results depend on random mazes, so numbers vary a little between runs.
