//(c) A+ Computer Science
// www.apluscompsci.com
//Name -  

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Player extends BrpsGame
{
    List<brpsChoices> choiceCache;
    List<brpsStates> stateCache;
    List<Player> opponents;
    brpsChoices choice;       //          0                  1             2          3        4       5
    int[] stats = new int[6]; // # of boulders played, # of blankets, # of birds, victories, losses, ties


    public Player() {
        choice = brpsChoices.NONE;
        choiceCache = new ArrayList<>();
        stateCache = new ArrayList<>();
        Arrays.fill(stats, 0);

        opponents = players
                .stream()
                .filter(p -> !p.equals(this)) // All players except this player
                .collect(Collectors.toList());
    }

    public Player(brpsChoices ch, int[] st) {
        assert(st.length == stats.length) : "Player instantiation failed: stats length (" + st.length + ") does not match required length (" + stats.length + ")!";
        choice = ch;
        choiceCache = new ArrayList<>();
        stateCache = new ArrayList<>();
        stats = st;

        opponents = players
                .stream()
                .filter(p -> !p.equals(this))
                .collect(Collectors.toList());
    }

    public void retrieveChoice(int playerIndex) { // REFACTOR
        brpsChoices playerChoice = brpsChoices.valueOf(prompt("Player " + playerIndex + ", choose your selection.", "Error: that's not a choice!", toStringArray(brpsChoices.values()), false, false).toUpperCase());
        if (playerChoice.ordinal() == 0) {
            playerChoice = randomizeChoice();
            System.out.println("You randomly decided on " + playerChoice + "!");
        }

        choice = playerChoice;
    }

    public static brpsChoices randomizeChoice() {
        return brpsChoices.values()[(int)(Math.random()*3+1)]; // indices 1-3. index 0 = none so omitted.
    }

    public void updateStats(brpsStates gameState) {
        switch (choice) {
            case BOULDER -> {
                stats[0]++;
            }

            case BLANKET -> {
                stats[1]++;
            }

            case BIRD -> {
                stats[2]++;
            }
        }

        switch (gameState) {
            case WIN -> {
                stats[3]++;
            }

            case LOSE -> {
                stats[4]++;
            }

            case TIE -> {
                stats[5]++;
            }
        }

        if (choiceCache.size() > 10) // Choice cache (limit 10)
            choiceCache.remove(0);
        choiceCache.add(choice);

        if (stateCache.size() > 10) // Game state cache (limit 10)
            stateCache.remove(0);
        stateCache.add(gameState);
    }
}