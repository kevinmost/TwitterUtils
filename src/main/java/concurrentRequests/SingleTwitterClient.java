package concurrentRequests;

import org.apache.log4j.Logger;
import twitter4j.*;

import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * @author kmost
 * @date 4/28/14
 */
public class SingleTwitterClient implements Callable<TreeMap<String, Object>> {

    // Log4j logger
    private static final Logger                             LOGGER                  = Logger.getLogger(SingleTwitterClient.class.getName());

    // Delimiter that goes between pulled data fields
    public static final String                              DELIMITER               = "|";

    // The client itself
    Twitter                                                 thisTwitterClient;

    // The data that this client pulls will be added to this Map
    TreeMap<String, Object>                                 thisTwitterClientData   = new TreeMap<>();

    // A 0-indexed number that identifies this client
    final int                                               uniqueIndex;


    public SingleTwitterClient(Twitter twitter, int ui) {
        thisTwitterClient = twitter;
        uniqueIndex = ui;
        // This rate limit status listener will be attached to each Twitter client we use.
        // Its purpose is to provide us with a warning when we are about to exhaust this client so that it can sleep until it is ready to go again
        thisTwitterClient.addRateLimitStatusListener(new RateLimitStatusListener() {
            @Override
            public void onRateLimitStatus(RateLimitStatusEvent event) {
                if (event.getRateLimitStatus().getRemaining() <= 2) {
                    int timeUntilReset = event.getRateLimitStatus().getSecondsUntilReset();
                    LOGGER.info("Thread " + uniqueIndex + ": Token is exhausted and will sleep for " + timeUntilReset + " seconds.");
                    try {
                        // Sleeps until the token is refreshed. Also sleep an extra 10 seconds just in case
                        Thread.sleep((timeUntilReset * 1000) + 10000);
                    } catch (InterruptedException e) {
                        LOGGER.fatal("Thread " + uniqueIndex + ": THREAD COULD NOT SLEEP");
                    }
                }
            }
            @Override
            public void onRateLimitReached(RateLimitStatusEvent event) {
                LOGGER.fatal("Thread " + uniqueIndex + ": RATE LIMIT REACHED ON TOKEN, WE NEED TO CHANGE OUR TOKEN");
            }
        });
    }


    @Override
    public TreeMap<String, Object> call() {
        // Each thread, when called, will keep making API calls until the data-queue is empty
        while (!TwitterTester.getTwitterData().isEmpty()) {
            // Get a Tweet ID from the queue and parse it
            String thisId = TwitterTester.getTwitterData().poll().trim();
            LOGGER.debug("Thread " + uniqueIndex + ": Getting data for tweet ID " + thisId);
            Long thisIdLong = Long.parseLong(thisId);

            // Get this Tweet (Status) and the user who posted it
            Status thisIdStatus = null;
            User thisIdUser = null;
            try {
                thisIdStatus = thisTwitterClient.showStatus(thisIdLong);
                thisIdUser = thisIdStatus.getUser();
            } catch (TwitterException te) {
                // If this request threw an exception, there is nothing that can be done.
                // The exception is not due to rate-limiting on our end because the rate-limit listener checks for that and sleeps the token as needed
                // Therefore, all we can do is log the error and its underlying cause and continue
                LOGGER.error("Thread " + uniqueIndex + ": Tweet ID " + thisIdLong + " returned error code " + te.getErrorCode() + ". Reason: " + te.getErrorMessage());
                continue;
            }

            // Collects the data we need to go along with this Tweet ID
            String thisIdData =
                    thisIdUser.getFollowersCount() + DELIMITER +
                    thisIdUser.getFriendsCount() + DELIMITER +
                    thisIdUser.getStatusesCount() + DELIMITER +
                    thisIdUser.getScreenName() + DELIMITER +
                    thisIdStatus.getText().replaceAll(DELIMITER, "").replaceAll("[\n\r]", " ");

            // Add this data to the list that this Twitter client is accumulating
            thisTwitterClientData.put(thisId, thisIdData);
        }

        LOGGER.info("Thread " + uniqueIndex + ": No more data in queue. Twitter thread " + uniqueIndex + " now terminating.");
        return thisTwitterClientData;
    }

}
