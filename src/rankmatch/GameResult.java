package rankmatch;

import java.util.List;

/** Immutable record of a completed turn, also used for replay. */
public record GameResult(PlayerSetup setup, List<Card> dealt, double score) {
    public GameResult {
        dealt = List.copyOf(dealt);
    }

    public int cardsDealt() {
        return dealt.size();
    }

    public int absoluteError() {
        return Math.abs(setup.guess() - cardsDealt());
    }
}
