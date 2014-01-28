import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tokens.TokenProxy;
import twitter4j.Status;

// USAGE: Create a new GetTweetsById object, passing in your Twitter client and the path to your ID text file. Call ".getAllTweets()" to return a List of every Tweet in that ID file.
// Make sure that you have one ID per newline in your ID text file.
// Speed: Approximately 720 Tweets per hour per token.
public class GetTweetsById {
	// Gets every tweet within the List<String> of Tweet IDs and returns it as a List<Status> where each element is one Tweet
	public static Map<String, Status> getAllTweets(List<String> tweetIds) {
		Map<String, Status> statusMap = new HashMap<>();
		for(String id: tweetIds) {
			try {
				statusMap.put(id, TokenProxy.getTokenProxy().getTwitter().showStatus(Long.parseLong(id.trim()))); // Take the current ID, parse it into a Long (required by Twitter4j) and get the Tweet with that ID, adding it to your List
			} catch (Exception e) {
				TokenProxy.getTokenProxy().exceptionHandler(e);
			}
		}
		return statusMap;
	}
}