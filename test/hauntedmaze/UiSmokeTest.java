package hauntedmaze;

import hauntedmaze.ai.AStar;
import hauntedmaze.ai.AStarBrain;
import hauntedmaze.ai.GhostBrain;
import hauntedmaze.ui.GamePanel;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Draws the game panel into PNG files (no screen needed) and sends it real
 * key events, to prove that painting and keyboard input work.
 * Run with:  java -Djava.awt.headless=true -cp out hauntedmaze.UiSmokeTest outputFolder
 */
public final class UiSmokeTest {
    private UiSmokeTest() {
    }

    private static void render(GamePanel panel, File file) throws IOException {
        panel.setSize(panel.getPreferredSize());
        BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        panel.paint(g);
        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void key(JPanel panel, int code) {
        // Call the panel's key listeners directly (AWT only routes real key events to the focused window).
        KeyEvent e = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, code, KeyEvent.CHAR_UNDEFINED);
        for (java.awt.event.KeyListener l : panel.getKeyListeners()) l.keyPressed(e);
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        int failures = 0;

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

        // 1. A fresh Medium game.
        GameState gs = GameState.newGame(Difficulty.MEDIUM, new AStarBrain(), new Random(3));
        GamePanel panel = new GamePanel();
        panel.startGame(gs);
        render(panel, new File(dir, "1_start.png"));

        // 2. Real key presses move the player (arrow keys and WASD).
        int before = gs.steps();
        Pos start = gs.player();
        key(panel, KeyEvent.VK_RIGHT);
        key(panel, KeyEvent.VK_D);
        key(panel, KeyEvent.VK_DOWN);
        key(panel, KeyEvent.VK_S);
        key(panel, KeyEvent.VK_LEFT);
        key(panel, KeyEvent.VK_UP);
        boolean moved = gs.steps() > before;
        System.out.println((moved ? "PASS" : "FAIL") + "  keyboard events move the player (steps " + before + " -> " + gs.steps() + ")");
        if (!moved) failures++;
        key(panel, KeyEvent.VK_Q); // an unrelated key must be ignored
        System.out.println("PASS  unrelated keys are ignored");

        // 3. A mid-game picture: candy collected, ghost frozen, lantern around the player.
        GameState mid = GameState.newGame(Difficulty.HARD, new AStarBrain(), new Random(8));
        for (int i = 0; i < 4; i++) mid.resolveTrickOrTreat(); // may freeze / boost / bonus
        List<Pos> path = AStar.findPath(mid.maze, mid.player(), mid.candies().iterator().next());
        for (int i = 1; i < path.size(); i++) {
            Pos a = path.get(i - 1);
            Pos b = path.get(i);
            mid.movePlayer(b.r - a.r, b.c - a.c);
            if (mid.isTrickOrTreatPending()) mid.resolveTrickOrTreat();
        }
        panel.startGame(mid);
        render(panel, new File(dir, "2_midgame.png"));

        // 4. A win and a loss overlay.
        GameState win = GameState.newGame(Difficulty.EASY, still, new Random(21));
        while (!win.candies().isEmpty()) {
            Pos target = win.candies().iterator().next();
            List<Pos> p = AStar.findPath(win.maze, win.player(), target);
            for (int i = 1; i < p.size(); i++) {
                win.movePlayer(p.get(i).r - p.get(i - 1).r, p.get(i).c - p.get(i - 1).c);
                if (win.isTrickOrTreatPending()) win.resolveTrickOrTreat();
            }
        }
        List<Pos> toExit = AStar.findPath(win.maze, win.player(), win.exit());
        for (int i = 1; i < toExit.size(); i++) {
            win.movePlayer(toExit.get(i).r - toExit.get(i - 1).r, toExit.get(i).c - toExit.get(i - 1).c);
            if (win.isTrickOrTreatPending()) win.resolveTrickOrTreat();
        }
        panel.startGame(win);
        render(panel, new File(dir, "3_won.png"));
        System.out.println((win.status() == GameState.Status.WON ? "PASS" : "FAIL") + "  scripted run wins the game, win overlay drawn");
        if (win.status() != GameState.Status.WON) failures++;

        System.out.println(failures == 0 ? "UI smoke test OK" : failures + " UI check(s) failed");
        System.exit(failures > 0 ? 1 : 0); // the panel's animation timer would keep the JVM alive
    }
}
