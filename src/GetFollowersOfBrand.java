import java.util.List;

import org.apache.commons.configuration.ConfigurationException;

import tokens.TwitterTokenRefresher;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;


public class GetFollowersOfBrand {

	public static List<User> getFollowers(String brand) throws TwitterException, ConfigurationException, InterruptedException {
		Twitter twitter = TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
		IDs ids = twitter.getFollowersIDs(brand, -1);
		return twitter.lookupUsers(ids.getIDs());
	}
}
