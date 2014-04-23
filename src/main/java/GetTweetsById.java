import tokens.TokenProxy;
import twitter4j.Status;
import twitter4j.User;

import java.util.ArrayList;
import java.util.List;

// USAGE: Create a new GetTweetsById object, passing in your Twitter client and the path to your ID text file. Call ".getAllTweets()" to return a List of every Tweet in that ID file.
// Make sure that you have one ID per newline in your ID text file.
// Speed: Approximately 720 Tweets per hour per token.
public class GetTweetsById {
    // Puts every tweet as its own List within the entire statusList
    public static List<String> getAllTweets(List<String> tweetIds) {
        List<String> statusList = new ArrayList<>();

        for(String id: tweetIds) { // For each Tweet ID...
            try {
                Status currentStatus = TokenProxy.getTokenProxy().getTwitter().showStatus(Long.parseLong(id.trim())); // Get the current Tweet
                User currentUser = currentStatus.getUser(); // Get the user that posted the current Tweet

                // Adds the following to the List
                statusList.add(
                        id + Tester.DELIMITER +  // The user's ID
                        currentUser.getFollowersCount() + Tester.DELIMITER +  // The followers of the user
                        currentUser.getFriendsCount() + Tester.DELIMITER +  // The friends of the user
                        currentUser.getStatusesCount() + Tester.DELIMITER +  // The number of statuses the user has posted
                        currentUser.getScreenName() + Tester.DELIMITER + // The user's name
                        currentStatus.getText().replaceAll("|", "").replaceAll("[\n\r]", " ") // The text of the Tweet
                        );
            } catch (Exception e) {
                TokenProxy.getTokenProxy().exceptionHandler(e);
            }
        }
        return statusList;
    }
}