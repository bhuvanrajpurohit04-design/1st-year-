package hauntedmaze;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The Hall of Fame: a plain text file with one line per game: score|level|steps|name. */
public final class HighScores {
    public static final Path FILE = Paths.get("highscores.txt");

    private HighScores() {
    }

    public static final class Entry {
        public final String name;
        public final int score;
        public final String difficulty;
        public final int steps;

        Entry(String name, int score, String difficulty, int steps) {
            this.name = name;
            this.score = score;
            this.difficulty = difficulty;
            this.steps = steps;
        }
    }

    public static void add(String name, int score, String difficulty, int steps) {
        add(FILE, name, score, difficulty, steps);
    }

    public static void add(Path file, String name, int score, String difficulty, int steps) {
        String line = score + "|" + difficulty + "|" + steps + "|" + cleanName(name) + "\n";
        try {
            Files.write(file, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // The game still works if the score cannot be saved.
        }
    }

    public static List<Entry> top(int n) {
        return top(FILE, n);
    }

    /** Best scores first; on equal scores, the player who used fewer steps ranks higher. */
    public static List<Entry> top(Path file, int n) {
        List<Entry> all = new ArrayList<>();
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\\|", 4);
                    if (p.length != 4) continue;
                    try {
                        all.add(new Entry(p[3], Integer.parseInt(p[0]), p[1], Integer.parseInt(p[2])));
                    } catch (NumberFormatException ignored) {
                        // skip a damaged line
                    }
                }
            }
        } catch (IOException ignored) {
            // treat as an empty table
        }
        Collections.sort(all, (a, b) -> a.score != b.score ? Integer.compare(b.score, a.score)
                : Integer.compare(a.steps, b.steps));
        return all.size() > n ? new ArrayList<>(all.subList(0, n)) : all;
    }

    static String cleanName(String name) {
        String s = name == null ? "" : name.replace('|', ' ').replace('\n', ' ').replace('\r', ' ').trim();
        if (s.length() > 16) s = s.substring(0, 16);
        return s.isEmpty() ? "Anonymous" : s;
    }
}
