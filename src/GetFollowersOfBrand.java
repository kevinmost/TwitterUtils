import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.ArrayUtils;

import tokens.TwitterTokenRefresher;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;


public class GetFollowersOfBrand {

	private static int chunkSize = 100; // Number of followers to get in each request. If this is too large, Twitter will throw an error
	private static Twitter twitter;
	
	public static List<User> getFollowers(String brand) throws TwitterException, ConfigurationException, InterruptedException {
		twitter = TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
		List<Long> allIds = new ArrayList<>();
		long[] ids;
		
		for (int i = -1; (ids = getFollowerIdsOnPage(brand, i)).length != 0; i++) { // Continues iterating as long as there are more followers still being gotten
			allIds.addAll(Arrays.asList(ArrayUtils.toObject(ids))); // Adds the current set of IDs to the allIds list
		}
		List<User> users = new ArrayList<>();
		int numberOfLookups = allIds.size()/chunkSize + 1;
		for (int i = 0; i < numberOfLookups; i++) { // Each iteration looks up 100 users and adds them to the list of users that will be returned
			try {
				users.addAll(
						twitter.lookupUsers(
								Arrays.copyOfRange(
										ArrayUtils.toPrimitive(
												allIds.toArray(new Long[allIds.size()])
										), 
										chunkSize*i, 
										chunkSize*(i+1)
								)
						)
				); // Get one chunk of followers and store them into a ResponseList\
			}
			catch(TwitterException te) {
				System.err.println("ERROR: Could not find user in lookup group");
			}
		}
		
		return users;
	}
	public static long[] getFollowerIdsOnPage(String brand, int page) throws ConfigurationException, TwitterException, InterruptedException {
		try {
			return twitter.getFollowersIDs(brand, page).getIDs();
		}
		catch(TwitterException te) {
			if (te.getStatusCode() == 88) {
				System.err.println("RATE LIMITED. Getting new token");
				TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
			}
		}
		return twitter.getFollowersIDs(brand, page).getIDs();
	}
}
