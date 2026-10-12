package hauntedmaze.ui;

import hauntedmaze.Difficulty;
import hauntedmaze.ai.Brains;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/** The main window: menu bar, the game panel, and the "choose difficulty" popup. */
public final class GameWindow {
    private final JFrame frame = new JFrame("Haunted Maze: The Learning Ghost");
    private final GamePanel panel = new GamePanel();

    public GameWindow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setResizable(false);
        frame.setJMenuBar(buildMenu());
        panel.setNewGameCallback(this::chooseDifficultyAndStart);
    }

    public void show() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        SwingUtilities.invokeLater(this::chooseDifficultyAndStart);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu game = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New game  (R)");
        newGame.addActionListener(e -> chooseDifficultyAndStart());
        JMenuItem fame = new JMenuItem("Hall of Fame");
        fame.addActionListener(e -> JOptionPane.showMessageDialog(frame, GamePanel.hallOfFameText(),
                "Hall of Fame", JOptionPane.PLAIN_MESSAGE));
        JMenuItem quit = new JMenuItem("Quit");
        quit.addActionListener(e -> System.exit(0));
        game.add(newGame);
        game.add(fame);
        game.addSeparator();
        game.add(quit);

        JMenu ghost = new JMenu("Ghost");
        JMenuItem learned = new JMenuItem("What has the learning ghost learned?");
        learned.addActionListener(e -> showPolicy());
        ghost.add(learned);

        JMenu help = new JMenu("Help");
        JMenuItem how = new JMenuItem("How to play");
        how.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                "You are the witch with the purple hat. A ghost hunts you through the maze!\n\n"
                        + "  - Move with the arrow keys or W A S D.\n"
                        + "  - Collect all 7 candies. Then the exit door opens.\n"
                        + "  - Reach the door to escape. Do not let the ghost touch you.\n"
                        + "  - Orange pumpkins start TRICK OR TREAT:\n"
                        + "      TREAT: freeze the ghost for 4 turns, or win 25 bonus points.\n"
                        + "      TRICK: the ghost gets a burst of speed for 4 turns.\n\n"
                        + "Easy: random ghost.   Medium: A* hunter.   Hard: a ghost that LEARNS.\n"
                        + "Fewer steps means a higher score. Hard mode scores double.",
                "How to play", JOptionPane.INFORMATION_MESSAGE));
        help.add(how);

        bar.add(game);
        bar.add(ghost);
        bar.add(help);
        return bar;
    }

    private void showPolicy() {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String text;
        try {
            text = "What the learning ghost does in each situation (best strategy found):\n\n"
                    + Brains.learningGhost().describePolicy();
        } finally {
            frame.setCursor(Cursor.getDefaultCursor());
        }
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(620, 420));
        JOptionPane.showMessageDialog(frame, scroll, "The ghost's memory", JOptionPane.PLAIN_MESSAGE);
        panel.requestFocusInWindow();
    }

    private void chooseDifficultyAndStart() {
        Object[] options = {"Easy - random ghost", "Medium - A* hunter ghost", "Hard - learning ghost", "Quit"};
        int pick = JOptionPane.showOptionDialog(frame,
                "Collect all 7 candies, open the exit door and escape the ghost!\n\n"
                        + "How smart should the ghost be?",
                "Haunted Maze", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (pick == 3) System.exit(0);
        if (pick < 0) {
            if (!panel.hasGame()) System.exit(0);
            return;
        }
        Difficulty difficulty = Difficulty.values()[pick];
        if (difficulty == Difficulty.HARD) {
            // The learning ghost practises the very first time (about a second).
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        try {
            panel.startGame(difficulty);
        } finally {
            frame.setCursor(Cursor.getDefaultCursor());
        }
        frame.pack();
        frame.setLocationRelativeTo(null);
        panel.requestFocusInWindow();
    }
}
