package tokens;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author kmost
 * @date 4/23/14
 */
public class TokenHandler {

    private static final Logger logger = Logger.getLogger(TokenHandler.class.getName());

    public static final RateLimitStatusListener rlsListener = new RateLimitStatusListener() {
        @Override
        public void onRateLimitStatus(RateLimitStatusEvent event) {
            if (event.getRateLimitStatus().getRemaining() <= 1) {
                logger.fatal("WE NEED TO CHANGE OUR TOKEN");
            }
        }
        @Override
        public void onRateLimitReached(RateLimitStatusEvent event) {
            logger.fatal("RATE LIMIT REACHED ON TOKEN, WE NEED TO CHANGE OUR TOKEN");
        }
    };

    // The XML token file
    private XMLConfiguration twitterTokens;

    // The Twitter clients that we generate from the XML file
    private List<Twitter> twitterClients;



    private int numberOfRateLimitedTokens = 0;
    private int numberOfInvalidTokens = 0;

    // Constructor
    public TokenHandler(String fileName) {
        logger.info("Parsing tokens");
        // Initialize list of Twitter clients
        twitterClients = new ArrayList<>();

        // Open XML token file
        try {
            twitterTokens = new XMLConfiguration(fileName);
        } catch (ConfigurationException e) {
            logger.fatal("Could not open token .xml file");
        }


        // Adds each token from the XML to twitterClients
        for (ConfigurationNode token : twitterTokens.getRoot().getChildren("token")) { // For each token object in the XML...
            ConfigurationBuilder thisTwitterBuilder = new ConfigurationBuilder();

            logger.debug("Adding new token with following parameters: ");
            for (ConfigurationNode tokenElement : token.getChildren()) {
                logger.debug(tokenElement.getName() + ": " + tokenElement.getValue().toString());
            }
            thisTwitterBuilder.setOAuthConsumerKey(token.getChild(0).getValue().toString());
            thisTwitterBuilder.setOAuthConsumerSecret(token.getChild(1).getValue().toString());
            thisTwitterBuilder.setOAuthAccessToken(token.getChild(2).getValue().toString());
            thisTwitterBuilder.setOAuthAccessTokenSecret(token.getChild(3).getValue().toString());

            // Creates the Twitter client for this token and tests it. If it is valid, adds it to the list of tokens we have
            Twitter thisTwitterClient = new TwitterFactory(thisTwitterBuilder.build()).getInstance();
            thisTwitterClient.addRateLimitStatusListener(rlsListener);
            try {
                thisTwitterClient.verifyCredentials();
                twitterClients.add(thisTwitterClient);
            } catch (TwitterException e) {
                switch (e.getErrorCode()) {
                    case 88: // Rate-limit error
                        logger.info("Token " + token.getChild(2).getValue().toString() + " is rate-limited and will not be used");
                        numberOfRateLimitedTokens++;
                        break;
                    default:
                        logger.warn("Token " + token.getChild(2).getValue().toString() + " is invalid. Please remove it from your XML.");
                        numberOfInvalidTokens++;
                        break;
                }
            }
        }

        // Log the number of tokens that were added. Logs as INFO if everything worked, otherwise logs as WARN
        logger.log((numberOfInvalidTokens + numberOfRateLimitedTokens == 0 ? Priority.INFO : Priority.WARN), twitterTokens.getRoot().getChildren("token").size() + " tokens found in XML file. " + numberOfRateLimitedTokens + " were rate-limited, " + numberOfInvalidTokens + " were invalid.");
        logger.info("Finished parsing tokens");
    }

    public void makeTwitterCall(Callable<Twitter> m, String... parameters) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    }

}
