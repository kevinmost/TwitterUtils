import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;

import tokens.TwitterTokenRefresher;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class GetSumOfRetweets {

	private static Twitter twitter;
	

	// Day format should be "yyyymmdd"
	public static int getNumberOfRetweets(String brand, String day) throws ConfigurationException, TwitterException, InterruptedException {
		int count = 0;
		twitter = TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
		Paging paging = new Paging(1, 200);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		List<Status> statusPage = new ArrayList<>();
		// boolean isNumeric = brand.matches("-?\\d+");
		
		while((statusPage = getUserTimeline(brand, paging)).size() > 0) {
			for (Status status : statusPage) { // Go through each status we just received from this page and check if the date matches
				if (sdf.format(status.getCreatedAt()).equals(day)) {
					count += status.getRetweetCount();
				}
			}
			paging = new Paging(paging.getPage()+1, 200);
		}
		return count;
	}
	
	public static List<Status> getUserTimeline(String brand, Paging paging) throws ConfigurationException, TwitterException, InterruptedException {
		try {
			return twitter.getUserTimeline(brand, paging);
		}
		catch (TwitterException te) {
			if (te.getErrorCode() == 88) {
				System.err.println("RATE LIMITED. Getting new token");
				twitter = TwitterTokenRefresher.getTwitterTokenRefresher().createTwitterClientWithValidToken();
			}
		}
		return twitter.getUserTimeline(brand, paging);
	}

}