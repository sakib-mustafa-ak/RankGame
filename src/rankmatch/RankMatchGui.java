package rankmatch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Swing interface backed by the same game engine and score file as the console. */
public final class RankMatchGui extends JFrame {
    private static final Color NAVY = new Color(27, 52, 87);
    private static final Color BACKGROUND = new Color(245, 248, 252);

    private final Random random = new Random();
    private final HighScoreStore scores = new HighScoreStore(Path.of("highscores.txt"));
    private final List<GameResult> results = new ArrayList<>();

    private final JSpinner playerCount = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
    private final JTextField name = new JTextField(18);
    private final JComboBox<String> rank = new JComboBox<>(Deck.RANKS.toArray(String[]::new));
    private final JSpinner matches = new JSpinner(new SpinnerNumberModel(2, 2, 4, 1));
    private final JSpinner guess = new JSpinner(new SpinnerNumberModel(21, 1, 52, 1));
    private final JLabel hint = new JLabel();
    private final JLabel status = new JLabel("Choose 1-4 players, then start a game.");
    private final JTextArea log = new JTextArea();
    private final JButton startButton = new JButton("Start game");
    private final JButton playButton = new JButton("Play turn");
    private final JButton nextButton = new JButton("Next player");
    private final JButton scoresButton = new JButton("High scores");
    private final JButton replayButton = new JButton("Replay round");
    private final JButton newButton = new JButton("New game");

    private int totalPlayers;
    private int currentPlayer;
    private boolean gameStarted;
    private boolean turnPlayed;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default Swing appearance still works.
            }
            new RankMatchGui().setVisible(true);
        });
    }

    public RankMatchGui() {
        super("RankMatch");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 690);
        setMinimumSize(new java.awt.Dimension(760, 540));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout(16, 16));

        JPanel header = new JPanel();
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("RANKMATCH");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        JLabel subtitle = new JLabel("Predict when the target cards will appear");
        subtitle.setForeground(new Color(220, 232, 245));
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        JPanel controls = new JPanel(new BorderLayout(12, 12));
        controls.setBackground(BACKGROUND);
        controls.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 0));
        JPanel top = new JPanel();
        top.setBackground(BACKGROUND);
        top.add(new JLabel("Players:"));
        top.add(playerCount);
        top.add(startButton);

        JPanel choices = new JPanel(new GridBagLayout());
        choices.setBackground(Color.WHITE);
        choices.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(211, 222, 233)),
            BorderFactory.createEmptyBorder(15, 12, 15, 12)));
        addRow(choices, 0, "Name", name);
        addRow(choices, 1, "Target rank", rank);
        addRow(choices, 2, "Matches", matches);
        addRow(choices, 3, "Guess", guess);
        hint.setForeground(NAVY);
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        addRow(choices, 4, "Strategy hint", hint);

        JPanel actions = new JPanel();
        actions.setBackground(BACKGROUND);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        for (JButton button : List.of(playButton, nextButton, scoresButton, replayButton, newButton)) {
            button.setAlignmentX(CENTER_ALIGNMENT);
            button.setMaximumSize(new java.awt.Dimension(175, 34));
            actions.add(button);
            actions.add(Box.createVerticalStrut(8));
        }
        JPanel side = new JPanel();
        side.setBackground(BACKGROUND);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        top.setAlignmentX(LEFT_ALIGNMENT);
        choices.setAlignmentX(LEFT_ALIGNMENT);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        side.add(top);
        side.add(Box.createVerticalStrut(12));
        side.add(choices);
        side.add(Box.createVerticalStrut(12));
        side.add(actions);
        controls.add(side, BorderLayout.NORTH);
        add(controls, BorderLayout.WEST);

        JPanel resultPanel = new JPanel(new BorderLayout(0, 8));
        resultPanel.setBackground(BACKGROUND);
        resultPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 16));
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        status.setForeground(NAVY);
        resultPanel.add(status, BorderLayout.NORTH);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        log.setMargin(new Insets(12, 12, 12, 12));
        resultPanel.add(new JScrollPane(log), BorderLayout.CENTER);
        add(resultPanel, BorderLayout.CENTER);

        startButton.addActionListener(event -> startGame());
        playButton.addActionListener(event -> playTurn());
        nextButton.addActionListener(event -> nextPlayer());
        scoresButton.addActionListener(event -> showHighScores());
        replayButton.addActionListener(event -> showReplay());
        newButton.addActionListener(event -> reset());
        matches.addChangeListener(event -> updateHint());
        name.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { autoPlayComputer(); }
            public void removeUpdate(DocumentEvent event) { autoPlayComputer(); }
            public void changedUpdate(DocumentEvent event) { autoPlayComputer(); }
        });
        updateHint();
        reset();
    }

    private static void addRow(JPanel panel, int row, String label, java.awt.Component component) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(5, 0, 5, 10);
        panel.add(new JLabel(label + ":"), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.weightx = 1;
        right.insets = new Insets(5, 0, 5, 0);
        panel.add(component, right);
    }

    private void reset() {
        gameStarted = false;
        turnPlayed = false;
        results.clear();
        currentPlayer = 0;
        name.setText("");
        rank.setSelectedIndex(0);
        matches.setValue(2);
        guess.setValue(21);
        playerCount.setEnabled(true);
        startButton.setEnabled(true);
        setChoicesEnabled(false);
        playButton.setEnabled(false);
        nextButton.setEnabled(false);
        scoresButton.setEnabled(false);
        replayButton.setEnabled(false);
        newButton.setEnabled(false);
        status.setText("Choose 1-4 players, then start a game.");
        log.setText("");
    }

    private void startGame() {
        totalPlayers = (Integer) playerCount.getValue();
        currentPlayer = 1;
        gameStarted = true;
        turnPlayed = false;
        playerCount.setEnabled(false);
        startButton.setEnabled(false);
        newButton.setEnabled(true);
        setChoicesEnabled(true);
        playButton.setEnabled(true);
        status.setText("Player 1 of " + totalPlayers + ": enter choices and play.");
        log.setText("=== New RankMatch game ===\n\n");
        name.requestFocusInWindow();
    }

    private void nextPlayer() {
        currentPlayer++;
        turnPlayed = false;
        name.setText("");
        setChoicesEnabled(true);
        playButton.setEnabled(true);
        nextButton.setEnabled(false);
        status.setText("Player " + currentPlayer + " of " + totalPlayers + ": enter choices and play.");
        name.requestFocusInWindow();
    }

    private void setChoicesEnabled(boolean enabled) {
        name.setEnabled(enabled);
        rank.setEnabled(enabled);
        matches.setEnabled(enabled);
        guess.setEnabled(enabled);
    }

    private void autoPlayComputer() {
        if (gameStarted && !turnPlayed && name.getText().trim().equalsIgnoreCase("Computer")) {
            SwingUtilities.invokeLater(() -> {
                if (gameStarted && !turnPlayed && name.getText().trim().equalsIgnoreCase("Computer")) {
                    playTurn();
                }
            });
        }
    }

    private void updateHint() {
        int required = (Integer) matches.getValue();
        hint.setText(String.format(Locale.ROOT, "Expected: %.1f cards", GameEngine.expectedCards(required)));
    }

    private PlayerSetup readSetup() throws ParseException {
        String playerName = name.getText().trim();
        if (playerName.equalsIgnoreCase("Computer")) {
            int required = 2 + random.nextInt(3);
            String target = Deck.RANKS.get(random.nextInt(Deck.RANKS.size()));
            int prediction = Math.max(1, Math.min(52,
                (int) Math.round(GameEngine.expectedCards(required)) + random.nextInt(9) - 4));
            return new PlayerSetup("Computer", target, required, prediction);
        }
        matches.commitEdit();
        guess.commitEdit();
        return new PlayerSetup(playerName, (String) rank.getSelectedItem(),
            (Integer) matches.getValue(), (Integer) guess.getValue());
    }

    private void playTurn() {
        if (!gameStarted || turnPlayed) return;
        try {
            GameResult result = GameEngine.play(readSetup(), Deck.shuffled(random));
            results.add(result);
            log.append(formatTurn(result));
            log.setCaretPosition(log.getDocument().getLength());
            turnPlayed = true;
            setChoicesEnabled(false);
            playButton.setEnabled(false);
            if (currentPlayer < totalPlayers) {
                status.setText("Player " + currentPlayer + " finished. Continue to the next player.");
                nextButton.setEnabled(true);
            } else {
                finishGame();
            }
        } catch (IllegalArgumentException | ParseException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Check your choices", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void finishGame() {
        gameStarted = false;
        double highest = results.stream().mapToDouble(GameResult::score).max().orElseThrow();
        List<String> winners = results.stream().filter(result -> result.score() == highest)
            .map(result -> result.setup().name()).toList();
        int smallestError = results.stream().mapToInt(GameResult::absoluteError).min().orElseThrow();
        List<String> closest = results.stream().filter(result -> result.absoluteError() == smallestError)
            .map(result -> result.setup().name()).toList();
        log.append(String.format(Locale.ROOT, "Winner%s: %s (%.1f points)%n",
            winners.size() == 1 ? "" : "s", String.join(", ", winners), highest));
        log.append("Closest prediction: " + String.join(", ", closest) + " (" + smallestError + " cards away)\n");
        try {
            scores.addAll(results);
            log.append("Scores saved to highscores.txt.\n");
        } catch (IOException error) {
            log.append("Could not save high scores: " + error.getMessage() + "\n");
        }
        scoresButton.setEnabled(true);
        replayButton.setEnabled(true);
        status.setText("Round complete. View scores, replay, or start a new game.");
    }

    static String formatTurn(GameResult result) {
        PlayerSetup setup = result.setup();
        StringBuilder text = new StringBuilder();
        text.append(String.format("%s | target %s | %d matches | guess %d%n",
            setup.name(), setup.rank(), setup.matchesRequired(), setup.guess()));
        int found = 0;
        for (int index = 0; index < result.dealt().size(); index++) {
            Card card = result.dealt().get(index);
            boolean matched = card.rank().equals(setup.rank());
            if (matched) found++;
            text.append(String.format(Locale.ROOT, "%2d. %-18s %-5s matches: %d%n",
                index + 1, card, matched ? "MATCH" : "", found));
        }
        text.append(String.format(Locale.ROOT,
            "Cards dealt: %d | Score: (%d - %d) / %d = %.1f | Error: %d%n%n",
            result.cardsDealt(), setup.guess(), result.cardsDealt(), setup.matchesRequired(),
            result.score(), result.absoluteError()));
        return text.toString();
    }

    private void showHighScores() {
        StringBuilder text = new StringBuilder();
        try {
            for (int required = 2; required <= 4; required++) {
                text.append(required).append(" matches:\n");
                List<HighScoreStore.Entry> entries = scores.topFive(required);
                if (entries.isEmpty()) text.append("  No scores yet.\n");
                for (int index = 0; index < entries.size(); index++) {
                    HighScoreStore.Entry entry = entries.get(index);
                    text.append(String.format(Locale.ROOT, "  %d. %s - %.1f%n",
                        index + 1, entry.name(), entry.score()));
                }
                text.append('\n');
            }
            showText("Top five high scores", text.toString(), 14);
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, "Could not read high scores: " + error.getMessage(),
                "High score error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showReplay() {
        StringBuilder text = new StringBuilder("=== Round replay ===\n\n");
        for (GameResult result : results) text.append(formatTurn(result));
        showText("Round replay", text.toString(), 24);
    }

    private void showText(String title, String content, int rows) {
        JTextArea area = new JTextArea(content, rows, 65);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), title, JOptionPane.INFORMATION_MESSAGE);
    }
}
