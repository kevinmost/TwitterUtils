import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;

import tokens.TwitterTokenRefresher;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

// USAGE: Create a new GetTweetsById object, passing in your Twitter client and the path to your ID text file. Call ".getAllTweets()" to return a List of every Tweet in that ID file.
// Make sure that you have one ID per newline in your ID text file.
// Speed: Approximately 720 Tweets per hour per token.
public class GetTweetsById {
	// Gets every tweet within the List<String> of Tweet IDs and returns it as a List<Status> where each element is one Tweet
	public static List<Status> getAllTweets(String idTextFilePath) throws IOException, NumberFormatException, TwitterException, ConfigurationException, InterruptedException {
		List<Status> statuses = new ArrayList<>();
		Twitter twitter = TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
		for(String id: Files.readAllLines(Paths.get(idTextFilePath), Charset.defaultCharset())) {
			try {
				statuses.add(twitter.showStatus(Long.parseLong(id.trim()))); // Take the current ID, parse it into a Long (required by Twitter4j) and get the Tweet with that ID, adding it to your List
			} catch (TwitterException te) {
				if (te.getErrorCode() == 88) { // If error code 88 is thrown, we're rate-limited and need to move to a new app token
					System.err.println("RATE LIMITED. Getting new token");
					twitter = TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
				}
				else // If another TwitterException error was thrown, there was just a problem with this tweet for some reason
					System.err.println(id + " not found");
			} catch(NumberFormatException nfe) {
				System.err.println(id + " is not in the proper format for Twitter tweet IDs");
			}
		}
		return statuses;
	}
}