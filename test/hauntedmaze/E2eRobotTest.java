package hauntedmaze;

import hauntedmaze.ai.AStar;
import hauntedmaze.ai.GhostBrain;
import hauntedmaze.ui.GameWindow;
import hauntedmaze.ui.GamePanel;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * Real end-to-end test: opens the real window on a (virtual) screen and
 * drives it with a Robot that presses real keys.
 *   java -cp out hauntedmaze.E2eRobotTest window  outDir   (difficulty popup + play)
 *   java -cp out hauntedmaze.E2eRobotTest tot     outDir   (walk onto a pumpkin -> popups)
 * Needs a display, e.g.:  xvfb-run -a java ...
 */
public final class E2eRobotTest {
    private E2eRobotTest() {
    }

    private static File dir;
    private static Robot robot;

    private static void shot(String name) throws Exception {
        Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
        BufferedImage img = robot.createScreenCapture(new java.awt.Rectangle(sz));
        ImageIO.write(img, "png", new File(dir, name));
    }

    private static void tap(int key) throws Exception {
        robot.keyPress(key);
        robot.keyRelease(key);
        Thread.sleep(120);
    }

    private static void clickScreenCentre() throws Exception {
        Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
        robot.mouseMove(sz.width / 2, sz.height / 2 + 60);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(200);
    }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        dir = new File(args[1]);
        dir.mkdirs();
        robot = new Robot();
        robot.setAutoDelay(40);

        if (mode.equals("window")) {
            SwingUtilities.invokeLater(() -> new GameWindow().show());
            Thread.sleep(2500);
            shot("e1_difficulty_popup.png");
            tap(KeyEvent.VK_ENTER);            // "Easy" is the default button
            Thread.sleep(1200);
            clickScreenCentre();
            int[] keys = {KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN,
                    KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_D, KeyEvent.VK_S, KeyEvent.VK_W, KeyEvent.VK_A};
            for (int round = 0; round < 6; round++) for (int k : keys) tap(k);
            Thread.sleep(300);
            shot("e2_after_keys.png");
        } else {
            GhostBrain still = new GhostBrain() {
                @Override
                public String name() {
                    return "still";
                }

                @Override
                public Pos nextMove(GameState s, Random rng) {
                    return s.ghost();
                }
            };
            GameState gs = GameState.newGame(Difficulty.EASY, still, new Random(5));
            GamePanel panel = new GamePanel();
            panel.startGame(gs);
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame("e2e");
                f.add(panel);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            });
            Thread.sleep(1200);
            clickScreenCentre();
            // Choose a pumpkin whose route does not run over the (motionless) ghost.
            List<Pos> path = null;
            for (Pos pumpkin : gs.pumpkins()) {
                List<Pos> candidate = AStar.findPath(gs.maze, gs.player(), pumpkin);
                if (!candidate.contains(gs.ghost())) {
                    path = candidate;
                    break;
                }
            }
            for (int i = 1; i < path.size(); i++) {
                Pos a = path.get(i - 1);
                Pos b = path.get(i);
                int key = b.r < a.r ? KeyEvent.VK_UP : b.r > a.r ? KeyEvent.VK_DOWN : b.c < a.c ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT;
                if (i == path.size() - 1) Thread.sleep(300);
                tap(key);
            }
            Thread.sleep(900);
            shot("t1_trick_or_treat_popup.png");
            tap(KeyEvent.VK_ENTER);            // knock on the left door
            Thread.sleep(900);
            shot("t2_result_popup.png");
            tap(KeyEvent.VK_ENTER);
            Thread.sleep(500);
            shot("t3_back_in_game.png");
            System.out.println("pumpkins left: " + gs.pumpkins().size() + " (started with " + GameState.PUMPKIN_COUNT
                    + "), pending: " + gs.isTrickOrTreatPending() + ", steps: " + gs.steps());
        }
        System.exit(0);
    }
}
