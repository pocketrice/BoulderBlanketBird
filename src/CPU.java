import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.DoubleStream;


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

        final double hRate;
        archetype(double hR) {
            hRate = hR;
        }
    }

    archetype atype;


    // Wikian-only attributes
    boolean hasWikianStrategiesRolled;
    double wikianRerollChance; // Chance that all strategies will be rerolled. Guaranteed 0% first two games, then +10% each add'l game.
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

    public choices archetypedChoice() {
        double[] weights = {0, 0.3333, 0.3333, 0.3333};

        switch (atype) {
            case NONE -> {
                return randomizeChoice();
            }

            case SEAGULL -> { // bC.values = NONE, BOULDER, BLANKET, BIRD
                return weightedRandom(choices.values(), new double[]{0, 0.25, 0.25, 0.5}, false);
            }

            case GEOLOGIST -> {
                return weightedRandom(choices.values(), new double[]{0, 0.5, 0.25, 0.25}, false);
            }

            case INSOMNIAC -> {
                return weightedRandom(choices.values(), new double[]{0, 0.25, 0.5, 0.25}, false);
            }

            case STRATEGIST -> {
                for (choices choice : choiceCache.subList(safeIndex(choiceCache, choiceCache.size()-2), choiceCache.size())) {
                    int winnerOrdinal = (opponents.get(0).choice.ordinal() + 1 > 3) ? 1 : opponents.get(0).choice.ordinal() + 1; // See below chart for logic. This wraps value back to 1 if exceeding max index (3).
                    editWeights(weights, winnerOrdinal, 0.15);
                }

                // Given choice...
                //  0       1        2       3
                // NONE, BOULDER, BLANKET, BIRD

                // The choice that would win against that choice...
                //  0       2        3       1
                // NONE,  BLANKET, BIRD,   BOULDER

                // Thus the winning choice is always 1 ordinal higher than the given choice. 4 wraps back to 1.

            }

            case WATCHREADER -> {
                // TIMES:                 8:00 = bird,         16:00 = boulder,         24:00 = blanket
                // BOOST INTERVALS:   (1) 4:00-12:00 bird, (2) 12:00-20:00 boulder, (3) 20:00-4:00 blanket
                // ORDINALS:                   (3)                     (1)                    (2)
                // Only one option is boosted per time. Boost is relative to how close it is to each option.
                double hourTime = (double) LocalDateTime.now().getSecond() / 3600;
                choices intervalChoice = (hourTime < 12) ? ((hourTime > 4) ? choices.BIRD : choices.BLANKET) : ((hourTime < 20) ? choices.BOULDER : choices.BLANKET); // 4 < time < 12 = bird, 12 < time < 20 = boulder, everything else = blanket
                int hourMarker = ((intervalChoice.ordinal() + 1 > 3) ? 1 : intervalChoice.ordinal() + 1) * 8; // 8 * interval # (equivalent to ordinal+1) = time marker.

                editWeights(weights, intervalChoice.ordinal(), 0.7 - (Math.abs(hourMarker - hourTime) * 0.175)); // Formula: 0.7 - |(n - m) * 0.175|. Thus, at max difference (4 hr), =0 and at min difference (0 hr), =0.7.
            }


            case AMATEUR -> {
                for (choices choice : choiceCache.subList(safeIndex(choiceCache, choiceCache.size()-2), choiceCache.size())) {
                    editWeights(weights, choice.ordinal(), 0.2);
                }
            }


            case PROGRAMMER -> {
                int programDepth = (int) proximityRandom(2.0, 1.0, 3.0); // How in-depth the program gets.
                double bugChance = proximityRandom(0.15, 0.3, 0.3); // The chance the program will run with diff behavior. Increases hintRate due to panic/confusion.
                double errorChance = proximityRandom(0.05, 0.02, 0.1); // Error = the program is completely unrunnable, hintRate is very high and CPU resorts to Amateur behavior.


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
                // It may be even harder to play a wikian than a grandmaster because every few games strategies are rerolled -- although often there are very few strategies, and thus the wikian picks like a normie.


                double[] strategyFailChance = {0.1, 0, 0.2, 0.6, 0.7, 0.3, 0.1}; // The chance a strategy will not be used at all.
                double[] usageFailChance = {0.1, 0.05, 0.1, 0.3, 0.2, 0.1, 0.05}; // The chance a picked strategy will fail (lower than sChance). Increases gradually and plateaus after 2 games.

                if (turnsSinceReroll > 2) // Increment rerollChance (+10% after first 2 games)
                    wikianRerollChance += 0.1;


                if (Math.random() < wikianRerollChance) // Queue a reroll if rerollChance proc'ed.
                    hasWikianStrategiesRolled = false;


                if (!hasWikianStrategiesRolled) { // Reroll all strategies
                    for (int i = 0; i < 7; i++)
                        usageFailChance[i] = (Math.random() > strategyFailChance[i]) ? 1.0 : usageFailChance[i]; // If strategy is not to be used, set its fail chance to 100%.

                    turnsSinceReroll = 0;
                    hasWikianStrategiesRolled = true;
                }


                for (int i = 0; i < 7; i++) {
                    if (Math.random() > usageFailChance[i]) {
                        switch (i) {
                            case 0 -> { // Axiom 1: opponent same move twice = use winning move to other choices // RPS sequence priority
                                for (choices choice : opponents.get(0).choiceCache.subList(safeIndex(choiceCache, choiceCache.size()-2), choiceCache.size())) {
                                    int winnerOrdinalToNext = (opponents.get(0).choice.ordinal() + 2 > 3) ? 1 : opponents.get(0).choice.ordinal() + 1; // todo: TESTME

                                    // given choice
                                    // 0              1       2        3
                                    // none     boulder  blanket   bird

                                    // winner to next in RPS sequence
                                    // 0          3        1         2
                                    // none    bird     blanket  boulder

                                    // Thus the winnerOrdinalToNext = given choice ordinal + 2.

                                    editWeights(weights, weightedRandom(new Integer[]{winnerOrdinalToNext, winnerOrdinalToNext + 1}, new double[]{0.7, 0.3}, false), 0.15);
                                }
                            }

                            case 1 -> { // Axiom 2: play scissors first round
                                if (choiceCache.size() == 0)
                                    return choices.BIRD;
                            }

                            case 2 -> { // Axiom 3: switch moves if lose
                                if (gameState.name().equals("WIN")) // P1 won over P2 (CPU).
                                    editWeights(weights, choice.ordinal(), -1.0);
                            }

                            case 3 -> { // Axiom 4: look for hand hints
                                double successfulHintInterpretationChance = 0.6; // SHIC. Yes, that's quite shic.
                                int winnerOrdinal = (opponents.get(0).choice.ordinal() + 1 > 3) ? 1 : opponents.get(0).choice.ordinal() + 1;

                                if (Math.random() < ((CPU)opponents.get(0)).hintRate) {
                                    System.out.println(embellishMessage(hintEmbellims, "he", this));

                                    if (Math.random() < successfulHintInterpretationChance)
                                        editWeights(weights, winnerOrdinal, 0.7);

                                }
                                else {
                                    System.out.println(embellishMessage(failedHintEmbellims, "fhe", this));
                                }
                            }

                            case 4 -> { // Axiom 5: reverse psych

                            }

                            case 5 -> { // Axiom 6: predict rock if opponent has lost 3x
                                if (stateCache.size() >= 3) {
                                    Object[] lastThreeStates = opponents.get(0).stateCache.subList(stateCache.size() - 3, stateCache.size()).toArray();
                                    if (Arrays.stream(lastThreeStates).allMatch(s -> s.equals(gameStates.LOSE))) { // Opponent lost 3x
                                        return choices.BLANKET; // Blanket to kill boulder
                                    }
                                }
                            }

                            case 6 -> { // Axiom 7: Paper > scissors > rock in general
                                editWeights(weights, 2, 0.5); // 1st-size weight on paper
                                editWeights(weights, 3, 0.2); // 2nd-size weight on scissors
                            }
                        }
                    }
                }

                turnsSinceReroll++;
            }

            case GRANDMASTER -> { // RPS is a luck-based game. However, professionals try to trick their opponents. Maybe some of that psychology can be implemented to trick the player.
                // All axioms are used.
                // Reverse psychology is very in-depth, hard to detect, and fail rate is nigh zero. Strategies are sometimes switched out to avoid stagnation.


            }
        }

        return weightedRandom(choices.values(), weights, false);
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



    public static <T extends Collection<E>, E> int[] safeIndex(T collection, int startIndex, int endIndex) { // <+> APM
        assert(collection.size() != 0) : "Error: unable to safe-index an empty collection!";
        int maxIndex = collection.size() - 1;

        startIndex = Math.max(startIndex, 0); // disallowed sIndex = negative
        endIndex = Math.min(endIndex, maxIndex); // disallowed eIndex = beyond collection max index

        return new int[]{startIndex, endIndex};
    }

    public static <T extends Collection<E>, E> int safeIndex(T collection, int index) { // <+> APM (overload)
        assert(collection.size() != 0) : "Error: unable to safe-index an empty collection!";
        int maxIndex = collection.size()-1;

        return (index < 0) ? 0 : Math.min(index, maxIndex); // (negative) ? [true] return 0 : ([false] (beyond maxIndex) ? [true] return maxIndex : [false] return index);
    }

    /*public static List<String> generateArchetypeProgram(int depth) {

    }

    public static List<String> compileArchetypeProgram() {

    }*/
}
