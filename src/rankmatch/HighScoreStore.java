package rankmatch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/** Append-only UTF-8 text store. Base64 names prevent delimiter/newline injection. */
public final class HighScoreStore {
    public record Entry(int matchesRequired, String name, double score) { }

    private final Path path;

    public HighScoreStore(Path path) {
        this.path = path;
    }

    public void addAll(List<GameResult> results) throws IOException {
        StringBuilder rows = new StringBuilder();
        for (GameResult result : results) {
            String encodedName = Base64.getEncoder().encodeToString(result.setup().name().getBytes(StandardCharsets.UTF_8));
            rows.append(result.setup().matchesRequired()).append('\t')
                .append(encodedName).append('\t').append(result.score()).append('\n');
        }
        Files.writeString(path, rows, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public List<Entry> topFive(int matchesRequired) throws IOException {
        if (matchesRequired < 2 || matchesRequired > 4) {
            throw new IllegalArgumentException("Matches required must be 2-4");
        }
        if (!Files.exists(path)) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>();
        for (String row : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String[] fields = row.split("\t", -1);
            if (fields.length != 3) continue;
            try {
                int group = Integer.parseInt(fields[0]);
                if (group != matchesRequired) continue;
                String name = new String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8);
                double score = Double.parseDouble(fields[2]);
                if (!Double.isFinite(score) || name.isBlank() || name.length() > 40) continue;
                entries.add(new Entry(group, name, score));
            } catch (IllegalArgumentException ignored) {
                // Ignore a damaged row; retain other saved scores.
            }
        }
        entries.sort(Comparator.comparingDouble(Entry::score).reversed());
        return List.copyOf(entries.subList(0, Math.min(5, entries.size())));
    }
}
