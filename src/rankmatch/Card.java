package rankmatch;

/** A standard playing card. */
public record Card(String rank, String suit) {
    public Card {
        if (!Deck.RANKS.contains(rank) || !Deck.SUITS.contains(suit)) {
            throw new IllegalArgumentException("Invalid card");
        }
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}
