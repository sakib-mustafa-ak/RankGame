package rankmatch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

/** Console entry point for RankMatch. Run from the project directory. */
public final class RankMatch {
    private final Scanner input;
    private final Random random;
    private final HighScoreStore scores;

    public RankMatch(Scanner input, Random random, HighScoreStore scores) {
        this.input = input;
        this.random = random;
        this.scores = scores;
    }

    public static void main(String[] args) {
        try {
            new RankMatch(new Scanner(System.in), new Random(),
                new HighScoreStore(Path.of("highscores.txt"))).run();
        } catch (NoSuchElementException interrupted) {
            System.out.println("\nInput ended. Goodbye.");
        }
    }

    public void run() {
        System.out.println("=== RankMatch ===");
        do {
            int count = readInt("Number of players (1-4): ", 1, 4);
            List<GameResult> results = new ArrayList<>();
            for (int player = 1; player <= count; player++) {
                System.out.println("\n--- Player " + player + " ---");
                PlayerSetup setup = readSetup();
                GameResult result = GameEngine.play(setup, Deck.shuffled(random));
                results.add(result);
                showTurn(result);
            }
            announceWinner(results);
            try {
                scores.addAll(results);
                System.out.println("Scores saved to highscores.txt.");
            } catch (IOException error) {
                System.out.println("Could not save high scores: " + error.getMessage());
            }
            if (readYesNo("View high scores? (y/n): ")) showHighScores();
            if (readYesNo("View replay? (y/n): ")) {
                System.out.println("\n=== Game replay ===");
                for (GameResult result : results) showTurn(result);
            }
        } while (readYesNo("Play another game? (y/n): "));
        System.out.println("Goodbye.");
    }

    private PlayerSetup readSetup() {
        String name;
        while (true) {
            System.out.print("Name (enter Computer for an automatic player): ");
            name = input.nextLine().trim();
            if (!name.isBlank() && name.length() <= 40 && name.chars().noneMatch(c -> Character.isISOControl(c))) break;
            System.out.println("Enter a printable name of 1-40 characters.");
        }
        if (name.equalsIgnoreCase("Computer")) {
            int required = 2 + random.nextInt(3);
            String rank = Deck.RANKS.get(random.nextInt(Deck.RANKS.size()));
            int guess = Math.max(1, Math.min(52,
                (int) Math.round(GameEngine.expectedCards(required)) + random.nextInt(9) - 4));
            System.out.printf(Locale.ROOT, "Computer chose rank %s, %d matches, guess %d.%n", rank, required, guess);
            return new PlayerSetup("Computer", rank, required, guess);
        }
        String rank;
        while (true) {
            System.out.print("Target rank (A, 2-10, J, Q, K): ");
            rank = input.nextLine().trim().toUpperCase(Locale.ROOT);
            if (Deck.RANKS.contains(rank)) break;
            System.out.println("Please enter A, 2-10, J, Q or K.");
        }
        int required = readInt("Matches required (2-4): ", 2, 4);
        System.out.printf(Locale.ROOT,
            "Strategy hint: the expected deal count for %d matches is %.1f.%n",
            required, GameEngine.expectedCards(required));
        int guess = readInt("Guess cards needed (1-52): ", 1, 52);
        return new PlayerSetup(name, rank, required, guess);
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = input.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) { }
            System.out.printf("Enter a whole number from %d to %d.%n", min, max);
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = input.nextLine().trim().toLowerCase(Locale.ROOT);
            if (answer.equals("y") || answer.equals("yes")) return true;
            if (answer.equals("n") || answer.equals("no")) return false;
            System.out.println("Enter y or n.");
        }
    }

    private static void showTurn(GameResult result) {
        PlayerSetup setup = result.setup();
        System.out.printf("%n%s | target %s | matches %d | guess %d%n",
            setup.name(), setup.rank(), setup.matchesRequired(), setup.guess());
        int matches = 0;
        for (int i = 0; i < result.dealt().size(); i++) {
            Card card = result.dealt().get(i);
            if (card.rank().equals(setup.rank())) matches++;
            System.out.printf("%2d. %-18s %s (matches: %d)%n",
                i + 1, card, card.rank().equals(setup.rank()) ? "MATCH" : "", matches);
        }
        System.out.printf(Locale.ROOT,
            "Cards dealt: %d | Score: (%d - %d) / %d = %.1f | Prediction error: %d%n",
            result.cardsDealt(), setup.guess(), result.cardsDealt(), setup.matchesRequired(),
            result.score(), result.absoluteError());
    }

    private static void announceWinner(List<GameResult> results) {
        double best = results.stream().mapToDouble(GameResult::score).max().orElseThrow();
        List<String> winners = results.stream().filter(r -> r.score() == best)
            .map(r -> r.setup().name()).toList();
        System.out.printf(Locale.ROOT, "%nWinner%s: %s (score %.1f)%n",
            winners.size() == 1 ? "" : "s", String.join(", ", winners), best);
        int closest = results.stream().mapToInt(GameResult::absoluteError).min().orElseThrow();
        List<String> closestNames = results.stream().filter(r -> r.absoluteError() == closest)
            .map(r -> r.setup().name()).toList();
        System.out.printf("Closest prediction: %s (%d cards away).%n",
            String.join(", ", closestNames), closest);
    }

    private void showHighScores() {
        System.out.println("\n=== Top 5 high scores by match count ===");
        for (int required = 2; required <= 4; required++) {
            System.out.println(required + " matches:");
            try {
                List<HighScoreStore.Entry> entries = scores.topFive(required);
                if (entries.isEmpty()) System.out.println("  No scores yet.");
                for (int i = 0; i < entries.size(); i++) {
                    HighScoreStore.Entry entry = entries.get(i);
                    System.out.printf(Locale.ROOT, "  %d. %s - %.1f%n", i + 1, entry.name(), entry.score());
                }
            } catch (IOException error) {
                System.out.println("  Could not read high scores: " + error.getMessage());
            }
        }
    }
}
