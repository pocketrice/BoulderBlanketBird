//(c) A+ Computer Science
// www.apluscompsci.com
//Name -  

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Player extends BrpsGame
{
    List<choices> choiceCache;
    List<gameStates> stateCache;
    gameStates gameState;
    List<Player> opponents;
    boolean hasPlayerAhead;
    choices choice;       //          0                  1             2          3        4       5
    int[] stats = new int[6]; // # of boulders played, # of blankets, # of birds, victories, losses, ties


    public Player() {
        choice = choices.NONE;
        choiceCache = new ArrayList<>();
        stateCache = new ArrayList<>();
        Arrays.fill(stats, 0);
        hasPlayerAhead = true;
    }

    public Player(choices ch, int[] st) {
        assert(st.length == stats.length) : "Player instantiation failed: stats length (" + st.length + ") does not match required length (" + stats.length + ")!";
        choice = ch;
        choiceCache = new ArrayList<>();
        stateCache = new ArrayList<>();
        stats = st;
        hasPlayerAhead = true;
    }

    public void retrieveChoice(int playerIndex) { // REFACTOR
        choices playerChoice = choices.valueOf(prompt("Player " + (playerIndex + 1) + ", pick your weapon of choice.", "Error: that's not a choice!", toStringArray(choices.values()), false, false).toUpperCase());
        if (playerChoice.ordinal() == 0) {
            playerChoice = randomizeChoice();
            System.out.println("You randomly decided on " + playerChoice + "!");
        }

        choice = playerChoice;
    }

    public static choices randomizeChoice() {
        return choices.values()[(int)(Math.random()*3+1)]; // indices 1-3. index 0 = none so omitted.
    }

    public void updateStats(gameStates gameState) {
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

    public int indexPlayerAhead(int[] playerOrder) { // 0, 2, 1
        return (playerOrder[players.indexOf(this)] == playerOrder[playerOrder.length-1]) ? playerOrder[players.indexOf(this)] : playerOrder[players.indexOf(this)] + 1; // If index of player == last index, there is no next player.
    }

    public void retrieveOpponents() {
        opponents = players
                .stream()
                .filter(p -> !p.equals(this)) // All players except this player
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "You";
    }
}