import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import tokens.TokenProxy;
import twitter4j.IDs;


public class GetFollowersOfBrand {


	public static Map<String, List<Long>> getFollowers(List<String> brands) {
		Map<String, List<Long>> allBrandsMap = new HashMap<>();
		long cursor = -1;
		IDs ids = null;
		System.err.println("[INFO ]: Listing followers's ids.");
		for (String brand: brands) {
			do {
				try {
					ids = TokenProxy.getTokenProxy().getTwitter().getFollowersIDs(brand, cursor);
					allBrandsMap.put(brand, Arrays.asList(ArrayUtils.toObject(ids.getIDs())));
				}
				catch (Exception e) {
					TokenProxy.getTokenProxy().exceptionHandler(e);
				}
				if (ids==null) { // If that didn't work, give it another shot because now TokenProxy should have replaced that token
					try {
						ids = TokenProxy.getTokenProxy().getTwitter().getFollowersIDs(brand, cursor);
						allBrandsMap.put(brand, Arrays.asList(ArrayUtils.toObject(ids.getIDs())));
					}
					catch (Exception e) {
						TokenProxy.getTokenProxy().exceptionHandler(e);
					}
				}
			} while ((cursor = ids.getNextCursor()) != 0);
		}
		return allBrandsMap;
	}
	public static long[] getFollowerIdsOnPage(String brand, int page) {
		try {
			return TokenProxy.getTokenProxy().getTwitter().getFollowersIDs(brand, page).getIDs();
		}
		catch(Exception e) {
			TokenProxy.getTokenProxy().exceptionHandler(e);
			return getFollowerIdsOnPage(brand, page);
		}
	}
}
