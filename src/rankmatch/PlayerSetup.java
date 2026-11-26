package rankmatch;

/** Choices made before a player's turn. */
public record PlayerSetup(String name, String rank, int matchesRequired, int guess) {
    public PlayerSetup {
        if (name == null || name.isBlank() || name.length() > 40 || name.chars().anyMatch(c -> Character.isISOControl(c))) {
            throw new IllegalArgumentException("Name must contain 1-40 printable characters");
        }
        if (!Deck.RANKS.contains(rank)) {
            throw new IllegalArgumentException("Rank must be A, 2-10, J, Q or K");
        }
        if (matchesRequired < 2 || matchesRequired > 4) {
            throw new IllegalArgumentException("Matches required must be 2-4");
        }
        if (guess < 1 || guess > 52) {
            throw new IllegalArgumentException("Guess must be 1-52");
        }
    }
}
