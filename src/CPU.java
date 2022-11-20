import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;
import org.javatuples.Pair;


public class CPU extends Player {
    enum archetype {
        NONE(0.3), // No changes.
        SEAGULL(0.3), // Favors bird. Same hint rate.
        GEOLOGIST(0.3), // Favors boulder. SHR.
        INSOMNIAC(0.3), // Favors blanket. SHR.
        STRATEGIST(0.15), // No favoring; skewed towards winning choice against last 2 rounds. LHR.
        WATCHREADER(0.15), // Favors one type based on the time of day. Morning = bird, noon = boulder, night = blanket. LHR.
        AMATEUR(0.7), // No favoring; skewed towards using the same option repeatedly. MHHR.
        PROGRAMMER(0.15), // Generates a random "program" for favors/skews. LHR.
        WIKIAN(0.5), // uses a strategy from https://www.wikihow.com/Win-at-Rock,-Paper,-Scissors. HHR.
        GRANDMASTER(0.05) // uses only the best strategies. Massively lower hint rate (MLHR).
        ;

        public final double hRate;
        archetype(double hR) {
            hRate = hR;
        }
    }

    archetype atype;
    double hintRate; // 0.0 - 1.0. Higher = more hints (bad for winning)

    boolean hasWikianStrategiesRolled;
    double wikianRerollChance; // (WIKIAN) Chance that all strategies will be rerolled. Guaranteed 0% first two games, then +10% each add'l game.
    int turnsSinceReroll;

    public CPU() {
        atype = weightedRandom(archetype.values(), new double[]{0.3, 0.08, 0.08, 0.08, 0.1, 0.09, 0.1, 0.05, 0.1, 0.02}, false);
        hintRate = proximityRandom(atype.hRate, 0.1, 0.05);
        turnsSinceReroll = 0;


        if (atype.name().equals("WIKIAN")) { // Wikian archetype
            hasWikianStrategiesRolled = false;
            wikianRerollChance = 0;
        }
    }

    public CPU(archetype at, double hR) {
        atype = at;
        turnsSinceReroll = 0;
        if (hR > 1 || hR < 0)
            hintRate = 0.3; // Default hint rate (SHR) if invalid hR.
        else
            hintRate = proximityRandom(hR, 0.1, 0.05);


        if (atype.name().equals("WIKIAN")) { // Wikian archetype
            hasWikianStrategiesRolled = false;
            wikianRerollChance = 0;
        }

    }

    public brpsChoices archetypedChoice() { // todo: custom messages for each archetype! Hints at the CPU's archetype and adds some flair (e.g. "Your opponent anxiously looks at their watch." "Your opponent holds a stern poker face and grins.")
        switch (atype) {
            case NONE -> {
                return randomizeChoice();
            }

            case SEAGULL -> { // bC.values = NONE, BOULDER, BLANKET, BIRD
                return weightedRandom(brpsChoices.values(), new double[]{0, 0.25, 0.25, 0.5}, false);
            }

            case GEOLOGIST -> {
                return weightedRandom(brpsChoices.values(), new double[]{0, 0.5, 0.25, 0.25}, false);
            }

            case INSOMNIAC -> {
                return weightedRandom(brpsChoices.values(), new double[]{0, 0.25, 0.5, 0.25}, false);
            }

            case STRATEGIST -> {
                double[] weights = {0, 0.3333, 0.3333, 0.3333};

                for (brpsChoices choice : choiceCache.subList(choiceCache.size()-2, choiceCache.size())) {
                    int winnerOrdinal = (choice.ordinal() + 1 > 3) ? 1 : choice.ordinal() + 1; // See below chart for logic. This wraps value back to 1 if exceeding max index (3).
                    editWeights(weights, winnerOrdinal, 0.15);
                }

                return weightedRandom(brpsChoices.values(), weights,false);


                // Given choice...
                //  0       1        2       3
                // NONE, BOULDER, BLANKET, BIRD

                // The choice that would win against that choice...
                //  0       2        3       1
                // NONE,  BLANKET, BIRD,   BOULDER

                // Thus the winning choice is always 1 ordinal higher than the given choice. 4 wraps back to 1.

            }

            case WATCHREADER -> {

            }


            case AMATEUR -> {
                double[] weights = {0, 0.3333, 0.3333, 0.3333};

                for (brpsChoices choice : choiceCache.subList(choiceCache.size()-2, choiceCache.size())) {
                    editWeights(weights, choice.ordinal(), 0.2);
                }

                return weightedRandom(brpsChoices.values(), weights, false);
            }


            case PROGRAMMER -> { // todo: roll all values below
                int programDepth = 3; // How in-depth the program gets.
                double bugChance = 0.15; // The chance the program will run with diff behavior. Increases hintRate due to panic/confusion.
                double errorChance = 0.05; // Error = the program is completely unrunnable, hintRate is very high and CPU resorts to Amateur behavior.
            }

            case WIKIAN -> { // https://www.wikihow.com/Win-at-Rock,-Paper,-Scissors
                // Axioms
                // - If opponent has thrown same move twice, then throw the winning move against the other two choices. Higher likelihood opponent will throw the next move in the rock-paper-scissors sequence.
                // - Plays scissors during the first round
                // - Switch moves if you lose
                // - Analyze for hand hints (seeing one of a type = higher likelihood)
                // - Reverse psychology
                // - Predicts rock from opponent if opponent has lost over three times
                // - Statistically, most thrown moves are rock > paper > scissors, meaning rates are skewed as paper > scissors > rock.

                // A wikian will only use a few of these axioms. Reverse psychology / hint analysis is used sparingly and often fails too.
                // It may be even harder to play a wikian than a grandmaster because every few games strategies are rerolled -- although often there are very few strategies, and thus the wikian picks like an amateur.


                double[] weights = {0, 0.3333, 0.3333, 0.3333};
                double[] strategyFailChance = {0.1, 0, 0.2, 0.6, 0.7, 0.3, 0.1}; // The chance a strategy will not be used at all.
                double[] usageFailChance = {0.1, 0.05, 0.1, 0.3, 0.2, 0.1, 0.05}; // The chance a picked strategy will fail (lower than sChance). Increases gradually and plateaus after 2 games.

                if (turnsSinceReroll > 2) // Increment rerollChance (+10% after first 2 games)
                    wikianRerollChance += 0.1;


                if (Math.random() < wikianRerollChance) // Queue a reroll if rerollChance proc'ed.
                    hasWikianStrategiesRolled = false;


                if (!hasWikianStrategiesRolled) { // Reroll all strategies
                    for (int i = 0; i < 7; i++)
                        usageFailChance[i] = (Math.random() > strategyFailChance[i]) ? 1.0 : usageFailChance[i]; // If strategy is not to be used, set its fail chance to 100%.

                    hasWikianStrategiesRolled = true;
                }


                for (int i = 0; i < 7; i++) {
                    if (Math.random() > usageFailChance[i]) {
                        switch (i) {
                            case 0 -> { // Axiom 1: opponent same move twice = use winning move to other choices // RPS sequence priority
                                players.get(players.indexOf(this)-1)
                            }

                            case 1 -> { // Axiom 2: play scissors first round
                                if (choiceCache.size() == 0)
                                    return brpsChoices.BIRD;
                            }

                            case 2 -> { // Axiom 3: switch moves if lose
                                if (gameState.name().equals("WIN")) // P1 won over P2 (CPU).
                                    editWeights(weights, choice.ordinal(), -1.0);
                            }

                            case 3 -> { // Axiom 4: look for hand hints
                                if ()
                            }

                            case 4 -> {

                            }

                            case 5 -> {

                            }

                            case 6 -> {
                                editWeights(weights, 2, 0.5); // weighted paper > scissors > rock
                                editWeights(weights, 3, 0.2);
                            }
                        }
                    }
                }

                turnsSinceReroll++;
                return weightedRandom(brpsChoices.values(), weights, false);
            }

            case GRANDMASTER -> { // RPS is a luck-based game. However, professionals try to trick their opponents. Maybe some of that psychology can be implemented to trick the player.
                // All axioms are used.
                // Reverse psychology is very in-depth, hard to detect, and fail rate is nigh zero. Strategies are sometimes switched out to avoid stagnation.


            }

        }
    }


    public static <T> T weightedRandom(T[] choices, double[] weights, boolean autoEqualize) // Yoinked from Digitridoo
    {
        double rng = Math.random();

        if (autoEqualize) {
            Arrays.fill(weights, 1.0 / choices.length);
        }

        assert (DoubleStream.of(weights).sum() != 1) : "weightedRandom weights do not add up to 1 (= " + DoubleStream.of(weights).sum() + ")!";
        assert (choices.length == weights.length) : "weightedRandom choice (" + choices.length + ") and weights (" + weights.length + ") array are not the same length!";

        for (int i = 0; i < weights.length; i++) {
            if (rng < weights[i])
                return choices[i];
            else
                rng -= weights[i];
        }

        return null;
    }


    public static void editWeights(double[] weights, int index, double weightOffset) {
        assert(Math.abs(weightOffset) <= 1.0) : "Error: weightOffset exceeds ±100%!";

        for (int i = 0; i < weights.length; i++) {
            weights[i] = (i == index) ? weights[i] + weightOffset : weights[i] - (weightOffset / weights.length); // Add offset to selected index, and subtract equal parts from others accordingly.
        }
    }


    // Returns a random number that's near a base #. lower is how much below base, upper is how much above base. See proof.
    public double proximityRandom(double base, double lowerOffset, double upperOffset) { // <+> APM
        return Math.random()*(lowerOffset + upperOffset) + base - lowerOffset;
    }
}
