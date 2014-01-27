import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;

import tokens.TwitterTokenRefresher;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class Tester {
	/* Always make sure to have a tokens.xml. This file should contain as many tokens as you feel are necessary for the use of this program. 
	 * This program will use every token that is provided in tokens.xml. No changes are required to be made directly in the code in order to scale up the program; simply add more tokens.
	 *
	 * There are 3 ways to call this program: 
	 * 1) "TwitterUtils.java tokens.xml tweet-ids.txt" will "rehydrate" every Tweet ID in the txt file (put one ID per newline)
	 * 2) "TwitterUtils.java tokens.xml united" will get the list of all of united's followers
	 * 3) "TwitterUtils.java tokens.xml united 20140115" will return how many times united's tweets were retweeted on January 15, 2014
	 */
	public static void main(String[] args) throws ConfigurationException, TwitterException, NumberFormatException, IOException, InterruptedException {
		TwitterTokenRefresher.getTwitterTokenRefresher(args[0]); // Initializes the TwitterTokenRefresher with its config. All other classes in this project are set up to request a new token from this class when they run out of API requests on the current token.

		if (args.length == 2) {
			if (args[1].contains(".")) {
				System.err.println("Getting all tweets in file");
				testGetTweetsById(args[1]);
			}
			else {
				System.err.println("Getting followers of " + args[1]);
				testGetFollowersOfBrand(args[1]);
			}
		}
		else if (args.length == 3) {
			System.err.println("Getting number of shares of " + args[1] + "'s tweets on " + args[2]);
			testRetweetSummer(args[1], args[2]);
		}
	}
	
	
	public static void testGetTweetsById(String filepath) throws NumberFormatException, ConfigurationException, IOException, InterruptedException, TwitterException {
		List<Status> allStatuses = GetTweetsById.getAllTweets(filepath);
		for (Status status : allStatuses) {
			System.out.println(status.getId() + "|" + status.getText().replaceAll("|", ""));
		}
	}
	public static void testRetweetSummer(String brand, String yyyymmdd) throws ConfigurationException, TwitterException, InterruptedException {
		System.out.println(GetSumOfRetweets.getNumberOfRetweets(brand, yyyymmdd));
	}
	public static void testGetFollowersOfBrand(String brand) throws ConfigurationException, TwitterException, InterruptedException {
		List<User> followers = GetFollowersOfBrand.getFollowers(brand);
		for (User user : followers) {
			System.out.println(user.getName());
		}
	}
	public static void testTwitterTokenRefresher() throws TwitterException, ConfigurationException, InterruptedException {
		Twitter twitter = TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
		for (Map.Entry<String, RateLimitStatus> rateLimit: twitter.getRateLimitStatus().entrySet()) {
			System.err.format("%-40s%-40s", rateLimit.getKey(), rateLimit.getValue().getRemaining());
			System.err.println();
		}
	}
}
