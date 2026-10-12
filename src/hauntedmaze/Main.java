package hauntedmaze;

import hauntedmaze.ui.GameWindow;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/** Entry point. Start the game with:  java -cp out hauntedmaze.Main */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("No display found: the game window cannot open on this machine.");
            System.out.println("Run the self-tests instead:  java -cp out hauntedmaze.SelfTest");
            return;
        }
        SwingUtilities.invokeLater(() -> new GameWindow().show());
    }
}
