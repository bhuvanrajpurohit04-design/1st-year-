package hauntedmaze.ui;

import hauntedmaze.Difficulty;
import hauntedmaze.GameState;
import hauntedmaze.HighScores;
import hauntedmaze.Pos;
import hauntedmaze.ai.Brains;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Random;

/**
 * Draws the game and turns key presses into moves. All the pictures (ghost,
 * pumpkin, candy, door) are drawn with Java2D shapes, so no image files
 * and no emoji fonts are needed.
 */
public final class GamePanel extends JPanel {
    private static final int TILE = 28;
    private static final int HUD_HEIGHT = 92;

    private static final Color BG = new Color(0x0d0a14);
    private static final Color WALL = new Color(0x3a1d5c);
    private static final Color WALL_EDGE = new Color(0x7a3fc4);
    private static final Color FLOOR_A = new Color(0x171021);
    private static final Color FLOOR_B = new Color(0x1b1326);
    private static final Color ORANGE = new Color(0xff7518);
    private static final Color GOLD = new Color(0xffd166);

    private GameState state;
    private long tick;
    private boolean endHandled;
    private String lastName = "";
    private Runnable newGameCallback;
    private final Random rng = new Random();

    public GamePanel() {
        setBackground(BG);
        setFocusable(true);
        setPreferredSize(new Dimension(GameState.MAZE_COLS * TILE, GameState.MAZE_ROWS * TILE + HUD_HEIGHT));
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        // Animation only: the game itself is turn based.
        new Timer(90, e -> {
            tick++;
            repaint();
        }).start();
    }

    public void setNewGameCallback(Runnable callback) {
        this.newGameCallback = callback;
    }

    public boolean hasGame() {
        return state != null;
    }

    public void startGame(Difficulty difficulty) {
        state = GameState.newGame(difficulty, Brains.forDifficulty(difficulty), rng);
        endHandled = false;
        repaint();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    /** Exposed for tests that render the panel into an image. */
    public void startGame(GameState existing) {
        state = existing;
        endHandled = false;
    }

    // ------------------------------------------------------------------ input

    private void handleKey(int keyCode) {
        int dr = 0;
        int dc = 0;
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                dr = -1;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                dr = 1;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                dc = -1;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                dc = 1;
                break;
            case KeyEvent.VK_R:
                if (newGameCallback != null) newGameCallback.run();
                return;
            default:
                return;
        }
        if (state == null || state.status() != GameState.Status.PLAYING) return;

        if (state.movePlayer(dr, dc) && state.isTrickOrTreatPending()) {
            repaint();
            showTrickOrTreat();
        }
        repaint();
        if (state.status() != GameState.Status.PLAYING && !endHandled) {
            endHandled = true;
            SwingUtilities.invokeLater(this::handleGameOver);
        }
    }

    /** The Trick or Treat popup: pick a door, then see who answers. */
    private void showTrickOrTreat() {
        Object[] doors = {"Knock on the LEFT door", "Knock on the RIGHT door"};
        JOptionPane.showOptionDialog(this,
                "A cursed pumpkin grins at you...\n\nTwo doors appear in the dark.\nKnock on one and see who answers!",
                "TRICK OR TREAT?", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, doors, doors[0]);
        GameState.TotResult result = state.resolveTrickOrTreat();
        JOptionPane.showMessageDialog(this, result.message,
                result.treat ? "TREAT!" : "TRICK!",
                result.treat ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        requestFocusInWindow();
    }

    private void handleGameOver() {
        Brains.saveLearner(); // the learning ghost remembers this game
        boolean won = state.status() == GameState.Status.WON;
        int score = state.score();
        String title = won ? "YOU ESCAPED!" : "THE GHOST GOT YOU!";
        String summary = title + "\nCandy: " + state.candiesCollected() + "/" + state.candiesTotal()
                + "   Steps: " + state.steps() + "   Score: " + score;
        if (score > 0) {
            Object name = JOptionPane.showInputDialog(this, summary + "\n\nEnter your name for the Hall of Fame:",
                    title, won ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE, null, null, lastName);
            if (name != null) {
                lastName = name.toString();
                HighScores.add(lastName, score, state.difficulty.label, state.steps());
            }
        } else {
            JOptionPane.showMessageDialog(this, summary + "\n\nNo points this time. Try again!", title,
                    JOptionPane.WARNING_MESSAGE);
        }
        JOptionPane.showMessageDialog(this, hallOfFameText(), "Hall of Fame", JOptionPane.PLAIN_MESSAGE);
        if (newGameCallback != null) newGameCallback.run();
    }

    public static String hallOfFameText() {
        List<HighScores.Entry> top = HighScores.top(5);
        if (top.isEmpty()) return "No scores yet. Be the first to escape!";
        StringBuilder sb = new StringBuilder("TOP 5\n\n");
        int rank = 1;
        for (HighScores.Entry e : top) {
            sb.append(rank++).append(".  ").append(e.name).append("  -  ").append(e.score)
                    .append("  (").append(e.difficulty).append(", ").append(e.steps).append(" steps)\n");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (state == null) {
            drawCentered(g, "Choose a difficulty to begin...", getHeight() / 2, 22, GOLD);
            g.dispose();
            return;
        }
        drawHud(g);
        g.translate(0, HUD_HEIGHT);
        drawMaze(g);
        drawExit(g);
        for (Pos p : state.pumpkins()) drawPumpkin(g, p);
        for (Pos p : state.candies()) drawCandy(g, p);
        drawGhost(g);
        drawPlayer(g);
        drawLanternDarkness(g);
        g.translate(0, -HUD_HEIGHT);
        if (state.status() != GameState.Status.PLAYING) drawOverlay(g);
        g.dispose();
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(0x1a1226));
        g.fillRect(0, 0, getWidth(), HUD_HEIGHT);
        g.setColor(ORANGE);
        g.fillRect(0, HUD_HEIGHT - 3, getWidth(), 3);

        g.setFont(new Font(Font.SERIF, Font.BOLD, 26));
        g.setColor(ORANGE);
        g.drawString("HAUNTED MAZE", 12, 30);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.setColor(new Color(0xcdb8ee));
        String right = state.difficulty.label + "  |  " + state.difficulty.brainName;
        g.drawString(right, getWidth() - g.getFontMetrics().stringWidth(right) - 12, 26);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString("Candy " + state.candiesCollected() + "/" + state.candiesTotal()
                + "     Steps " + state.steps() + "     Score " + state.score(), 12, 54);

        String ghostLine;
        Color ghostColor;
        if (state.ghostFrozenTurns() > 0) {
            ghostLine = "Ghost: FROZEN (" + state.ghostFrozenTurns() + ")";
            ghostColor = new Color(0x7fd8ff);
        } else if (state.ghostBoostTurns() > 0) {
            ghostLine = "Ghost: ANGRY (" + state.ghostBoostTurns() + ")";
            ghostColor = new Color(0xff5a4a);
        } else {
            ghostLine = "Ghost: hunting";
            ghostColor = new Color(0xe8e8f5);
        }
        g.setColor(ghostColor);
        String exitLine = state.exitOpen() ? "Exit: OPEN" : "Exit: locked";
        g.drawString(ghostLine + "     " + exitLine, getWidth() - 270, 54);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.setColor(GOLD);
        g.drawString(state.message(), 12, 76);
        g.setColor(new Color(0x8f80ad));
        String hint = "Arrows / WASD: move    R: new game";
        g.drawString(hint, getWidth() - g.getFontMetrics().stringWidth(hint) - 12, 76);
    }

    private void drawMaze(Graphics2D g) {
        for (int r = 0; r < state.maze.rows; r++) {
            for (int c = 0; c < state.maze.cols; c++) {
                int x = c * TILE;
                int y = r * TILE;
                if (state.maze.isWall(r, c)) {
                    g.setColor(WALL);
                    g.fillRect(x, y, TILE, TILE);
                    g.setColor(new Color(WALL_EDGE.getRed(), WALL_EDGE.getGreen(), WALL_EDGE.getBlue(), 90));
                    g.drawRect(x + 1, y + 1, TILE - 3, TILE - 3);
                } else {
                    g.setColor(((r + c) & 1) == 0 ? FLOOR_A : FLOOR_B);
                    g.fillRect(x, y, TILE, TILE);
                }
            }
        }
    }

    private void drawCandy(Graphics2D g, Pos p) {
        int x = p.c * TILE;
        int y = p.r * TILE;
        int cx = x + TILE / 2;
        int cy = y + TILE / 2 + (int) Math.round(Math.sin(tick * 0.3 + p.c) * 1.5);
        g.setColor(GOLD);
        g.fillPolygon(new int[]{cx - 6, cx - 12, cx - 12}, new int[]{cy, cy - 4, cy + 4}, 3);
        g.fillPolygon(new int[]{cx + 6, cx + 12, cx + 12}, new int[]{cy, cy - 4, cy + 4}, 3);
        g.setColor(new Color(0xff5fa2));
        g.fillOval(cx - 7, cy - 5, 14, 10);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine(cx - 2, cy - 4, cx - 2, cy + 4);
        g.drawLine(cx + 2, cy - 4, cx + 2, cy + 4);
    }

    private void drawPumpkin(Graphics2D g, Pos p) {
        int x = p.c * TILE;
        int y = p.r * TILE;
        int glow = 40 + (int) (25 * (1 + Math.sin(tick * 0.35)));
        g.setColor(new Color(255, 117, 24, glow));
        g.fillOval(x - 2, y - 2, TILE + 4, TILE + 4);
        g.setColor(ORANGE);
        g.fillOval(x + 4, y + 8, TILE - 8, TILE - 11);
        g.setColor(new Color(0xd85a00));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(x + 4, y + 8, TILE - 8, TILE - 11);
        g.drawLine(x + TILE / 2, y + 8, x + TILE / 2, y + TILE - 4);
        g.setColor(new Color(0x2e7d32));
        g.fillRect(x + TILE / 2 - 1, y + 4, 3, 6);
        g.setColor(new Color(0x1a0f00));
        g.fillPolygon(new int[]{x + 8, x + 12, x + 10}, new int[]{y + 15, y + 15, y + 12}, 3);
        g.fillPolygon(new int[]{x + 16, x + 20, x + 18}, new int[]{y + 15, y + 15, y + 12}, 3);
        g.drawArc(x + 9, y + 15, 10, 6, 200, 140);
    }

    private void drawExit(Graphics2D g) {
        Pos e = state.exit();
        int x = e.c * TILE;
        int y = e.r * TILE;
        boolean open = state.exitOpen();
        int pulse = 50 + (int) (30 * (1 + Math.sin(tick * 0.3)));
        g.setColor(open ? new Color(46, 204, 113, pulse) : new Color(231, 76, 60, 60));
        g.fillOval(x - 3, y - 3, TILE + 6, TILE + 6);
        g.setColor(new Color(0x7b4a2d));
        g.fillRoundRect(x + 5, y + 3, TILE - 10, TILE - 5, 10, 10);
        g.setColor(open ? GOLD : new Color(0xe74c3c));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x + 5, y + 3, TILE - 10, TILE - 5, 10, 10);
        g.setColor(GOLD);
        g.fillOval(x + TILE - 12, y + TILE / 2, 4, 4);
        if (!open) {
            g.setColor(new Color(0xc0c0c8));
            g.fillRect(x + TILE / 2 - 4, y + TILE / 2 - 1, 8, 7);
            g.setStroke(new BasicStroke(1.6f));
            g.drawArc(x + TILE / 2 - 3, y + TILE / 2 - 6, 6, 8, 0, 180);
        }
    }

    private void drawPlayer(Graphics2D g) {
        Pos p = state.player();
        int x = p.c * TILE;
        int y = p.r * TILE;
        g.setColor(new Color(0x2ec4b6));
        g.fillOval(x + 6, y + 10, TILE - 12, TILE - 12);
        g.setColor(new Color(0x7b2cbf));
        g.fillPolygon(new int[]{x + TILE / 2, x + 7, x + TILE - 7}, new int[]{y + 1, y + 13, y + 13}, 3);
        g.fillRoundRect(x + 4, y + 12, TILE - 8, 4, 3, 3);
        g.setColor(GOLD);
        g.fillRect(x + 9, y + 10, TILE - 18, 2);
        g.setColor(Color.BLACK);
        g.fillOval(x + 10, y + 17, 3, 3);
        g.fillOval(x + TILE - 13, y + 17, 3, 3);
    }

    private void drawGhost(Graphics2D g) {
        Pos p = state.ghost();
        int px = p.c * TILE;
        int py = p.r * TILE;
        boolean frozen = state.ghostFrozenTurns() > 0;
        boolean angry = state.ghostBoostTurns() > 0;

        Color glow = frozen ? new Color(120, 200, 255) : angry ? new Color(255, 70, 50) : new Color(170, 200, 255);
        int alpha = 70 + (int) (35 * (1 + Math.sin(tick * 0.4)));
        float cx = px + TILE / 2f;
        float cy = py + TILE / 2f;
        g.setPaint(new RadialGradientPaint(cx, cy, TILE * 1.4f, new float[]{0f, 1f},
                new Color[]{new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), alpha),
                        new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)}));
        g.fillOval((int) (cx - TILE * 1.4f), (int) (cy - TILE * 1.4f), (int) (TILE * 2.8f), (int) (TILE * 2.8f));

        double w = TILE - 6;
        double h = TILE - 3;
        double x0 = px + 3;
        double y0 = py + 2 + Math.sin(tick * 0.4) * 1.5;
        Path2D.Double body = new Path2D.Double();
        body.moveTo(x0, y0 + h);
        body.lineTo(x0, y0 + w / 2);
        body.curveTo(x0, y0 - w * 0.15, x0 + w, y0 - w * 0.15, x0 + w, y0 + w / 2);
        body.lineTo(x0 + w, y0 + h);
        double seg = w / 3;
        for (int i = 0; i < 3; i++) {
            double xr = x0 + w - i * seg;
            body.quadTo(xr - seg / 2, y0 + h - 7, xr - seg, y0 + h);
        }
        body.closePath();
        g.setColor(frozen ? new Color(0xcdeeff) : angry ? new Color(0xffd6d0) : new Color(0xf4f4ff));
        g.fill(body);
        g.setColor(angry ? new Color(0xb00020) : Color.BLACK);
        g.fillOval((int) (x0 + w * 0.22), (int) (y0 + w * 0.32), 4, 5);
        g.fillOval((int) (x0 + w * 0.60), (int) (y0 + w * 0.32), 4, 5);
        g.fillOval((int) (x0 + w * 0.40), (int) (y0 + w * 0.62), 5, angry ? 6 : 4);
    }

    /** A soft darkness around the edges makes it feel like a lantern in a haunted maze. */
    private void drawLanternDarkness(Graphics2D g) {
        Pos p = state.player();
        float cx = p.c * TILE + TILE / 2f;
        float cy = p.r * TILE + TILE / 2f;
        float radius = TILE * 11f;
        g.setPaint(new RadialGradientPaint(cx, cy, radius, new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)}));
        g.fillRect(0, 0, state.maze.cols * TILE, state.maze.rows * TILE);
    }

    private void drawOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(0, HUD_HEIGHT, getWidth(), getHeight() - HUD_HEIGHT);
        boolean won = state.status() == GameState.Status.WON;
        int mid = HUD_HEIGHT + (getHeight() - HUD_HEIGHT) / 2;
        drawCentered(g, won ? "YOU ESCAPED!" : "THE GHOST GOT YOU!", mid - 10, 44,
                won ? new Color(0x7bd88f) : new Color(0xff4d6d));
        drawCentered(g, "Score " + state.score() + "   -   press R for a new game", mid + 30, 18, Color.WHITE);
    }

    private void drawCentered(Graphics2D g, String text, int y, int size, Color color) {
        g.setFont(new Font(Font.SERIF, Font.BOLD, size));
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, y);
    }
}
