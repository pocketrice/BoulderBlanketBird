//(c) A+ Computer Science
// www.apluscompsci.com
//Name - Lucas Xie

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.javatuples.Pair;
import org.apache.commons.lang3.ArrayUtils;
import org.javatuples.Tuple;
import org.javatuples.Unit;

public class BrpsGame
{
	private int remainingGames, totalGames;
	private int[] setScore; // Useful for maybe "loading" mid-game for replay.


	public static Scanner input = new Scanner(System.in);

	// MESSAGE EMBELLIMS
	public static Set<Unit<String>> topics = new HashSet<>();
	public static Set<Pair<choices, String>> revPsychEmbellims = new HashSet<>();
	public static Set<Pair<choices, String>> choiceEmbellims = new HashSet<>();
	public static Set<Pair<choices, String>> hintEmbellims = new HashSet<>();
	public static Set<Pair<CPU.archetype, String>> archetypeHintEmbellims = new HashSet<>();
	public static Set<Unit<String>> failedHintEmbellims = new HashSet<>();
	public static Set<Pair<choices, String>> winEmbellims = new HashSet<>();
	public static Set<Pair<choices, String>> tieEmbellims = new HashSet<>();

	public static Set<String> usedEmbellims = new HashSet<>();


	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";

	enum choices {
		NONE,
		BOULDER,
		BLANKET,
		BIRD,
	}

	enum gameStates {
		WIN,
		LOSE,
		TIE,
		INVALID
	}

	public BrpsGame() {
		totalGames = 1;
		remainingGames = 1;
		setScore = new int[]{0, 0};
	}

	public BrpsGame(int tg, int[] ss) {
		assert(ss.length == 2) : "Invalid BRPS instantiation with gameScore length of " + ss.length + "!";
		totalGames = tg;
		remainingGames = tg;
		setScore = ss;
	}

	static List<Player> players = new ArrayList<>();
	static int[] playerOrder;


	public static void main(String[] args) throws InterruptedException {
		populateEmbellims();

		BrpsGame bGame = new BrpsGame( (int) prompt("How many rounds shall we play? (max 15 rounds)", "Error: invalid number.", 1, 15, true), new int[]{0,0});
		int totalPlayerCount = (int) prompt("How many players will be playing (both CPU and human)?", "Error: invalid number. Clamped to 2 temporarily.", 2, 2, true);
		int humanPlayerCount = (int) prompt("How many human players?", "Error: invalid number.", 0, 2, true);
		int cpuPlayerCount = totalPlayerCount - humanPlayerCount;

		for (int i = 0; i < humanPlayerCount; i++) {
			players.add(new Player());
		}

		for (int i = 0; i < cpuPlayerCount; i++) {
			players.add(new CPU());
		}

		players.forEach(Player::retrieveOpponents);



		while (true) {
			if (bGame.remainingGames > 0) {
				System.out.println(ANSI_YELLOW + "\n\t\t\t\t\t\t\t「  GAME " + (bGame.totalGames - bGame.remainingGames + 1) + "  」\t\t");
				System.out.println("==================================================================================" + ANSI_RESET);
				//players = reorderList(players, determineOrder(players));
				players.forEach(e -> e.choice = choices.NONE); // Reset player choices at beginning of each game
				playerOrder = determineOrder(players);

				for (int pIndex : playerOrder) {
					Player player = players.get(pIndex);
					player.hasPlayerAhead = !(pIndex == playerOrder[playerOrder.length - 1]); // If index is at end of playerOrder, then there is no player ahead.

					if (player instanceof CPU) { // CPU
						System.out.println(ANSI_BLUE + "➢ It is now Player " + (players.indexOf(player) + 1) + "'s turn (CPU)." + ANSI_RESET);
						fancyDelay(500, ANSI_CYAN + "Thinking...", "Done!", 2);

						if (player.choice == choices.NONE) {
							player.choice = ((CPU) player).archetypedChoice(); // (CPU)player ensures player is type CPU.
						}
					}

					else { // Human
						System.out.println(ANSI_BLUE + "➢ It is now Player " + (players.indexOf(player) + 1) + "'s turn (Human)." + ANSI_RESET);

						if (!(player.hasPlayerAhead && humanPlayerCount == 2)) { // Always call unless it is Human v. Human and player is first to go
							if (prompt("You have a chance to peek at your opponent's move. Want to try?", "Error: pick yes or no.", new String[]{"yes", "no"}, false, false).equals("yes")) {
									if (Math.random() < player.opponents.get(0).hintRate) {

										if (player.opponents.get(0) instanceof CPU) { // Opponent is CPU {
											player.opponents.get(0).choice = ((CPU) player.opponents.get(0)).archetypedChoice();

											if (Math.random() < 0.4)
												System.out.println(ANSI_CYAN + " ❖ " + embellishMessage(hintEmbellims, "he", player) + "\n" + ANSI_RESET);
											else
												System.out.println(ANSI_CYAN + " ❖ " + embellishMessage(archetypeHintEmbellims, "ahe", player) + "\n" + ANSI_RESET);
										}

										else { // Opponent is Human
											System.out.println(ANSI_CYAN + " ❖ " + embellishMessage(hintEmbellims, "he", player) + "\n" + ANSI_RESET);
										}
									}
									else {
										System.out.println(ANSI_CYAN + " ❖ " + embellishMessage(failedHintEmbellims, "fhe", player) + "\n" + ANSI_RESET);
									}
							}
						}

						player.retrieveChoice(players.indexOf(player), (humanPlayerCount == 2));
					}

					if (humanPlayerCount == 2) {
						System.out.println(ANSI_CYAN + "\nDue to a limitation, you need to manually 'delete' your input at that line." + ANSI_RESET);
						prompt(ANSI_CYAN + "Confirm that you have deleted your input." + ANSI_RESET, "Type 'confirm' to confirm line deletion.", new String[]{"confirm"}, false, false);
						System.out.println();
					}

					System.out.println(ANSI_PURPLE + "(✓) P" + (players.indexOf(player) + 1) + " finished choosing hand.\n\n\n\n" + ANSI_RESET);
				}

				TimeUnit.MILLISECONDS.sleep(1200);
				System.out.println(ANSI_BLUE + "Three...");
				TimeUnit.MILLISECONDS.sleep(1200);
				System.out.println("\t\tTwo...");
				TimeUnit.MILLISECONDS.sleep(1200);
				System.out.println("\t\t\t\tOne...\n");
				TimeUnit.MILLISECONDS.sleep(1200);
				System.out.println("BOULDER,  BLANKET,  BIRD!\n" + ANSI_RESET);

				fancyDelay(500, ANSI_CYAN + "The results are in...", "Now!", 2);
				Player winner = determineWinner(players.get(0), players.get(1));

				for (int pIndex : playerOrder) {
					Player player = players.get(pIndex);

					System.out.println(" ♦  " + embellishMessage(choiceEmbellims, "ce", player));
					//System.out.println(player.choice + "(YOURS) " + player.opponents.get(0).choice + "(OPPO) " + player.gameState); // debug line
					player.updateStats(player.gameState);
					TimeUnit.MILLISECONDS.sleep(4000);
				}

				if (winner == null) { // Tie scenario
					System.out.println(ANSI_CYAN + "\n ♢ " + embellishMessage(tieEmbellims, "te", players.get(playerOrder[0])) + ANSI_GREEN + " It's a tie!" + ANSI_RESET);
				}
				else {
					System.out.println(ANSI_CYAN + "\n ♢ " + embellishMessage(winEmbellims, "we", winner) + ANSI_GREEN + " Player " + (players.indexOf(winner) + 1) + " wins!" + ANSI_RESET);
				}
				bGame.remainingGames--;
				System.out.println(ANSI_YELLOW + "Game " + (bGame.totalGames - bGame.remainingGames) + " finished!\n\n\n" + ANSI_RESET);

				if (bGame.remainingGames == 0)
					fancyDelay(400, ANSI_YELLOW + "Wrapping up and cleaning some last-minute things...", "Done!\n", 5);
				else
					fancyDelay(400, ANSI_YELLOW + "Sit tight, your next game starts soon...", "Ready!\n", 5);

			}
			else {
				System.out.println(ANSI_BLUE + " ✧ Tournament end! Congrats! ✧" + ANSI_RESET);
				System.exit(0);
			}
		}
	}

	public static <T extends Tuple> String embellishMessage(Set<T> embellims, String setAbbrev, Player target) { // Slightly embellish each win message so they're not always the same.
		Set<String> filteredEmbellims = new HashSet<>();
		String targetName = (players.indexOf(target) == 0) ? ((target instanceof CPU) ? "The computer['s]" : "You[r]") : (!(players.get(0) instanceof CPU) ? "Your opponent['s]" : "The opposing computer['s]");
		assert (setAbbrev.matches("ce|he|fhe|ahe|se|te")) : "Embellim set abbreviation invalid! (" + setAbbrev + ")"; // Note: archetypeHintEmbellims and hintEmbellims are supposed to be COMBINED. Which one is called should be rolled outside of this method.

		switch (setAbbrev) {
			case "ce" -> { // choiceEmbellims (self choice). "You" or "Your opponent"
				filteredEmbellims = embellims.stream()
						.filter(e -> e.getValue(0).equals(target.choice))
						.map(e -> e.getValue(1).toString())
						.map(s -> targetName.replaceAll("\\[.*?]", "") + " " + s)
						.collect(Collectors.toSet());
			}

			case "he" -> { // hintEmbellims (oppo choice). "You" or "Your opponent"
				filteredEmbellims = embellims.stream()
						.filter(e -> e.getValue(0).equals(target.opponents.get(0).choice))
						.map(e -> e.getValue(1).toString())
						.map(s -> targetName.replaceAll("\\[.*?]", "") + " " + s)
						.collect(Collectors.toSet());
			}

			case "fhe" -> { // failedHintEmbellims (generic). "You" or "Your opponent"
				filteredEmbellims = embellims.stream()
						.map(e -> e.getValue(0).toString())
						.map(s -> targetName.replaceAll("\\[.*?]", "") + " " + s)
						.collect(Collectors.toSet());
			}

			case "ahe" -> { // archetypeHintEmbellims (oppo archetype). "You" or "Your opponent"
				CPU targetOpponent = (CPU)(target.opponents.get(0));

				filteredEmbellims = embellims.stream()
						.filter(e -> e.getValue(0).equals(targetOpponent.atype))
						.map(e -> e.getValue(1).toString())
						.map(s -> targetName.replaceAll("\\[.*?]", "") + " " + s)
						.collect(Collectors.toSet());
			}

			case "we" -> { // winEmbellim (winner's choice). "Your {CHOICE}" or "Your opponent's {CHOICE}"
				filteredEmbellims = embellims.stream()
						.filter(e -> e.getValue(0).equals(Objects.requireNonNull(determineWinner(target, target.opponents.get(0))).choice)) // DW(t, t.opponent) guaranteed not to return null (tie/invalid) b/c tie case is handled in another method.
						.map(e -> e.getValue(1).toString())
						.map(s -> targetName.replace("[", "").replace("]", "") + " " + target.choice.toString().toLowerCase() + " " + s)
						.collect(Collectors.toSet());
			}

			case "te" -> { // tieEmbellim (default to self choice). "Your {CHOICE}" or "Your opponent's {CHOICE}". targetName nulled if contains *BOTH...*
				filteredEmbellims = embellims.stream()
						.filter(e -> e.getValue(0).equals(target.choice))
						.map(e -> e.getValue(1).toString())
						.map(s -> {
							if (s.contains("*"))
								return s.charAt(1) + s.substring(2, s.lastIndexOf("*")).toLowerCase() + s.substring(s.lastIndexOf("*")+1);
							else
								return targetName.replace("[", "").replace("]", "") + " " + target.choice.toString().toLowerCase() + " " + s;
						})
						.collect(Collectors.toSet());

			}
		}

		// Possible games...
		// Human v. CPU: target = you
		// Human v. human: target = you
		// CPU v. CPU: target = your computer

		// Huh, ok.

		if (!checkIfSubset(filteredEmbellims, usedEmbellims)) { // If all filteredEmbellims have been used, clear that set from usedEmbellims.
			deleteSubset(filteredEmbellims, usedEmbellims);
		}

		String selectedEmbellim = filteredEmbellims.stream()
				.filter(e -> !usedEmbellims.contains(e))
				.toList()
				.get((int)(Math.random() * filteredEmbellims.size()));

		usedEmbellims.add(selectedEmbellim); // Mark embellim as "already used".



		for (String tsString : getSubstrings(selectedEmbellim, Pattern.compile("\\[.*?]"))) {
			int targetSensitiveSide = (players.indexOf(target) == 0) ? ((target instanceof CPU) ? 2 : 0) : 1; // Target-sensitive = [target is you|target is your oppo|target is comp's oppo] part of the embellim.
			String[] targetSensitiveString = tsString.replace("[", "").replace("]", "").split("\\|"); // Split target-sensitive string into its two halves.

			selectedEmbellim = selectedEmbellim.replaceFirst("\\[.*?]", targetSensitiveString[targetSensitiveSide]); // Apply to embellim string.
		}

		return selectedEmbellim
				.replace("{CHOICE}", String.valueOf(target.choice))
				.replace("{TOPIC}", topics.stream().toList().get((int)(Math.random() * topics.size())).getValue0());
	}

	public static void populateEmbellims() {
		// [a|b] denotes [your action | opponent action].  ----- Formatted as "You" + message, or "Your opponent" + message.

		// choiceEmbellims = selfChoice
		choiceEmbellims.add(Pair.with(choices.NONE, "striked forcefully with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "brandished a {CHOICE} from out of nowhere!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "came out swinging with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "unsheathed a tactical {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "pulled out a {CHOICE} from [your|their] pocket!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "swiped quickly with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "jerked a {CHOICE} forward!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "loafed around absentmindedly and pulled out a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "pretended not to care but suddenly threw a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "thought wistfully and conjured a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "chucked a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "came barreling forward with a {CHOICE} in hand!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "whispered something inaudible and out came a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "told [the opponent|you] about something interesting and presented a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "closed [your|their] eyes and unearthed a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "wished for a {CHOICE} and it came true!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "found the nearest {CHOICE} and flinged it!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "ordered a {CHOICE} as soon as possible!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "boldly wielded a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "feigned ignorance and used a surprise {CHOICE} attack!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "bashed [the opponent|you] with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "just missed with a {CHOICE} but tried again!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "surprised [the opponent|you] with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "tried a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "told a funny joke about {CHOICE}s!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "drew out a {CHOICE} and made a witty remark!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "stylishly swept up a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(choices.NONE, "tactfully grabbed a {CHOICE}!"));

		choiceEmbellims.add(Pair.with(choices.BOULDER, "excavated a boulder!"));
		choiceEmbellims.add(Pair.with(choices.BOULDER, "slammed a boulder onto the ground!"));
		choiceEmbellims.add(Pair.with(choices.BOULDER, "hauled a boulder over!"));
		choiceEmbellims.add(Pair.with(choices.BOULDER, "rolled a boulder in and made a big mess of dust!"));
		choiceEmbellims.add(Pair.with(choices.BOULDER, "busied [yourself|themselves] with a boulder!"));
		choiceEmbellims.add(Pair.with(choices.BOULDER, "bowled a first-try strike using a boulder!"));

		choiceEmbellims.add(Pair.with(choices.BLANKET, "threw a million blankets into the air Gatsby-style!"));
		choiceEmbellims.add(Pair.with(choices.BLANKET, "warmed [yourself|themselves|itself] with a blanket!"));
		choiceEmbellims.add(Pair.with(choices.BLANKET, "took a random blanket from [your|their|its] bed!"));
		choiceEmbellims.add(Pair.with(choices.BLANKET, "cleaned off the table with a blanket!"));
		choiceEmbellims.add(Pair.with(choices.BLANKET, "took a nap and unfurled a blanket!"));
		choiceEmbellims.add(Pair.with(choices.BLANKET, "unrolled a blanket and made a big mess of lint!"));

		choiceEmbellims.add(Pair.with(choices.BIRD, "plucked a random bird from out of the sky!"));
		choiceEmbellims.add(Pair.with(choices.BIRD, "revealed, out of a pile of feathers, a bird!"));
		choiceEmbellims.add(Pair.with(choices.BIRD, "squawked ferociously with a bird!"));
		choiceEmbellims.add(Pair.with(choices.BIRD, "flew into the air on a bird!"));
		choiceEmbellims.add(Pair.with(choices.BIRD, "happily presented a tiny bird and made a big mess of feathers everywhere!"));



		// hintEmbellims = oppoChoice
		// Note: these generic hint embellims basically guarantee a free win. They will not occur often though.
		hintEmbellims.add(Pair.with(choices.NONE, "peeked and saw [your opponent|you|the opposing computer] hiding a {CHOICE}!"));
		hintEmbellims.add(Pair.with(choices.NONE, "talked about {CHOICE}s and [your opponent|you|the opposing computer] suddenly got nervous!"));
		hintEmbellims.add(Pair.with(choices.NONE, "did a little spin and saw [your opponent|you|the opposing computer] flash a {CHOICE} before applauding!"));
		hintEmbellims.add(Pair.with(choices.NONE, "tricked [your opponent|you|its opponent] into revealing [their|your|their] {CHOICE}!"));
		hintEmbellims.add(Pair.with(choices.NONE, "saw the faint shadow of a {CHOICE} on the ground!"));

		hintEmbellims.add(Pair.with(choices.BOULDER, "overheard [your opponent|you|the opponent] whispering about the beach!"));
		hintEmbellims.add(Pair.with(choices.BOULDER, "peeked and saw [your opponent|you|the opponent] firmly gripping [their|your|their] hand."));
		hintEmbellims.add(Pair.with(choices.BOULDER, "saw [your opponent|you|the other computer] sitting with an unusually furrowed brow!"));
		hintEmbellims.add(Pair.with(choices.BOULDER, "overheard [your opponent|you|the opponent] mumbling 'That rocks!' many times!"));

		hintEmbellims.add(Pair.with(choices.BLANKET, "looked at [your opponent|you|the opposing computer] and [they|you|they] seemed unusually drowsy."));
		hintEmbellims.add(Pair.with(choices.BLANKET, "peeked and saw [your opponent|you|the opponent] with very relaxed fingers!"));
		hintEmbellims.add(Pair.with(choices.BLANKET, "overheard [your opponent|you|the opposing computer] thinking out loud about the weekend."));
		hintEmbellims.add(Pair.with(choices.BLANKET, "glanced over and saw [your opponent|you|the opponent] holding a calm expression."));

		hintEmbellims.add(Pair.with(choices.BIRD, "tried to peek over and saw [your opponent|you|the opposing computer] preparing a somewhat firm gesture."));
		hintEmbellims.add(Pair.with(choices.BIRD, "asked about lunch and [your opponent|you|its opponent] seemed to favor fried chicken."));
		hintEmbellims.add(Pair.with(choices.BIRD, "saw [your opponent|you|the opposing computer] sitting with a gentle grin and in deep thought!"));
		hintEmbellims.add(Pair.with(choices.BIRD, "overheard [your opponent|you|the opposing computer] suddenly peep like a chicken and both laughed."));



		// Failed hint embellims = generic.
		failedHintEmbellims.add(Unit.with("tried to glance over [your opponent's|your|its opponent's] shoulder but failed!"));
		failedHintEmbellims.add(Unit.with("panicked and talked about the weather instead and nervously laughed."));
		failedHintEmbellims.add(Unit.with("forgot what [you|they|it] were doing and thought about {TOPIC}."));
		failedHintEmbellims.add(Unit.with("peeked over [your opponent's|your|the opponent's] shoulder but was met with a stern glare!"));
		failedHintEmbellims.add(Unit.with("absentmindedly dozed off for just a moment."));
		failedHintEmbellims.add(Unit.with("[were|was|was] too nervous to say anything."));
		failedHintEmbellims.add(Unit.with("tried to get a hint but decided to talk about {TOPIC} instead."));
		failedHintEmbellims.add(Unit.with("[were|was|was] too preoccupied with the drizzle outside to ask something."));
		failedHintEmbellims.add(Unit.with("shifted around and attempted to say something, but it got stuck on the tip of [your|their|its] tongue."));
		failedHintEmbellims.add(Unit.with("loafed around instead!"));
		failedHintEmbellims.add(Unit.with("stubbornly decided to not try for a hint."));

		// ArchetypeHintEmbellims = oppoArchetype
		// These are special hints (and hint fails!) that hint at the archetype of the CPU. And big ol' tip: many of these are tricky -- they line up with choice hints but can be misleading!
		// Note I: archetypeHints can be called by CPUs, too (in a CPU v. CPU case)! They will then "intelligently" adapt their weights accordingly (of course, not 100% of the time -- there is a chance they won't understand the hint).
		// Note II: there are no archetypeFailHintEmbellims b/c that would give away the archetypes! These embellims below already kind of do that. Plus, I don't want to write more hints :)

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.NONE, "attempted to look for any clues from [your opponent|their opponent|its opponent] but was surprised to see nothing of note.")); // Yes, even none archetype has hints -- still an archetype!
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.NONE, "asked many questions but was disappointed to find out [your opponent|their opponent|its opponent] was simply quite ordinary."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.NONE, "probed [your opponent|their opponent|its opponent] for their interests and finally asked them out for lunch, and they simply happily agreed."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.SEAGULL, "asked about the beach and [your opponent|their opponent|the opposing computer] responded with a passionate, joyful response!"));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.SEAGULL, "squinted real hard and for a second [you|they|it] thought [you|they|it] saw [your opponent's|their opponent's|the opponent's] pockets full of seashells."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.SEAGULL, "were intrigued about [your opponent's|their opponent's|its opponent's] interests and learned about their love of the seashore."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GEOLOGIST, "peeked and saw a rock collecting pamphlet sticking out of [your opponent's|their opponent's|its opponent's] pocket."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GEOLOGIST, "saw [your opponent|their opponent] grinning in delight thinking about pebbles."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GEOLOGIST, "made a joke about rocks and [your opponent|their opponent|its opponent's] bellowed a big hearty laugh."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.INSOMNIAC, "tried to think about [your|their|its] next moves, and [your opponent|their opponent|its opponent] unexpectedly fell asleep."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.INSOMNIAC, "peered and saw that [your opponent|their opponent|its opponent] had deep bags under their eyes."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.INSOMNIAC, "peeked and noticed a blanket crudely shoved into [your opponent's|their opponent's|its opponent's] unusually deep coat pocket."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.STRATEGIST, "asked about chess boxing and [your opponent|their opponent|its opponent] lit up in excitement."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.STRATEGIST, "sat in deep thought for a moment and [your opponent|their opponent|its opponent] grinned in admiration."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.STRATEGIST, "tried to trick [your opponent|their opponent|the opponent] but they surprisingly saw through [your|their|its] paper-thin plans.")); // Seems like a fail, but it reveals strategist!

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WATCHREADER, "took a peek and saw [your opponent|their opponent|its opponent] impatiently and anxiously glancing at their watch a little too much."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WATCHREADER, "wanted to say something, but [your opponent|their opponent|its opponent] kept looking outside the window at the billowing clouds."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WATCHREADER, "brought up the subject of watches out of the blue. [Your opponent|Their opponent|The opponent] was mightily impressed!"));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.AMATEUR, "saw [your opponent|their opponent|its opponent] nervously readjust their dress collar repeatedly."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.AMATEUR, "had to hesitantly remind [your opponent|their opponent|the opponent] the rules of the game for the third time in a row."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.AMATEUR, "glanced at [your opponent|their opponent|its opponent] closely. They seemed to either be in very deep thought or absolute confused bewilderment."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.PROGRAMMER, "peeked and saw [your opponent|their opponent|its opponent] busy clacking away on a tiny little laptop."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.PROGRAMMER, "asked [your opponent|their opponent|its opponent] about their day and was met with a flurry of lingo [you|they|it] didn't understand."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.PROGRAMMER, "peered over and thought [you|they|it] saw eight StackOverflow tabs open on [your opponent's|their opponent's|the opponent's] computer."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WIKIAN, "took a peek and saw [your opponent|their opponent|the opponent] quietly scrolling through something on their phone."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WIKIAN, "wanted to say something but was interrupted by [your opponent|their opponent|the opponent] reciting something to themselves out loud."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WIKIAN, "remarked something about wikis casually and [your opponent|their opponent|its opponent] gulped a nervous breath."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GRANDMASTER, "tried to smartly snag a hint from [your opponent|their opponent|its opponent] but they deftly evaded all [your|their|its] attempts.")); // Remember: seems like a fail, yet reveals archetype!
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GRANDMASTER, "peered over and noticed [your opponent|their opponent|the opponent] coolly waiting with a serious but warm smile."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GRANDMASTER, "thought for a split second that [your opponent|their opponent|its opponent] looked familiar, but brushed the thought aside.")); // e.g. famous RPS player, seen on the interwebs!


		// stateEmbellim = winner's choice
		// stateEmbellim = P1's winning choice. ----  Formatted as "Your {CHOICE}" + message!
		winEmbellims.add(Pair.with(choices.BOULDER, "took down [the opponent's|your|the opponent's] bird in a single blow!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "crushed the bird in a split second!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "rolled around and somehow ended up with a pile of feathers!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "was pecked by the opposing bird, and pecked right back!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "challenged the opposing bird to a duel and they quarrelled! (The boulder emerged victorious.)"));
		winEmbellims.add(Pair.with(choices.BOULDER, "lectured the opposing bird on the dangers of geology and it flew off immediately!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "grabbed a handful of worms and tricked the opposing bird!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "bowled over the opposing bird. Strike!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "made the ground tremble and quake. The bird was shaken to its core!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "lovingly made the opposing bird an earthworm sandwich. The bird yielded!"));
		winEmbellims.add(Pair.with(choices.BOULDER, "did nothing... and the opposing bird was crushed by sheer boredom!"));

		winEmbellims.add(Pair.with(choices.BLANKET, "covered the boulder in a veil of crushing darkness!"));
		winEmbellims.add(Pair.with(choices.BLANKET, "whispered something aggressively. The opposing boulder didn't have the guts and promptly rolled off!"));
		winEmbellims.add(Pair.with(choices.BLANKET, "feigned sleep and struck back at the perfect opportunity!"));
		winEmbellims.add(Pair.with(choices.BLANKET, "lied flat on the ground and made the opposing boulder slip!"));
		winEmbellims.add(Pair.with(choices.BLANKET, "made the opposing boulder fall asleep!"));
		winEmbellims.add(Pair.with(choices.BLANKET, "folded itself into a blade and split the boulder!"));
		winEmbellims.add(Pair.with(choices.BLANKET, "fell into a deep sleep and destroyed the boulder in its dream."));
		winEmbellims.add(Pair.with(choices.BLANKET, "recited one of George Orwell's hyper-dystopian novels and bewildered the boulder!"));
		winEmbellims.add(Pair.with(choices.BLANKET, "scared the boulder off with an aggressive tax report!"));
		winEmbellims.add(Pair.with(choices.BLANKET, "made something spin around! The boulder was mightily impressed."));
		winEmbellims.add(Pair.with(choices.BLANKET, "flattened the opposing boulder into a new-age hippie rug."));

		winEmbellims.add(Pair.with(choices.BIRD, "called for help! A flock of geese rumbled by and tore through the blanket."));
		winEmbellims.add(Pair.with(choices.BIRD, "squawked something mean! The opposing blanket was reduced to tears."));
		winEmbellims.add(Pair.with(choices.BIRD, "drilled a bunch of scathing holes into the opposing blanket!"));
		winEmbellims.add(Pair.with(choices.BIRD, "publicly humiliated the blanket on the Internet."));
		winEmbellims.add(Pair.with(choices.BIRD, "flew around and pecked at the opposing blanket."));
		winEmbellims.add(Pair.with(choices.BIRD, "used a little pocket magic. Presto! The blanket magically became a pile of birdseed."));
		winEmbellims.add(Pair.with(choices.BIRD, "called its mother for some advice. The blanket passed out from sheer surprise!"));
		winEmbellims.add(Pair.with(choices.BIRD, "ordered a hot slice from Mach Pizza and the blanket died from jealousy!"));
		winEmbellims.add(Pair.with(choices.BIRD, "ruffled its wings and spread a bunch of feathers onto the unfortuante opposing blanket!"));
		winEmbellims.add(Pair.with(choices.BIRD, "sang a beautiful melody and lulled the blanket to sleep."));
		winEmbellims.add(Pair.with(choices.BIRD, "flapped its wings, screeched, and scratched with sharp claws!"));
		winEmbellims.add(Pair.with(choices.BIRD, "endearingly gifted the blanket some soft feathers. It was mightily impressed!"));


		// Reverse psych embellims = self choice



		// Tie embellims = self choice?
		tieEmbellims.add(Pair.with(choices.BIRD, "and [your opponent's|your|the opponent's] bird crashed right into each other!"));
		tieEmbellims.add(Pair.with(choices.BIRD, "tapped [the opponent's|your|its opponent's] bird lightly and it lovingly tapped right back! Awww."));
		tieEmbellims.add(Pair.with(choices.BIRD, "flew into the other bird. Both flustered, they apologized profusely to each other!"));
		tieEmbellims.add(Pair.with(choices.BIRD, "whispered to the other bird, grinned, and both flew together off into the joyous, radiant sunset."));
		tieEmbellims.add(Pair.with(choices.BIRD, "invited [the opponent's|your|the opponent's] bird to a cracker party. Friendship!"));
		tieEmbellims.add(Pair.with(choices.BIRD, "aimed right at the opposing bird... and narrowly missed!"));
		tieEmbellims.add(Pair.with(choices.BIRD, "*BOTH BIRDS* bellowed a scathing war cry... just missed!"));

		tieEmbellims.add(Pair.with(choices.BOULDER, "*BOTH BOULDERS* tumbled into each other and shattered into a showering of rocks!"));
		tieEmbellims.add(Pair.with(choices.BOULDER, "and [the opponent's|your|the opponent's] boulder decided to become one with the earth and stopped rolling."));
		tieEmbellims.add(Pair.with(choices.BOULDER, "took the opposing boulder out to the local bowling alley for a ten-pin showdown!"));
		tieEmbellims.add(Pair.with(choices.BOULDER, "and [the opponent's|your|its opponent's] boulder charged forward and they rolled right past!"));
		tieEmbellims.add(Pair.with(choices.BOULDER, "found the other boulder and opted for the prosperity of unequivocal peace over violence."));
		tieEmbellims.add(Pair.with(choices.BOULDER, "*BOTH BOULDERS* tumbled forward but accidentally rolled into the '90s! Totally tubular, my dude."));

		tieEmbellims.add(Pair.with(choices.BLANKET, "and the opposing blanket snugly drifted right into bed and took a nap!"));
		tieEmbellims.add(Pair.with(choices.BLANKET, "openly expressed its concerns over today's socioeconomic woes to the other and loftly made peace."));
		tieEmbellims.add(Pair.with(choices.BLANKET, "*BOTH BLANKETS* did a lil' Irish jig. Thunderous applause!!"));
		tieEmbellims.add(Pair.with(choices.BLANKET, "blanketed the opposing blanket. Absolutely riveting."));
		tieEmbellims.add(Pair.with(choices.BLANKET, "noticed the incoming blanket and humbly apologized for the misunderstanding. They then lived out their hearty lives forever as friends. Aww."));
		tieEmbellims.add(Pair.with(choices.BLANKET, "tried to strike at [the opponent's|your|the opponent's] blanket, but the wind promptly swept it over yonder!"));


		// Topics
		topics.add(Unit.with("the weekend"));
		topics.add(Unit.with("the weather"));
		topics.add(Unit.with("dinner"));
		topics.add(Unit.with("colors"));
		topics.add(Unit.with("vehicles"));
		topics.add(Unit.with("strategy games"));
		topics.add(Unit.with("something random"));
		topics.add(Unit.with("what time it was"));
		topics.add(Unit.with("garlic bread"));
		topics.add(Unit.with("whether or not pineapple goes on pizza"));
		topics.add(Unit.with("politics"));
		topics.add(Unit.with("nothing in particular"));
		topics.add(Unit.with("winter break"));
		topics.add(Unit.with("today's society's sociopolitical catastrophe"));
		topics.add(Unit.with("the future of mankind"));
		topics.add(Unit.with("the significance of time"));
		topics.add(Unit.with("a video game you liked in your childhood"));
		topics.add(Unit.with("flowers"));
		topics.add(Unit.with("the nugget-shaped cloud you saw yesterday"));
		topics.add(Unit.with("your favorite food"));
		topics.add(Unit.with("a story you'd been wanting to write"));
		topics.add(Unit.with("the wonders of the world"));
		topics.add(Unit.with("the scrumptious fragrance of rice"));
		topics.add(Unit.with("some of your favourite fonts"));
		topics.add(Unit.with("the wistful memories of yesteryear"));
		topics.add(Unit.with("intelligent drum/bass, and the rise of boom bap instrumentals"));
		topics.add(Unit.with("freeform jazz"));
		topics.add(Unit.with("the soulful whistling of the wind"));
		topics.add(Unit.with("the first thing you thought of"));
		topics.add(Unit.with("a potential answer to the infamous three-body problem"));
		topics.add(Unit.with("how much rice you've eaten in the past year"));
	}

	public static int[] determineOrder(List<Player> players) {
		Integer[] order = new Integer[players.size()];
		int initialIndex = (int)(Math.random() * players.size());

		for (int i = 0; i < players.size(); i++) {
			order[i] = initialIndex + i > players.size() - 1 ? 0 : initialIndex + i; // Wrap index to 0 if it's beyond list length (e.g. list = 3, so index of 3 should wrap to 0)
		}

		System.out.println(ANSI_YELLOW + "(+) Player order is " + grammaticParse(Arrays.stream(order).map(o -> o + 1).map(o -> "P" + o).toArray(), "then") + ".\n\n\n" + ANSI_RESET);

		return ArrayUtils.toPrimitive(order);
	}

	public static Player determineWinner(Player playerA, Player playerB) {
		if (playerA.choice.equals(playerB.choice)) { // Eliminate tie scenario
			playerA.gameState = gameStates.TIE;
			playerB.gameState = gameStates.TIE;
			return null;
		}

		if (playerA.choice.ordinal() == 0 || playerB.choice.ordinal() == 0) {
			playerA.gameState = gameStates.INVALID;
			playerB.gameState = gameStates.INVALID;
			return null;
		}
		/* Boolean operators version */
		// boulder 1 blanket 2 bird 3
		// 3 2, 3 1, 2 1, 2 3, 1 2, 1 3
		// 1,   2,   1,   -1,  -1,  -2
		// win, lose, win, lose, lose, WIN (outlier)
		// win = 1, -2 **** lose = -1, 2

		if (playerA.choice.ordinal() - playerB.choice.ordinal() == 1 || playerA.choice.ordinal() - playerB.choice.ordinal() == -2) {
			playerA.gameState = gameStates.WIN;
			playerB.gameState = gameStates.LOSE;
			return playerA;
		}
		else {
			playerA.gameState = gameStates.LOSE;
			playerB.gameState = gameStates.WIN;
			return playerB;
		}
	}



	public static String prompt(String message, String errorMessage, String[] bounds, boolean lineMode, boolean isCaseSensitive) // <+> APM
	{
		String nextInput;

		while (true)
		{
			System.out.print(message);
			if (!message.equals(""))
				System.out.println();

			if (lineMode) {
				input.nextLine();
				nextInput = input.nextLine();
			}
			else {
				nextInput = input.next();
			}

			if (!isCaseSensitive)
			{
				nextInput = nextInput.toLowerCase();

				for (int i = 0; i < bounds.length; i++)
					bounds[i] = bounds[i].toLowerCase();
			}

			if (nextInput.matches(String.join("|", bounds)) || bounds[0].equals("")) {
				return nextInput;
			} else {
				System.out.println(ANSI_RED + errorMessage + ANSI_RESET);
			}

		}
	}

	public static double prompt(String message, String errorMessage, double min, double max, boolean isIntegerMode)
	{
		String nextInput;
		double parsedInput = 0;
		boolean isValid;

		while (true) {
			System.out.print(message);
			if (!message.equals(""))
				System.out.println();

			nextInput = input.next();
			try {

				if (!isIntegerMode) {
					parsedInput = Double.parseDouble(nextInput);
				} else {
					parsedInput = Integer.parseInt(nextInput);
				}

				input.nextLine();
				isValid = true;
			} catch (Exception e) {
				isValid = false;
			}

			if (parsedInput >= min && parsedInput <= max && isValid) {
				return parsedInput;
			} else {
				System.out.println(ANSI_RED + errorMessage + ANSI_RESET);
			}
		}
	}

	public static void fancyDelay(long delay, String loadMessage, String completionMessage, int iterations) throws InterruptedException { // Yoinked from SchudawgCannoneer
		int recursionCount = 0;
		System.out.print(loadMessage + " /");

		while (recursionCount < iterations) {
			TimeUnit.MILLISECONDS.sleep(delay);
			System.out.print("\b\u2014");
			TimeUnit.MILLISECONDS.sleep(delay);
			System.out.print("\b\\");
			TimeUnit.MILLISECONDS.sleep(delay);
			System.out.print("\b|");
			TimeUnit.MILLISECONDS.sleep(delay);
			System.out.print("\b/");
			recursionCount++;
		}
		System.out.print("\b\n" + completionMessage + "\n" + ANSI_RESET);
	}

	public static <T> List<T> reorderList(List<T> unorderedList, int[] order) { // <+> APM
		List<T> newList = new ArrayList<>();

		for (int index : order)
			newList.add(unorderedList.get(index));

		return newList;
	}

	public static <T> String grammaticParse(T[] array, String conjunction) { // Yoinked from Digitridoo
		StringBuilder gParsedString = new StringBuilder(Arrays.toString(array));

		gParsedString.deleteCharAt(0)
				.deleteCharAt(gParsedString.length()-1)
				.insert(gParsedString.lastIndexOf(",") + 1, " " + conjunction);

		return gParsedString.toString();
	}

	public static <T> String[] toStringArray(T[] array) { // <+> APM
		String[] newArray = new String[array.length];

		for (int i = 0; i < array.length; i++) {
			newArray[i] = array[i].toString();
		}

		return newArray;
	}




	public static int numOfStringContent(String string, Pattern pattern) { // <+> APM
		int count = 0;
		Matcher matcher = pattern.matcher(string);

		while (matcher.find()) {
			count++;
		}

		return count;
	}

	public static String[] getSubstrings(String string, Pattern pattern) { // <+> APM
		List<String> substrings = new ArrayList<>();
		String editedString = string;
		Matcher matcher = pattern.matcher(editedString);

		for (int i = 0; i < numOfStringContent(string, pattern); i++) {
			matcher.find();
			substrings.add(matcher.group(0));
			editedString = editedString.replaceFirst(String.valueOf(pattern), "");
		}

		return substrings.toArray(new String[0]);
	}

	public static <T extends Collection<E>, E> boolean checkIfSubset(T subList, T mainList) { // <+> APM
		for (E item : subList) {
			if (!(mainList.contains(item)))
				return false;
		}

		return true;
	}

	public static <T extends Collection<E>, E> void deleteSubset(T subList, T mainList) { // <+> APM
		for (E item : subList) {
			mainList.remove(item);
		}
	}
}



