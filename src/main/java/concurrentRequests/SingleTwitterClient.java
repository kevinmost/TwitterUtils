package concurrentRequests;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import twitter4j.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * @author kmost
 * @date 4/28/14
 */
public class SingleTwitterClient implements Callable<TreeMap<String, String>> {

    // Log4j logger
    private static final Logger                             LOGGER                  = Logger.getLogger(SingleTwitterClient.class.getName());

    // Delimiter that goes between pulled data fields
    public static final String                              DELIMITER               = "|";

    // For "--retweets" only. Formats the date that we get from Twitter4j as yyyyMMdd so we can do a String compare
    public static final SimpleDateFormat                    sdf = new SimpleDateFormat("yyyyMMdd");

    // The client itself
    Twitter                                                 thisTwitterClient;

    // The data that this client pulls will be added to this Map. Key is either the brandname or Tweet ID (the input from the txt file), and the value is the data retrieved
    TreeMap<String, String>                                 thisTwitterClientData   = new TreeMap<>();

    // A 0-indexed number that identifies this client
    final int                                               thisTwitterClientIndex;

    // Whether this client will get all followers of a list of brands, all tweets in a txt file, or the number of retweets for a list of brands
    final String                                            thisTwitterClientPurpose;

    // This string is only used in the "retweets" instance, where we need to get the number of retweets a brand got on a certain day
    final String                                            thisTwitterClientRetweetsDate;

    public SingleTwitterClient(Twitter twitter, int index, String purpose, String yyyymmdd) {
        thisTwitterClient = twitter;
        thisTwitterClientIndex = index;
        thisTwitterClientPurpose = purpose;
        thisTwitterClientRetweetsDate = yyyymmdd;

        // This rate limit status listener will be attached to each Twitter client we use.
        // Its purpose is to provide us with a warning when we are about to exhaust this client so that it can sleep until it is ready to go again
        thisTwitterClient.addRateLimitStatusListener(new RateLimitStatusListener() {
            @Override
            public void onRateLimitStatus(RateLimitStatusEvent event) {
                if (event.getRateLimitStatus().getRemaining() <= 2) {
                    int timeUntilReset = event.getRateLimitStatus().getSecondsUntilReset();
                    LOGGER.info("Thread " + thisTwitterClientIndex + ": Token is exhausted and will sleep for " + timeUntilReset + " seconds.");
                    try {
                        // Sleeps until the token is refreshed. Also sleep an extra 10 seconds just in case
                        Thread.sleep((timeUntilReset * 1000) + 10000);
                    } catch (InterruptedException e) {
                        LOGGER.fatal("Thread " + thisTwitterClientIndex + ": THREAD COULD NOT SLEEP");
                    }
                }
            }
            @Override
            public void onRateLimitReached(RateLimitStatusEvent event) {
                LOGGER.fatal("Thread " + thisTwitterClientIndex + ": RATE LIMIT REACHED ON TOKEN, WE NEED TO CHANGE OUR TOKEN");
            }
        });
    }


    @Override
    public TreeMap<String, String> call() {
        // The next element from the queue
        String next;

        // Each thread, when called, will keep making API calls until the data-queue is empty
        switch (thisTwitterClientPurpose) {
            case "tweets":
                while ((next = pollQueue()) != null) {
                    // Get a Tweet ID from the queue and parse it
                    Long thisIdLong;

                    try {
                        thisIdLong = Long.parseLong(next.replaceAll("[^0-9]", ""));
                        LOGGER.debug("Thread " + thisTwitterClientIndex + ": Getting data for tweet ID " + next);
                    } catch (NumberFormatException nfe) {
                        LOGGER.error("Thread " + thisTwitterClientIndex + ": Input Tweet ID " + next + " is not a valid number!");
                        continue;
                    }

                    // Get this Tweet (Status) and the user who posted it
                    Status thisIdStatus;
                    User thisIdUser;
                    try {
                        thisIdStatus = thisTwitterClient.showStatus(thisIdLong);
                        thisIdUser = thisIdStatus.getUser();
                    } catch (TwitterException te) {
                        // If this request threw an exception, there is nothing that can be done.
                        // The exception is not due to rate-limiting on our end because the rate-limit listener checks for that and sleeps the token as needed
                        // Therefore, all we can do is log the error and its underlying cause and continue
                        LOGGER.error("Thread " + thisTwitterClientIndex + ": Tweet ID " + thisIdLong + " returned error code " + te.getErrorCode() + ". Reason: " + te.getErrorMessage());
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
                    thisTwitterClientData.put(next, thisIdData);
                }
                break;
            case "brands":
                while ((next = pollQueue()) != null) {
                    // Get a brandname from the queue and parse it
                    LOGGER.debug("Thread " + thisTwitterClientIndex + ": Getting followers of " + next);


                    StringBuilder thisBrandFollowers = new StringBuilder();

                    // Boilerplate so we can traverse through the paginated Twitter items
                    IDs ids = null;
                    long cursor = -1;

                    // Get all of the followers of this brand, page by page, and add them to "thisBrandFollowers" using append
                    do {
                        try {
                            ids = thisTwitterClient.getFollowersIDs(next, cursor);
                        } catch (TwitterException te) {
                            LOGGER.error("Thread " + thisTwitterClientIndex + ": Brand " + next + " on page " + cursor + " returned error code " + te.getErrorCode() + ". Reason: " + te.getErrorMessage());
                        }
                        // Appends this array of user IDs as a comma-separated String
                        thisBrandFollowers.append(StringUtils.join(ArrayUtils.toObject(ids.getIDs()), ',')).append(',');
                    } while ((cursor = ids.getNextCursor()) != 0);

                    // Trim off the last character, which is just a hanging ","
                    thisBrandFollowers.setLength(thisBrandFollowers.length() - 1);

                    thisTwitterClientData.put(next, thisBrandFollowers.toString());
                }
                break;
            case "retweets":
                while ((next = pollQueue()) != null) {
                    // Get a brandname from the queue and parse it
                    LOGGER.debug("Thread " + thisTwitterClientIndex + ": Getting retweets of " + next + " on " + thisTwitterClientRetweetsDate);


                    // Get this brand's timeline using a paging object
                    Paging paging = new Paging(1, 200);
                    List<Status> thisBrandTimeline ;

                    // The running sum of the number of retweets that the user's statuses from this date have received
                    int count = 0;

                    // Keep going through page-by-page until we have no more tweets. The loop also contains a break when we have started to get Tweets before our target date, as the tweets are sorted by recency
                    while ((thisBrandTimeline = timelineGetter(next, paging)).size() > 0) {
                        int dateComparison = 0;
                        for (Status status : thisBrandTimeline) {
                            // Compares the current Status' creation date to the target date
                            dateComparison = sdf.format(status.getCreatedAt()).compareTo(thisTwitterClientRetweetsDate);

                            if (dateComparison == 0) {
                                count += status.getRetweetCount();
                            }
                        }
                        if (dateComparison < 0) {
                            break;
                        }
                        // Move to the next page if we haven't gotten to the point where we are getting Tweets from too far in the past
                        paging = new Paging(paging.getPage()+1, 200);
                    }

                    thisTwitterClientData.put(next, ""+count);
                }
                break;
        }

        LOGGER.info("Thread " + thisTwitterClientIndex + ": No more data in queue. Twitter thread " + thisTwitterClientIndex + " now terminating.");
        return thisTwitterClientData;
    }


    public List<Status> timelineGetter(String thisBrand, Paging paging) {
        try {
            return thisTwitterClient.getUserTimeline(thisBrand, paging);
        } catch (TwitterException te) {
            LOGGER.error("Thread " + thisTwitterClientIndex + ": Brand " + thisBrand + " on page " + paging.getPage() + " returned error code " + te.getErrorCode() + ". Reason: " + te.getErrorMessage());
        }
        return new ArrayList<>();
    }


    // This method polls the data queue from the TwitterTester class and returns the next element in the list (or null if the list is empty)
    public static synchronized String pollQueue() {
        String temp = TwitterTester.getTwitterData().poll();

        if (temp == null) {
            return null;
        } else {
            return temp.trim();
        }
    }

}
