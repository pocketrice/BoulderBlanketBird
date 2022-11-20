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
	private int[] setScore;
	brpsStates gameState;


	public static Scanner input = new Scanner(System.in);
	public static Dictionary<brpsChoices, String> choiceEmbellims;
	public static Dictionary<brpsChoices, String> hintEmbellims;
	public static Dictionary<brpsChoices, String> failedHintEmbellims;
	public static Dictionary<Pair<brpsStates, brpsChoices>, String> stateEmbellims;


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

				System.out.println(embellishMessage(choiceEmbellims));
				bGame.determineWinner(players.get(0).choice, players.get(1).choice);

				System.out.println(players.get(0).choice + "(A) " + players.get(1).choice + "(B) " + bGame.gameState); // debug line

				System.out.println(embellishMessage(stateEmbellims));

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

	public static <T,K> String embellishMessage(Dictionary<T,K> embellims) { // Slightly embellish each win message so they're not always the same. Hard-coded embellishments. Can be trimmed using switch-case!
		return ANSI_YELLOW + "embellishMessage not implemented fixnow okthanks :)" + ANSI_RESET;
	}

	public void populateEmbellims() {

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

/* Switch-case version
		switch (primaryChoice) {
			case BOULDER -> {
				if (comparedChoice.name().equals("BLANKET")) // boulder < blanket
					return brpsStates.LOSE;
				else // boulder > bird
					return brpsStates.WIN;
			}

			case BLANKET -> {
				if (comparedChoice.name().equals("BIRD")) // blanket < bird
					return brpsStates.LOSE;
				else // blanket > boulder
					return brpsStates.WIN;
			}

			case BIRD -> {
				if (comparedChoice.name().equals("BOULDER")) // bird < boulder
					return brpsStates.LOSE;
				else // bird > blanket
					return brpsStates.WIN;
			}
		}*/
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



