//(c) A+ Computer Science
// www.apluscompsci.com
//Name - Lucas Xie

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.javatuples.Pair;
import org.apache.commons.lang3.ArrayUtils;

public class BrpsGame
{
	private int remainingGames, totalGames;
	private int[] setScore; // Useful for maybe "loading" mid-game for replay.
	brpsStates gameState;


	public static Scanner input = new Scanner(System.in);

	// MESSAGE EMBELLIMS
	public static Set<String> topics;
	public static Set<Pair<brpsChoices, String>> choiceEmbellims;
	public static Set<Pair<brpsChoices, String>> hintEmbellims;
	public static Set<Pair<CPU.archetype, String>> archetypeHintEmbellims;
	public static Set<String> failedHintEmbellims;
	public static Set<Pair<brpsChoices, String>> stateEmbellims;


	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";

	enum brpsChoices {
		NONE,
		BOULDER,
		BLANKET,
		BIRD,
	}

	enum brpsStates {
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

		BrpsGame bGame = new BrpsGame( (int) prompt("How many rounds to play? (max 15 rounds)", "Error: invalid number.", 1, 15, true), new int[]{0,0});
		int totalPlayerCount = (int) prompt("How many players will be playing (both CPU or human)?", "Error: invalid number. Clamped to 2 temporarily.", 2, 2, true);
		int humanPlayerCount = (int) prompt("How many human players?", "Error: invalid number.", 0, 2, true);
		int cpuPlayerCount = totalPlayerCount - humanPlayerCount;

		for (int i = 0; i < humanPlayerCount; i++) {
			players.add(new Player());
		}

		for (int i = 0; i < cpuPlayerCount; i++) {
			players.add(new CPU());
		}



		while (true) {
			if (bGame.remainingGames > 0) {
				//players = reorderList(players, determineOrder(players));
				playerOrder = determineOrder(players);

				for (int pIndex : playerOrder) {
					Player player = players.get(pIndex);

					if (player instanceof CPU) { // CPU
						System.out.println(ANSI_BLUE + "It is now Player " + players.indexOf(player) + "'s turn (CPU)." + ANSI_RESET);
						fancyDelay(500);
						player.choice = ((CPU) player).archetypedChoice(); // (CPU)player ensures player is type CPU.
						System.out.println(ANSI_PURPLE + "\nP" + players.indexOf(player) + " finished choosing hand.\n\n" + ANSI_RESET);
					}

					else { // Human
						try {
							players.get(players.indexOf(player) + 1);

							if (players.get(players.indexOf(player) + 1) instanceof CPU) { // There is an index ahead AND that next index is a CPU.
								switch (prompt("You have a chance to peek at your opponent's move. Want to try?", "Error: pick yes or no.", new String[]{"yes", "no"}, false, false)) {
									case "yes" -> {
										if (Math.random() < ((CPU) players.get(players.indexOf(player) + 1)).hintRate) {
											System.out.println(embellishMessage(hintEmbellims));
											System.out.println("success\n\n"); // debug
										} else {
											System.out.println(embellishMessage(failedHintEmbellims));
											System.out.println("fail\n\n"); // debug
										}
									}

									case "no" -> {}
								}
							}
						} catch (Exception ignored) {};


						System.out.println(ANSI_BLUE + "It is now Player " + players.indexOf(player) + "'s turn (Human)." + ANSI_RESET);
						player.retrieveChoice(players.indexOf(player));
						System.out.println(ANSI_PURPLE + "P" + players.indexOf(player) + " finished choosing hand.\n\n" + ANSI_RESET);
					}
				}

				TimeUnit.MILLISECONDS.sleep(1200);
				System.out.println("Three...");
				TimeUnit.MILLISECONDS.sleep(1200);
				System.out.println("Two...");
				TimeUnit.MILLISECONDS.sleep(1200);
				System.out.println("One...");
				TimeUnit.MILLISECONDS.sleep(1200);
				System.out.println("BOULDER, BLANKET, BIRD!\n");

				System.out.println(embellishMessage(choiceEmbellims, players.get(0).choice));
				bGame.determineWinner(players.get(0).choice, players.get(1).choice);

				System.out.println(players.get(0).choice + "(A) " + players.get(1).choice + "(B) " + bGame.gameState); // debug line

				System.out.println(embellishMessage(stateEmbellims, players.get(0).choice));

				for (Player player : players) { // Update individual stats
					player.updateStats(bGame.gameState);
				}

				bGame.remainingGames--;
				System.out.println("Game " + (bGame.totalGames - bGame.remainingGames) + " finished!");

			}
			else {
				System.out.println("Tournament end! Congrats!");
				System.exit(0);
			}
		}
	}

	public static <T> String embellishMessage(Set<T> embellims, String type) { // Slightly embellish each win message so they're not always the same.
		return ANSI_YELLOW + "embellishMessage not implemented fixnow okthanks :)" + ANSI_RESET;
	}

	public static void populateEmbellims() {
		// [a|b] denotes [your action | opponent action].  ----- Formatted as "You" + message, or "Your opponent" + message.
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "striked forcefully with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "brandished a {CHOICE} from out of nowhere!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "came out swinging with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "unsheathed a tactical {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "pulled out a {CHOICE} from [your|their] pocket!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "swiped quickly with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "jerked a {CHOICE} forward!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "loafed around absentmindedly and pulled out a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "pretended not to care but suddenly threw a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "thought wistfully and conjured a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "chucked a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "came barreling forward with a {CHOICE} in hand!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "whispered something inaudible and out came a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "told [the opponent|you] about something interesting and presented a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "closed [your|their] eyes and unearthed a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "wished for a {CHOICE} and it came true!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "found the nearest {CHOICE} and flinged it!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "ordered a {CHOICE} as soon as possible!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "boldly wielded a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "feigned ignorance and used a surprise {CHOICE} attack!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "bashed [the opponent|you] with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "just missed with a {CHOICE} but tried again!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "surprised [the opponent|you] with a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "tried a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "told a funny joke about {CHOICE}s!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "drew out a {CHOICE} and made a witty remark!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "stylishly swept up a {CHOICE}!"));
		choiceEmbellims.add(Pair.with(brpsChoices.NONE, "tactfully grabbed a {CHOICE}!"));

		choiceEmbellims.add(Pair.with(brpsChoices.BOULDER, "excavated a boulder!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BOULDER, "slammed a boulder onto the ground!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BOULDER, "hauled a boulder over!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BOULDER, "rolled a boulder in and made a big mess of dust!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BOULDER, "busied [yourself|themselves] with a boulder!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BOULDER, "bowled a first-try strike using a boulder!"));

		choiceEmbellims.add(Pair.with(brpsChoices.BLANKET, "threw a million blankets into the air Gatsby-style!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BLANKET, "warmed [yourself|themselves] with a blanket!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BLANKET, "took a blanket from your bed!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BLANKET, "cleaned off the table with a blanket!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BLANKET, "took a nap and unfurled a blanket!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BLANKET, "unrolled a blanket and made a big mess of lint!"));

		choiceEmbellims.add(Pair.with(brpsChoices.BIRD, "plucked a random bird from out of the sky!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BIRD, "revealed, out of a pile of feathers, a bird!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BIRD, "squawked ferociously with a bird!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BIRD, "flew into the air on a bird!"));
		choiceEmbellims.add(Pair.with(brpsChoices.BIRD, "happily presented a bird and made a big mess of feathers everywhere!"));




		// Note: these generic hint embellims basically guarantee a free win. They will not occur often though.
		hintEmbellims.add(Pair.with(brpsChoices.NONE, "peeked and saw [your opponent|you] hiding a {CHOICE}!"));
		hintEmbellims.add(Pair.with(brpsChoices.NONE, "talked about {CHOICE}s and [your opponent|you] suddenly got nervous!"));
		hintEmbellims.add(Pair.with(brpsChoices.NONE, "did a little spin and saw [your opponent|you] flash a {CHOICE} before applauding!"));
		hintEmbellims.add(Pair.with(brpsChoices.NONE, "tricked [your opponent|you] into revealing {their|your} {CHOICE}!"));
		hintEmbellims.add(Pair.with(brpsChoices.NONE, "saw the faint shadow of a {CHOICE} on the ground!"));

		hintEmbellims.add(Pair.with(brpsChoices.BOULDER, "overheard [your opponent|you] whispering about the beach!"));
		hintEmbellims.add(Pair.with(brpsChoices.BOULDER, "peeked and saw [your opponent|you] firmly gripping [their|your] hand."));
		hintEmbellims.add(Pair.with(brpsChoices.BOULDER, "saw [your opponent|you] sitting with an unusually furrowed brow!"));
		hintEmbellims.add(Pair.with(brpsChoices.BOULDER, "overheard [your opponent|you] mumbling 'That rocks!' many times!"));

		hintEmbellims.add(Pair.with(brpsChoices.BLANKET, "looked at [your opponent|you] and [they|you] seemed unusually drowsy."));
		hintEmbellims.add(Pair.with(brpsChoices.BLANKET, "peeked and saw [your opponent|you] with very relaxed fingers!"));
		hintEmbellims.add(Pair.with(brpsChoices.BLANKET, "overheard [your opponent|you] thinking out loud about the weekend."));
		hintEmbellims.add(Pair.with(brpsChoices.BLANKET, "glanced over and saw [your opponent|you] holding a calm expression."));

		hintEmbellims.add(Pair.with(brpsChoices.BIRD, "tried to peek over and saw [your opponent|you] preparing a somewhat firm gesture."));
		hintEmbellims.add(Pair.with(brpsChoices.BIRD, "asked about lunch and [your opponent|you] seemed to favor fried chicken."));
		hintEmbellims.add(Pair.with(brpsChoices.BIRD, "saw [your opponent|you] sitting with a gentle grin and in deep thought!"));
		hintEmbellims.add(Pair.with(brpsChoices.BIRD, "overheard [your opponent|you] suddenly peep like a chicken and both laughed."));



		// Failed hint embellims are all generic.
		failedHintEmbellims.add("tried to glance over [your opponent's|your] shoulder but failed!");
		failedHintEmbellims.add("panicked and talked about the weather instead and nervously laughed.");
		failedHintEmbellims.add("forgot what [you|they] were doing and thought about {TOPIC}.");
		failedHintEmbellims.add("peeked over [your opponent's|your] shoulder but was met with a stern glare!");
		failedHintEmbellims.add("absentmindedly dozed off for just a moment.");
		failedHintEmbellims.add("[were|was] too nervous to say anything.");
		failedHintEmbellims.add("tried to get a hint but decided to talk about {TOPIC} instead.");
		failedHintEmbellims.add("[were|was] too preoccupied with the drizzle outside to ask something.");
		failedHintEmbellims.add("shifted around and attempted to say something, but it got stuck on the tip of [your|their] tongue.");
		failedHintEmbellims.add("loafed around instead!");
		failedHintEmbellims.add("stubbornly decided to not try for a hint.");

		// ...except for archetypes. These are special hints (and hint fails!) that hint at the archetype of the CPU. And big ol' tip: many of these are tricky -- they line up with choice hints but can be misleading!
		// Note I: archetypeHints can be called by CPUs, too (in a CPU v. CPU case)! They will then "intelligently" adapt their weights accordingly (of course, not 100% of the time -- there is a chance they won't understand the hint).
		// Note II: there are no archetypeFailHintEmbellims b/c that would give away the archetypes! These embellims below already kind of do that. Plus, I don't want to write more hints :)

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.NONE, "attempted to look for any clues from [your opponent|their opponent] but were surprised to see nothing of note.")); // Yes, even none archetype has hints -- still an archetype!
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.NONE, "asked many questions but were disappointed to find out[your opponent|their opponent] was simply quite ordinary."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.NONE, "probed [your opponent|their opponent] for their interests and finally asked them out for lunch, and they simply happily agreed."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.SEAGULL, "asked about the beach and [your opponent|their opponent] responded with a passionate, joyful response!"));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.SEAGULL, "squinted real hard and for a second [you|they] thought [you|they] saw [your opponent's|their opponent's] pockets full of seashells."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.SEAGULL, "were intrigued about [your opponent's|their opponent's] interests and learned about their love of the seashore."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GEOLOGIST, "peeked and saw a rock collecting pamphlet sticking out of [your opponent's|their opponent's] pocket."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GEOLOGIST, "saw [your opponent|their opponent] grinning in delight thinking about pebbles."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GEOLOGIST, "made a joke about rocks and [your opponent|their opponent] bellowed a big hearty laugh."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.INSOMNIAC, "tried to think about [your|their] next moves, and [your opponent|their opponent] unexpectedly fell asleep."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.INSOMNIAC, "peered and saw that [your opponent|their opponent] had deep bags under their eyes."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.INSOMNIAC, "peeked and noticed a blanket crudely shoved into [your opponent's|their opponent's] unusually deep coat pocket."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.STRATEGIST, "asked about chess boxing and [your opponent|their opponent] lit up in excitement."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.STRATEGIST, "sat in deep thought for a moment and [your opponent|their opponent] grinned in admiration."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.STRATEGIST, "tried to trick [your opponent|their opponent] but they surprisingly saw through [your|their] paper-thin plans.")); // Seems like a fail, but it reveals strategist!

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WATCHREADER, "took a peek and saw [your opponent|their opponent] impatiently and anxiously glancing at their watch a little too much."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WATCHREADER, "wanted to say something, but [your opponent|their opponent] kept looking outside the window at the billowing clouds."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WATCHREADER, "brought up the subject of watches out of the blue. [Your opponent|Their opponent] was mightily impressed!"));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.AMATEUR, "saw [your opponent|their opponent] nervously readjust their dress collar repeatedly."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.AMATEUR, "had to hesitantly remind [your opponent|their opponent] the rules of the game for the third time in a row."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.AMATEUR, "glanced at [your opponent|their opponent] closely. They seemed to either be in very deep thought or absolute confused bewilderment."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.PROGRAMMER, "peeked and saw [your opponent|their opponent] busy clacking away on a tiny little laptop."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.PROGRAMMER, "asked [your opponent|their opponent] about their day and was met with a flurry of lingo [you|they] didn't understand."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.PROGRAMMER, "peered over and thought [you|they] saw eight StackOverflow tabs open on [your opponent's|their opponent's] computer."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WIKIAN, "took a peek and saw [your opponent|their opponent] quietly scrolling through something on their phone."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WIKIAN, "wanted to say something but was interrupted by [your opponent|their opponent] reciting something to themselves out loud."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.WIKIAN, "remarked something about wikis casually and [your opponent|their opponent] gulped a nervous breath."));

		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GRANDMASTER, "tried to smartly snag a hint from [your opponent|their opponent] but they deftly evaded all [your|their] attempts.")); // Remember: seems like a fail, yet reveals archetype!
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GRANDMASTER, "peered over and noticed [your opponent|their opponent] coolly waiting with a serious but warm smile."));
		archetypeHintEmbellims.add(Pair.with(CPU.archetype.GRANDMASTER, "thought for a split second that [your opponent|their opponent] looked familiar, but brushed the thought aside.")); // e.g. famous RPS player, seen on the interwebs!


		// stateEmbellim = P1's winning choice. ----  Formatted as "Your {CHOICE}" + message!
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "took down [the opponent's|your] bird in a single blow!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "crushed the bird in a split second!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "rolled around and somehow ended up with a pile of feathers!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "was pecked by the opposing bird, and pecked right back!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "invited the opposing bird for a party and goofed off! (The boulder emerged victorious.)"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "lectured the opposing bird on the dangers of geology and it flew off immediately!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "grabbed a handful of worms and tricked the opposing bird!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "bowled over the opposing bird. Strike!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "made the ground tremble and quake. The bird was shaken to its core!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "lovingly made the opposing bird an earthworm sandwich. The bird yielded!"));
		stateEmbellims.add(Pair.with(brpsChoices.BOULDER, "did nothing... and the opposing bird was crushed by sheer boredom!"));

		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "covered the boulder in a veil of crushing darkness!"));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "whispered something aggressively. The opposing boulder didn't have the guts and promptly rolled off!"));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "feigned sleep and struck back at the perfect opportunity!"));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "lied flat on the ground and made the opposing boulder slip!"));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "made the opposing boulder fall asleep!"));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "folded itself into a blade and split the boulder!"));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "fell into a deep sleep and destroyed the boulder in its dream."));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "recited one of George Orwell's hyper-dystopian novels and bewildered the boulder!"));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "scared the boulder off with an aggressive tax report!"));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "made something spin around! The boulder was mightily impressed."));
		stateEmbellims.add(Pair.with(brpsChoices.BLANKET, "flattened the opposing boulder into a new-age hippie rug."));

		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "called for help! A flock of geese rumbled by and teared through the blanket."));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "squawked something mean! The opposing blanket was reduced to tears."));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "drilled a bunch of scathing holes into the opposing blanket!"));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "publicly humiliated the blanket on the Internet."));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "flew around and pecked at the opposing blanket."));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "used a little pocket magic. Presto! The blanket magically became a pile of birdseed."));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "called its mother for some advice. The blanket passed out from sheer surprise!"));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "ordered a hot slice from Mach Pizza and the blanket died from jealousy!"));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "ruffled its wings and spread a bunch of feathers onto the unfortuante opposing blanket!"));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "sang a beautiful melody and lulled the blanket to sleep."));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "flapped its wings, screeched, and scratched with sharp claws!"));
		stateEmbellims.add(Pair.with(brpsChoices.BIRD, "endearingly gifted the blanket some soft feathers. It was mightily impressed!"));
	}

	public static int[] determineOrder(List<Player> players) {
		Integer[] order = new Integer[players.size()];
		int initialIndex = (int)(Math.random() * players.size());

		for (int i = 0; i < players.size(); i++) {
			order[i] = initialIndex + i > players.size() - 1 ? 0 : initialIndex + i; // Wrap index to 0 if it's beyond list length (e.g. list = 3, so index of 3 should wrap to 0)
		}

		System.out.println(ANSI_YELLOW + "Player order is " + grammaticParse(order, "then") + ".\n\n\n" + ANSI_RESET);

		return ArrayUtils.toPrimitive(order);
	}

	public void determineWinner(brpsChoices primaryChoice, brpsChoices comparedChoice) {
		if (primaryChoice.equals(comparedChoice)) {// Eliminate tie scenario
			gameState = brpsStates.TIE;
			return;
		}

		if (primaryChoice.ordinal() == 0 || comparedChoice.ordinal() == 0) {
			gameState = brpsStates.INVALID;
			return;
		}

		/* Boolean operators version */
		// boulder 1 blanket 2 bird 3
		// 3 2, 3 1, 2 1, 2 3, 1 2, 1 3
		// 1,   2,   1,   -1,  -1,  -2
		// win, lose, win, lose, lose, WIN (outlier)
		// win = 1, -2 **** lose = -1, 2

		if (primaryChoice.ordinal() - comparedChoice.ordinal() == 1 || primaryChoice.ordinal() - comparedChoice.ordinal() == -2) {
			gameState = brpsStates.WIN;
		}
		else
			gameState = brpsStates.LOSE;
	}



	public static String prompt(String message, String errorMessage, String[] bounds, boolean lineMode, boolean isCaseSensitive) // <+> APM
	{
		String nextInput;

		while (true)
		{
			if (!(message.matches("NO_MESSAGE")))
			{
				System.out.println(message);
			}

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

	public static void fancyDelay(long delay) throws InterruptedException { // Yoinked from SchudawgCannoneer
		int recursionCount = 0;
		System.out.print(ANSI_CYAN + "Thinking... /");

		while (recursionCount < 2) {
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
		System.out.print("\b\nDone!" + ANSI_RESET);
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
}



