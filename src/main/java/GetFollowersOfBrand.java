import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import tokens.TokenProxy;
import twitter4j.IDs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GetFollowersOfBrand {

    private static final Logger logger = Logger.getLogger("GLOBAL");

    public static Map<String, List<Long>> getFollowers(List<String> brands) {
        Map<String, List<Long>> allBrandsMap = new HashMap<>();
        long cursor = -1;
        IDs ids = null;
        logger.info("Listing followers's ids.");
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
