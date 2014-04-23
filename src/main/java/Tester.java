import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tokens.TokenProxy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Tester {
    /* Always make sure to have a tokens.xml. This file should contain as many tokens as you feel are necessary for the use of this program. 
     * This program will use every token that is provided in tokens.xml. No changes are required to be made directly in the code in order to scale up the program; simply add more tokens.
     *
     * There are 3 ways to call this program: 
     * 1) "TwitterUtils.java tokens.xml tweet-ids.txt" will "rehydrate" every Tweet ID in the txt file (put one ID per newline)
     * 2) "TwitterUtils.java tokens.xml united" will get the list of all of united's followers
     * 3) "TwitterUtils.java tokens.xml united 20140115" will return how many times united's tweets were retweeted on January 15, 2014
     */

    private static final Logger logger = LogManager.getLogger("GLOBAL");
    public static final String DELIMITER = "|";


    public static void main(String[] args) throws ConfigurationException, IOException {
        if (!args[0].endsWith(".xml") || !args[2].endsWith(".txt") || (args.length == 4? args[3].length() != 8 : false)) // Exits the program if arguments passed are malformed
            showStdErrAndExit();


        TokenProxy.getTokenProxy(args[0]); // Initializes the TokenProxy with its config. All other classes in this project are set up to request a new token from this class when they run out of API requests on the current token.
        List<String> lines = Files.readAllLines(Paths.get(args[2]), Charset.defaultCharset()); // Parses the txt file into a List

        if (args[1].equals("--brands")) {
            logger.info("Getting followers of brands");
            testGetFollowersOfBrand(lines);
        }
        else if (args[1].equals("--tweets")) {
            logger.info("Rehydrating all tweets in file");
            testGetTweetsById(lines);

        }
        else if (args[1].equals("--retweets")) {
            logger.info("Getting number of shares of brand's tweets on date " + args[3]);
            testRetweetSummer(lines, args[3]);
        }
        else
            showStdErrAndExit();
    }
    
    public static void showStdErrAndExit() {
        logger.fatal("Invalid argument. \nFirst argument must be tokens.xml file. \nSecond argument must be either \"brands\" to get all followers for brands, \"tweets\" to rehydrate all tweet IDs provided, or \"retweets\" to sum up retweets for a day. \nThird argument must be a .txt file containing either a list of tweet IDs or a list of brand names. \nFourth argument is a date in yyyyMMdd format, provided only if using the \"retweets\" function");
        System.exit(1);
    }
    public static void testGetTweetsById(List<String> tweetIds) {
        List<String> statusList = GetTweetsById.getAllTweets(tweetIds);
        for (String status : statusList) {
            System.out.println(status);
        }
    }
    public static void testRetweetSummer(List<String> brands, String yyyymmdd) {
        Map<String, Integer> retweetsMap = GetSumOfRetweets.getNumberOfRetweets(brands, yyyymmdd);
        for (Map.Entry<String, Integer> brand : retweetsMap.entrySet()) {
            System.out.println(brand.getKey() + DELIMITER + brand.getValue());
        }
    }
    public static void testGetFollowersOfBrand(List<String> brands) {
        Map<String, List<Long>> allBrandsMap = GetFollowersOfBrand.getFollowers(brands);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Long>> brand : allBrandsMap.entrySet()) {
            sb.append(brand.getKey()).append(DELIMITER);
            for (Long follower : brand.getValue()) {
                sb.append(follower).append(',');
            }
            sb.setLength(sb.length()-1);
            System.out.println(sb.toString());
            sb.setLength(0);
        }
    }
}
