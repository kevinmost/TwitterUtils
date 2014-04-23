import org.apache.log4j.Logger;
import tokens.TokenProxy;
import twitter4j.Paging;
import twitter4j.Status;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetSumOfRetweets {


    private static final Logger logger = Logger.getLogger("GLOBAL");

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private static List<Status> statusPage = new ArrayList<>();

    private static int count = 0;
    private static int dateComparison;
    private static Paging paging = new Paging(1,200);

    // Run-speed: 1000 handles process in ~5 minutes
    public static Map<String, Integer> getNumberOfRetweets(List<String> brands, String day) {
        Map<String, Integer> retweetsMap = new HashMap<>();
        for (String brand : brands) {
            // Initialize variables
            count = 0;
            dateComparison = 0;
            paging = new Paging(1, 200);
            
            while((statusPage = getUserTimeline(brand)).size() > 0) {
                for (Status status : statusPage) { // Go through each status we just received from this page and check if the date matches
                    dateComparison = sdf.format(status.getCreatedAt()).compareTo(day);
                    if (dateComparison == 0) { // The dates of the returned Tweets are equal to our required dates
                        count += status.getRetweetCount();
                    }
                }
                if (dateComparison < 0) { // If we are hitting Tweets that are earlier than our target date, stop running this loop
                    break;
                }
                paging = new Paging(paging.getPage()+1, 200); // If we are continuing this loop, go on to the next page before re-iterating
            }
            retweetsMap.put(brand, count); // Put this brand's retweet sum into the Map
        }
        return retweetsMap;
    }
    
    public static List<Status> getUserTimeline(String brand) {
        try {
            return TokenProxy.getTokenProxy().getTwitter().getUserTimeline(brand, paging);
        }
        catch (Exception e) {
            TokenProxy.getTokenProxy().exceptionHandler(e);
        }
        return new ArrayList<Status>();
    }

}