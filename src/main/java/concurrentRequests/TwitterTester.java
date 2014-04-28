package concurrentRequests;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author kmost
 * @date 4/23/14
 */
public class TwitterTester  {

    // Log4j logger
    private static final Logger                             LOGGER                  = Logger.getLogger(TwitterTester.class.getName());

    // TODO: Implement dynamic parameters from the command line. For now, we'll hardcode these
    private static String                                   xmlFile                 = "res/tokens.xml";
    private static String                                   dataFile                = "res/data.txt";

    // Each object in this list is a Twitter client
    private static List<Twitter>                            twitterClients          = new ArrayList<>();

    //  The queue of data that we have to parse
    private static Queue<String>                            twitterData             = new LinkedList<>();

    // Counts the number of tokens that
    private static int                                      numberOfRateLimitedTokens, numberOfInvalidTokens;

    // The object that manages our Twitter threads
    private static ExecutorService                          executor;

    // The object that holds all of the data returned from our threads
    private static List<Future<TreeMap<String, Object>>>    futures                 = new ArrayList<>();



    // TODO: Enable command line parameter passing
    // TODO: Enable the other 2 functions of this program (currently can grab Tweets by ID, but also needs to grab followers by user, and grab number of retweets per day by user)
    public static void main(String[] args) {
        // Load log4j's properties
        PropertyConfigurator.configure("log4j.properties");


        // Create all of the needed clients from the XML file
        createTwitterClients(xmlFile);

        // Creates the queue of data that we need our clients to read
        readFileToQueue(dataFile);

        // Creates an executor with a thread for each token
        LOGGER.info("Creating an executor with " + twitterClients.size() + " threads");
        executor = Executors.newFixedThreadPool(twitterClients.size());

        // Submit a thread for each token to the executor, and add the returned result to the "futures" object
        for (int i = 0; i < twitterClients.size(); i++) {
            Callable<TreeMap<String, Object>> twitterExecutor = new SingleTwitterClient(twitterClients.get(i), i);
            Future<TreeMap<String, Object>> submit = executor.submit(twitterExecutor);
            LOGGER.info("Successfully submitted Twitter token " + i + " to executor");
            futures.add(submit);
        }


        // Go through each TreeMap we received from the Twitter clients and print all of their data to stdout
        for (Future<TreeMap<String, Object>> aMap : futures) {
            try {
                for (Map.Entry<String,Object> hydratedTweet : aMap.get().entrySet()) {
                    System.out.println(hydratedTweet.getKey() + "|" + hydratedTweet.getValue());
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.fatal("Interrupted while trying to merge results of data-scrape");
            }
        }

        // TODO: The code actually does get up to here, but how do we make it actually exit properly? Surely System.exit(0) would not be correct. executor.shutdown() perhaps?
    }

    // Reads an input file, splits it by newline, and stores each element in a queue that our threads will read from
    public static void readFileToQueue(String fileName) {
        List<String> tempDataList = null;
        try {
            tempDataList = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
            for (String data : tempDataList) {
                twitterData.add(data);
            }
            LOGGER.info("Added requested data to queue");
        } catch (IOException e) {
            LOGGER.fatal("Could not read data file");
        }
    }

    // Creates the Twitter clients, each with an attached RateLimitStatusListener, and stores them into the "twitterClients" variable
    public static void createTwitterClients(String fileName) {
        LOGGER.info("Parsing tokens");
        // Initialize list of Twitter clients
        twitterClients = new ArrayList<>();

        XMLConfiguration twitterTokens = null;
        // Open XML token file
        try {
            twitterTokens = new XMLConfiguration(fileName);
        } catch (ConfigurationException e) {
            LOGGER.fatal("Could not open token .xml file");
        }


        // Adds each token from the XML to twitterClients
        for (ConfigurationNode token : twitterTokens.getRoot().getChildren("token")) { // For each token object in the XML...
            ConfigurationBuilder thisTwitterBuilder = new ConfigurationBuilder();

            LOGGER.debug("Adding new token with following parameters: ");
            for (ConfigurationNode tokenElement : token.getChildren()) {
                LOGGER.debug(tokenElement.getName() + ": " + tokenElement.getValue().toString());
            }
            thisTwitterBuilder.setOAuthConsumerKey(token.getChild(0).getValue().toString());
            thisTwitterBuilder.setOAuthConsumerSecret(token.getChild(1).getValue().toString());
            thisTwitterBuilder.setOAuthAccessToken(token.getChild(2).getValue().toString());
            thisTwitterBuilder.setOAuthAccessTokenSecret(token.getChild(3).getValue().toString());

            // Creates the Twitter client for this token and tests it. If it is valid, adds it to the list of clients we have
            Twitter thisTwitterClient = new TwitterFactory(thisTwitterBuilder.build()).getInstance();
            try {
                thisTwitterClient.verifyCredentials();
                twitterClients.add(thisTwitterClient);
            } catch (TwitterException e) {
                switch (e.getErrorCode()) {
                    case 88: // Rate-limit error
                        LOGGER.info("Token " + token.getChild(2).getValue().toString() + " is rate-limited and will not be used");
                        numberOfRateLimitedTokens++;
                        break;
                    default:
                        LOGGER.warn("Token " + token.getChild(2).getValue().toString() + " is invalid. Please remove it from your XML.");
                        numberOfInvalidTokens++;
                        break;
                }
            }
        }

        // Log the number of tokens that were added. Logs as INFO if everything worked, otherwise logs as WARN
        LOGGER.log((numberOfInvalidTokens + numberOfRateLimitedTokens == 0 ? Priority.INFO : Priority.WARN), twitterTokens.getRoot().getChildren("token").size() + " tokens found in XML file. " + numberOfRateLimitedTokens + " were rate-limited, " + numberOfInvalidTokens + " were invalid.");
        LOGGER.info("Finished parsing tokens");

    }

    public static Queue<String> getTwitterData() {
        return twitterData;
    }

}
