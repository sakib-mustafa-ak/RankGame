package rankmatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Creates a fresh, complete deck and shuffles it with Fisher-Yates. */
public final class Deck {
    public static final List<String> RANKS = List.of("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K");
    public static final List<String> SUITS = List.of("Clubs", "Diamonds", "Hearts", "Spades");

    private Deck() { }

    public static List<Card> shuffled(Random random) {
        List<Card> cards = new ArrayList<>(52);
        for (String suit : SUITS) {
            for (String rank : RANKS) {
                cards.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(cards, random);
        return List.copyOf(cards);
    }
}
