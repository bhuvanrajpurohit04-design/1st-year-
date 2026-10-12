import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class HauntedMaze extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HauntedMaze game = new HauntedMaze();
            game.setVisible(true);
        });
    }

    public HauntedMaze() {
        setTitle("👻 Haunted Maze: The Learning Ghost");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    // Grid settings (Odd dimensions DFS maze generation ke liye best rehti hain)
    public static final int ROWS = 19;
    public static final int COLS = 19;
    public static final int TILE_SIZE = 34;

    // Tile Types
    public static final int WALL = 1;
    public static final int PATH = 0;
    public static final int CANDY = 2;
    public static final int PUMPKIN = 3;
    public static final int EXIT = 4;

    private int[][] maze = new int[ROWS][COLS];

    // Player and Ghost state
    private int playerX = 1, playerY = 1;
    private int ghostX = ROWS - 2, ghostY = COLS - 2;

    // Game stats
    private int score = 0;
    private int candiesRemaining = 0;
    private int highScore = 0;
    private String difficulty = "Medium"; // "Easy", "Medium", "Hard"
    private boolean gameOver = false;
    private boolean gameWon = false;

    // Ghost mechanics
    private javax.swing.Timer ghostTimer;
    private int normalGhostSpeed = 380; // ms per move
    private int ghostFreezeTicks = 0;

    // Q-Learning Table: State(hash of relative pos) -> Action values [Up, Down, Left, Right]
    private static Map<String, double[]> qTable = new HashMap<>();
    private final double ALPHA = 0.2;  // Learning rate
    private final double GAMMA = 0.8;  // Discount factor
    private final double EPSILON = 0.15; // Exploration rate
    private static final String HIGH_SCORE_FILE = "highscore.txt";

    // HUD labels
    private JLabel infoLabel;
    private JComboBox<String> diffSelector;

    public GamePanel() {
        setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE + 50));
        setBackground(new Color(15, 10, 25)); // Dark Halloween theme
        setLayout(new BorderLayout());
        setFocusable(true);
        addKeyListener(this);

        loadHighScore();
        initControls();
        startNewGame();
    }

    private void initControls() {
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(25, 15, 40));
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 8));

        infoLabel = new JLabel();
        infoLabel.setForeground(Color.ORANGE);
        infoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(infoLabel);

        diffSelector = new JComboBox<>(new String[]{"Easy (Random)", "Medium (A*)", "Hard (Q-Learning)"});
        diffSelector.setSelectedIndex(1);
        diffSelector.setFocusable(false);
        diffSelector.addActionListener(e -> {
            String selected = (String) diffSelector.getSelectedItem();
            if (selected.contains("Easy")) difficulty = "Easy";
            else if (selected.contains("Medium")) difficulty = "Medium";
            else difficulty = "Hard";
            startNewGame();
            requestFocusInWindow();
        });
        topPanel.add(new JLabel("🧠 AI:"));
        ((JLabel)topPanel.getComponent(topPanel.getComponentCount() - 1)).setForeground(Color.LIGHT_GRAY);
        topPanel.add(diffSelector);

        JButton restartBtn = new JButton("🔄 Restart");
        restartBtn.setFocusable(false);
        restartBtn.setBackground(new Color(70, 30, 90));
        restartBtn.setForeground(Color.WHITE);
        restartBtn.addActionListener(e -> {
            startNewGame();
            requestFocusInWindow();
        });
        topPanel.add(restartBtn);

        add(topPanel, BorderLayout.NORTH);
    }

    private void startNewGame() {
        score = 0;
        gameOver = false;
        gameWon = false;
        ghostFreezeTicks = 0;

        generateRandomMaze();

        playerX = 1;
        playerY = 1;
        ghostX = ROWS - 2;
        ghostY = COLS - 2;

        if (ghostTimer != null) ghostTimer.stop();
        ghostTimer = new javax.swing.Timer(normalGhostSpeed, this);
        ghostTimer.start();

        updateHUD();
        repaint();
    }

    // --- MAZE GENERATION USING RANDOMIZED DFS ---
    private void generateRandomMaze() {
        candiesRemaining = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                maze[r][c] = WALL;
            }
        }

        // DFS carved path
        Stack<Point> stack = new Stack<>();
        maze[1][1] = PATH;
        stack.push(new Point(1, 1));
        Random rand = new Random();

        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};

        while (!stack.isEmpty()) {
            Point curr = stack.peek();
            List<int[]> neighbors = new ArrayList<>();

            for (int[] d : dirs) {
                int nr = curr.x + d[0];
                int nc = curr.y + d[1];
                if (nr > 0 && nr < ROWS - 1 && nc > 0 && nc < COLS - 1 && maze[nr][nc] == WALL) {
                    neighbors.add(d);
                }
            }

            if (!neighbors.isEmpty()) {
                int[] chosen = neighbors.get(rand.nextInt(neighbors.size()));
                int wallR = curr.x + chosen[0] / 2;
                int wallC = curr.y + chosen[1] / 2;
                int nextR = curr.x + chosen[0];
                int nextC = curr.y + chosen[1];

                maze[wallR][wallC] = PATH;
                maze[nextR][nextC] = PATH;
                stack.push(new Point(nextR, nextC));
            } else {
                stack.pop();
            }
        }

        // Add candies and pumpkins on paths
        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (maze[r][c] == PATH && !(r == 1 && c == 1) && !(r == ROWS - 2 && c == COLS - 2)) {
                    double p = rand.nextDouble();
                    if (p < 0.05) {
                        maze[r][c] = PUMPKIN; // Pumpkin trap / treat
                    } else if (p < 0.50) {
                        maze[r][c] = CANDY;
                        candiesRemaining++;
                    }
                }
            }
        }

        // Exit point
        maze[ROWS - 2][COLS - 2] = EXIT;
    }

    // --- GAME LOOP / GHOST AI TICK ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver || gameWon) return;

        if (ghostFreezeTicks > 0) {
            ghostFreezeTicks--;
            return;
        }

        moveGhost();
        checkCollisions();
        repaint();
    }

    private void moveGhost() {
        if (difficulty.equals("Easy")) {
            moveGhostRandom();
        } else if (difficulty.equals("Medium")) {
            moveGhostAStar();
        } else {
            moveGhostQLearning();
        }
    }

    // 1. Easy: Random Walk
    private void moveGhostRandom() {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        List<Point> validMoves = new ArrayList<>();
        for (int[] d : dirs) {
            int nx = ghostX + d[0], ny = ghostY + d[1];
            if (isValid(nx, ny)) validMoves.add(new Point(nx, ny));
        }
        if (!validMoves.isEmpty()) {
            Point p = validMoves.get(new Random().nextInt(validMoves.size()));
            ghostX = p.x;
            ghostY = p.y;
        }
    }

    // 2. Medium: A* Pathfinding
    private static class Node implements Comparable<Node> {
        int x, y, g, h;
        Node parent;
        Node(int x, int y, int g, int h, Node parent) {
            this.x = x; this.y = y; this.g = g; this.h = h; this.parent = parent;
        }
        int f() { return g + h; }
        @Override
        public int compareTo(Node o) { return Integer.compare(this.f(), o.f()); }
    }

    private void moveGhostAStar() {
        PriorityQueue<Node> open = new PriorityQueue<>();
        boolean[][] closed = new boolean[ROWS][COLS];
        open.add(new Node(ghostX, ghostY, 0, Math.abs(ghostX - playerX) + Math.abs(ghostY - playerY), null));

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        Node targetNode = null;

        while (!open.isEmpty()) {
            Node curr = open.poll();
            if (curr.x == playerX && curr.y == playerY) {
                targetNode = curr;
                break;
            }
            if (closed[curr.x][curr.y]) continue;
            closed[curr.x][curr.y] = true;

            for (int[] d : dirs) {
                int nx = curr.x + d[0], ny = curr.y + d[1];
                if (isValid(nx, ny) && !closed[nx][ny]) {
                    int h = Math.abs(nx - playerX) + Math.abs(ny - playerY);
                    open.add(new Node(nx, ny, curr.g + 1, h, curr));
                }
            }
        }

        // Trace back first step from ghost
        if (targetNode != null) {
            Node curr = targetNode;
            while (curr.parent != null && curr.parent.parent != null) {
                curr = curr.parent;
            }
            ghostX = curr.x;
            ghostY = curr.y;
        }
    }

    // 3. Hard: Q-Learning Brain
    private void moveGhostQLearning() {
        String state = getQState();
        double[] qValues = qTable.computeIfAbsent(state, k -> new double[4]);

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // 0: Up, 1: Down, 2: Left, 3: Right
        int action;

        // Epsilon-Greedy choice
        if (Math.random() < EPSILON) {
            action = new Random().nextInt(4);
        } else {
            action = getBestAction(qValues);
        }

        int nx = ghostX + dirs[action][0];
        int ny = ghostY + dirs[action][1];

        double reward = -1; // Default step penalty
        if (!isValid(nx, ny)) {
            reward = -20; // Hit wall penalty
        } else {
            int oldDist = Math.abs(ghostX - playerX) + Math.abs(ghostY - playerY);
            int newDist = Math.abs(nx - playerX) + Math.abs(ny - playerY);

            if (nx == playerX && ny == playerY) {
                reward = 100; // Caught player!
            } else if (newDist < oldDist) {
                reward = 10; // Getting closer
            } else {
                reward = -5; // Moving further away
            }

            ghostX = nx;
            ghostY = ny;
        }

        // Update Q-Value: Q(s, a) = Q(s, a) + alpha * [reward + gamma * max(Q(s')) - Q(s, a)]
        String nextState = getQState();
        double[] nextQ = qTable.computeIfAbsent(nextState, k -> new double[4]);
        double maxNextQ = Arrays.stream(nextQ).max().orElse(0.0);
        qValues[action] += ALPHA * (reward + (GAMMA * maxNextQ) - qValues[action]);
    }

    private String getQState() {
        // Discretized relative position of player to ghost
        int dx = Integer.compare(playerX, ghostX);
        int dy = Integer.compare(playerY, ghostY);
        return dx + ":" + dy;
    }

    private int getBestAction(double[] qValues) {
        int best = 0;
        double max = qValues[0];
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > max) {
                max = qValues[i];
                best = i;
            }
        }
        return best;
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS && maze[r][c] != WALL;
    }

    // --- PLAYER MOVEMENT & TRICK-OR-TREAT ---
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver || gameWon) return;

        int key = e.getKeyCode();
        int nx = playerX, ny = playerY;

        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) nx--;
        else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) nx++;
        else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) ny--;
        else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) ny++;

        if (isValid(nx, ny)) {
            playerX = nx;
            playerY = ny;

            // Collect Candy
            if (maze[playerX][playerY] == CANDY) {
                maze[playerX][playerY] = PATH;
                score += 10;
                candiesRemaining--;
            }
            // Trick or Treat Pumpkin
            else if (maze[playerX][playerY] == PUMPKIN) {
                maze[playerX][playerY] = PATH;
                handlePumpkinEvent();
            }
            // Reach Exit Door
            else if (maze[playerX][playerY] == EXIT) {
                if (candiesRemaining == 0) {
                    gameWon = true;
                    score += 100;
                    ghostTimer.stop();
                    checkAndUpdateHighScore();
                    JOptionPane.showMessageDialog(this, "🏆 YOU ESCAPED THE HAUNTED MAZE!\nFinal Score: " + score, "Victory!", JOptionPane.INFORMATION_MESSAGE);
                }
            }

            checkCollisions();
            updateHUD();
            repaint();
        }
    }

    private void handlePumpkinEvent() {
        boolean treat = new Random().nextBoolean();
        if (treat) {
            ghostFreezeTicks = 8; // Freezes ghost for ~3 seconds
            score += 25;
            JOptionPane.showMessageDialog(this, "🍬 TREAT! The ghost got frozen for 3 seconds!", "🎃 Trick-or-Treat", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Trick: Ghost gets boosted speed temporarily
            ghostTimer.setDelay(200);
            new javax.swing.Timer(4000, e -> {
                ghostTimer.setDelay(normalGhostSpeed);
                ((javax.swing.Timer)e.getSource()).stop();
            }).start();
            JOptionPane.showMessageDialog(this, "😱 TRICK! The ghost is super fast for 4 seconds! RUN!", "🎃 Trick-or-Treat", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void checkCollisions() {
        if (playerX == ghostX && playerY == ghostY) {
            gameOver = true;
            ghostTimer.stop();
            checkAndUpdateHighScore();
            repaint();
            JOptionPane.showMessageDialog(this, "💀 The Ghost Caught You!\nYour Score: " + score, "Game Over", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateHUD() {
        infoLabel.setText(String.format("🍬 Candies Left: %d  |  ⭐ Score: %d  |  🏆 High Score: %d", candiesRemaining, score, highScore));
    }

    // --- HIGH SCORE HANDLING ---
    private void loadHighScore() {
        try (BufferedReader br = new BufferedReader(new FileReader(HIGH_SCORE_FILE))) {
            highScore = Integer.parseInt(br.readLine().trim());
        } catch (Exception ignored) {
            highScore = 0;
        }
    }

    private void checkAndUpdateHighScore() {
        if (score > highScore) {
            highScore = score;
            try (PrintWriter pw = new PrintWriter(new FileWriter(HIGH_SCORE_FILE))) {
                pw.println(highScore);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updateHUD();
    }

    // --- DRAWING & GRAPHICS (EMOJI / RETRO STYLE) ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int offsetY = 45; // Below HUD panel

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int px = c * TILE_SIZE;
                int py = r * TILE_SIZE + offsetY;

                if (maze[r][c] == WALL) {
                    g2.setColor(new Color(40, 20, 60));
                    g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    g2.setColor(new Color(25, 10, 40));
                    g2.drawRect(px, py, TILE_SIZE, TILE_SIZE);
                } else {
                    g2.setColor(new Color(18, 14, 28));
                    g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);

                    if (maze[r][c] == CANDY) {
                        drawEmoji(g2, "🍬", px, py);
                    } else if (maze[r][c] == PUMPKIN) {
                        drawEmoji(g2, "🎃", px, py);
                    } else if (maze[r][c] == EXIT) {
                        drawEmoji(g2, candiesRemaining == 0 ? "🚪" : "🔒", px, py);
                    }
                }
            }
        }

        // Draw Player & Ghost
        drawEmoji(g2, "🧙", playerY * TILE_SIZE, playerX * TILE_SIZE + offsetY);
        String ghostSprite = (ghostFreezeTicks > 0) ? "🧊" : "👻";
        drawEmoji(g2, ghostSprite, ghostY * TILE_SIZE, ghostX * TILE_SIZE + offsetY);
    }

    private void drawEmoji(Graphics2D g2, String emoji, int x, int y) {
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        FontMetrics fm = g2.getFontMetrics();
        int fx = x + (TILE_SIZE - fm.stringWidth(emoji)) / 2;
        int fy = y + (TILE_SIZE + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(emoji, fx, fy);
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
}