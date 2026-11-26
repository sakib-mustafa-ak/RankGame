package rankmatch;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** Dependency-free automated tests. Run with assertions enabled. */
public final class RankMatchTests {
    private static int checked = 0;

    public static void main(String[] args) throws Exception {
        deckHasEveryCardOnce();
        dealStopsAtRequestedMatch();
        scoreSupportsNegativeAndFractionalValues();
        scoreUsesOneDecimalForComparison();
        expectedCountIsCorrect();
        invalidChoicesAreRejected();
        incompleteDeckIsRejected();
        highScoresPersistAndSeparateGroups();
        malformedScoreRowsAreIgnored();
        computerConsoleFlowCompletes();
        guiTurnTextPreservesResult();
        System.out.println("Passed " + checked + " checks.");
    }

    private static void check(boolean condition, String message) {
        checked++;
        if (!condition) throw new AssertionError(message);
    }

    private static void deckHasEveryCardOnce() {
        List<Card> deck = Deck.shuffled(new Random(7));
        check(deck.size() == 52, "52 cards");
        check(new HashSet<>(deck).size() == 52, "no duplicates");
        for (String rank : Deck.RANKS) {
            check(deck.stream().filter(c -> c.rank().equals(rank)).count() == 4, "four of " + rank);
        }
    }

    private static void dealStopsAtRequestedMatch() {
        List<Card> deck = Deck.shuffled(new Random(9));
        for (int required = 2; required <= 4; required++) {
            GameResult result = GameEngine.play(new PlayerSetup("Test", "K", required, 30), deck);
            check(result.dealt().stream().filter(c -> c.rank().equals("K")).count() == required,
                "correct match total");
            check(result.dealt().get(result.dealt().size() - 1).rank().equals("K"), "ends on match");
        }
    }

    private static void scoreSupportsNegativeAndFractionalValues() {
        List<Card> deck = new ArrayList<>(Deck.shuffled(new Random(11)));
        deck.sort((a, b) -> Boolean.compare(b.rank().equals("A"), a.rank().equals("A")));
        GameResult result = GameEngine.play(new PlayerSetup("Test", "A", 2, 1), deck);
        check(result.cardsDealt() == 2, "early stop");
        check(result.score() == -0.5, "signed fractional score");
        check(result.absoluteError() == 1, "absolute error");
    }

    private static void scoreUsesOneDecimalForComparison() {
        List<Card> deck = new ArrayList<>(Deck.shuffled(new Random(11)));
        deck.sort((a, b) -> Boolean.compare(b.rank().equals("A"), a.rank().equals("A")));
        GameResult thirds = GameEngine.play(new PlayerSetup("Thirds", "A", 3, 4), deck);
        GameResult quarters = GameEngine.play(new PlayerSetup("Quarters", "A", 4, 5), deck);
        GameResult negative = GameEngine.play(new PlayerSetup("Negative", "A", 4, 3), deck);
        check(thirds.score() == 0.3, "thirds rounded to one decimal");
        check(quarters.score() == thirds.score(), "equal displayed scores compare equally");
        check(negative.score() == -0.3, "negative half rounds away from zero");
    }

    private static void expectedCountIsCorrect() {
        check(GameEngine.expectedCards(2) == 21.2, "two match expectation");
        check(GameEngine.expectedCards(3) == 31.8, "three match expectation");
        check(GameEngine.expectedCards(4) == 42.4, "four match expectation");
    }

    private static void incompleteDeckIsRejected() {
        boolean rejected = false;
        try {
            GameEngine.play(new PlayerSetup("Test", "K", 2, 10), Deck.shuffled(new Random(1)).subList(0, 51));
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check(rejected, "incomplete deck rejected");
    }

    private static void invalidChoicesAreRejected() {
        boolean rejected = false;
        try { new PlayerSetup("", "K", 2, 10); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "empty name rejected");
        rejected = false;
        try { new PlayerSetup("Test", "Z", 2, 10); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "invalid rank rejected");
        rejected = false;
        try { new PlayerSetup("Test", "K", 5, 10); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "invalid matches rejected");
        rejected = false;
        try { new PlayerSetup("Test", "K", 2, 53); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "invalid guess rejected");
        rejected = false;
        try { new PlayerSetup("Bad\tName", "K", 2, 10); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "control characters rejected");
    }

    private static void highScoresPersistAndSeparateGroups() throws Exception {
        Path file = Files.createTempFile("rankmatch-test-", ".txt");
        try {
            HighScoreStore store = new HighScoreStore(file);
            List<GameResult> results = new ArrayList<>();
            List<Card> deck = Deck.shuffled(new Random(13));
            for (int guess = 10; guess <= 16; guess++) {
                results.add(GameEngine.play(new PlayerSetup("Name;" + guess, "Q", 2, guess), deck));
            }
            results.add(GameEngine.play(new PlayerSetup("Other", "Q", 3, 20), deck));
            store.addAll(results);
            HighScoreStore reopened = new HighScoreStore(file);
            List<HighScoreStore.Entry> top = reopened.topFive(2);
            check(top.size() == 5, "top five only");
            check(top.get(0).name().equals("Name;16"), "highest first and safe name encoding");
            check(reopened.topFive(3).size() == 1, "separate group");
            check(reopened.topFive(4).isEmpty(), "empty group");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void malformedScoreRowsAreIgnored() throws Exception {
        Path file = Files.createTempFile("rankmatch-corrupt-", ".txt");
        try {
            Files.writeString(file, "broken row\n2\tVGVzdA==\tNaN\n5\tVGVzdA==\t999\n", StandardCharsets.UTF_8);
            HighScoreStore store = new HighScoreStore(file);
            GameResult valid = GameEngine.play(new PlayerSetup("Valid", "A", 2, 20), Deck.shuffled(new Random(4)));
            store.addAll(List.of(valid));
            check(store.topFive(2).size() == 1, "malformed rows skipped");
            check(store.topFive(2).get(0).name().equals("Valid"), "valid row survives");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void computerConsoleFlowCompletes() throws Exception {
        Path file = Files.createTempFile("rankmatch-console-", ".txt");
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            String commands = "1\nComputer\ny\ny\nn\n";
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            new RankMatch(new Scanner(commands), new Random(21), new HighScoreStore(file)).run();
        } finally {
            System.setOut(original);
            Files.deleteIfExists(file);
        }
        String output = captured.toString(StandardCharsets.UTF_8);
        check(output.contains("Computer chose rank"), "automatic choices displayed");
        check(!output.contains("Target rank ("), "computer needs no choice input");
        check(output.contains("Game replay"), "replay available");
        check(output.contains("Top 5 high scores"), "scores available");
        check(output.contains("Goodbye."), "full console flow completes");
    }

    private static void guiTurnTextPreservesResult() {
        GameResult result = GameEngine.play(
            new PlayerSetup("Gui Test", "A", 2, 20), Deck.shuffled(new Random(17)));
        String text = RankMatchGui.formatTurn(result);
        check(text.contains("Gui Test | target A | 2 matches | guess 20"), "GUI shows setup");
        check(text.contains(result.dealt().get(0).toString()), "GUI shows dealt cards");
        check(text.contains("Cards dealt: " + result.cardsDealt()), "GUI shows result");
    }
}
