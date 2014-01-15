import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;

import tokens.TwitterTokenRefresher;
import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class Tester {
	public static void main(String[] args) throws ConfigurationException, TwitterException, NumberFormatException, IOException, InterruptedException {
		TwitterTokenRefresher.getTwitterTokenRefresher("twitter-tokens.xml"); // Initializes the TwitterTokenRefresher with its config. All other classes in this project are set up to request a new token from this class when they run out of API requests on the current token.
		testGetFollowersOfBrand();
	}
	
	
	public static void testGetTweetsById() throws NumberFormatException, ConfigurationException, IOException, TwitterException, InterruptedException {
		System.out.println(GetTweetsById.getAllTweets("ids3.txt"));
	}
	public static void testRetweetSummer() throws ConfigurationException, TwitterException, InterruptedException {
		System.out.println(GetSumOfRetweets.getNumberOfRetweets("united", "20140115"));
	}
	public static void testGetFollowersOfBrand() throws ConfigurationException, TwitterException, InterruptedException {
		List<User> followers = GetFollowersOfBrand.getFollowers("united"); // TODO: Apparently the URL is too long at some point with United, this needs to be fixed
		for (User user : followers) {
			System.out.println(user.getName());
		}
	}
	public static void testTwitterTokenRefresher() throws TwitterException, ConfigurationException, InterruptedException {
		Twitter twitter = TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
		for (Map.Entry<String, RateLimitStatus> rateLimit: twitter.getRateLimitStatus().entrySet()) {
			System.out.format("%-40s%-40s", rateLimit.getKey(), rateLimit.getValue().getRemaining());
			System.out.println();
		}
	}
}
