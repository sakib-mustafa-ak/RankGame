package rankmatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;

/** Pure turn logic: draw until the requested matching card appears. */
public final class GameEngine {
    private GameEngine() { }

    public static GameResult play(PlayerSetup setup, List<Card> deck) {
        if (deck == null || deck.size() != 52 || new HashSet<>(deck).size() != 52) {
            throw new IllegalArgumentException("A complete 52-card deck is required");
        }
        int matches = 0;
        for (int index = 0; index < deck.size(); index++) {
            if (deck.get(index).rank().equals(setup.rank())) {
                matches++;
            }
            if (matches == setup.matchesRequired()) {
                int cardsDealt = index + 1;
                double score = BigDecimal.valueOf(setup.guess() - cardsDealt)
                    .divide(BigDecimal.valueOf(setup.matchesRequired()), 1, RoundingMode.HALF_UP)
                    .doubleValue();
                return new GameResult(setup, deck.subList(0, cardsDealt), score);
            }
        }
        throw new IllegalStateException("Target rank missing from complete deck");
    }

    /** Exact expected position of the nth target in 52 cards containing four targets. */
    public static double expectedCards(int matchesRequired) {
        if (matchesRequired < 2 || matchesRequired > 4) {
            throw new IllegalArgumentException("Matches required must be 2-4");
        }
        return matchesRequired * 53.0 / 5.0;
    }
}
