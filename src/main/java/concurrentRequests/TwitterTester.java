package concurrentRequests;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.File;
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
    private static final Logger                                 LOGGER                  = Logger.getLogger(TwitterTester.class.getName());

    private static Properties                                   loggerProps             = new Properties() {{
        setProperty("log4j.rootLogger", "INFO, stderr");
        setProperty("log4j.appender.stderr", "org.apache.log4j.ConsoleAppender");
        setProperty("log4j.appender.stderr.Target", "System.err");
        setProperty("log4j.appender.stderr.layout", "org.apache.log4j.PatternLayout");
        setProperty("log4j.appender.stderr.layout.ConversionPattern", "%-9r %-7p (%c{1}:%M:%L) - %m%n");
    }};


    // The delimiter that goes between the key and the value of the "twitterFutures" Maps
    private static final String                                  DELIMITER               = "|";

    // The settings that the user inputted using the command-line args
    private static Map<String, String>                          commandLineArgs;



    // Each object in this list is a Twitter client created from our tokens
    private static List<Twitter>                                twitterClients          = new ArrayList<>();

    //  The queue of data that we have to parse using the twitterClients
    private static Queue<String>                                twitterData             = new LinkedList<>();

    // The object that holds all of the data returned from our twitterClients
    private static List<Future<TreeMap<String, String>>>        twitterFutures          = new ArrayList<>();

    // The object that manages our twitterClients
    private static ExecutorService                              twitterExecutors;





    public static void main(String[] args) {
        // Load log4j's properties
        PropertyConfigurator.configure(loggerProps);


        // Take the user's command line arguments
        if ((commandLineArgs = parseArguments(args)) == null) {
            LOGGER.fatal("EXITING DUE TO MALFORMED COMMAND-LINE ARGUMENTS");
            System.exit(1);
        }

        // Create all of the needed clients from the XML file
        createTwitterClients(commandLineArgs.get("tokens"));

        // Creates the queue of data that we need our clients to read
        readFileToQueue(commandLineArgs.get("data"));

        // Creates an executor with a thread for each token
        LOGGER.info("Creating an executor with " + twitterClients.size() + " threads");
        twitterExecutors = Executors.newFixedThreadPool(twitterClients.size());

        Callable<TreeMap<String, String>> twitterExecutor;
        // Submit a thread for each token to the executor, and add the returned result to the "twitterFutures" object
        for (int i = 0; i < twitterClients.size(); i++) {
            if (commandLineArgs.get("mode").equals("retweets")) {
                twitterExecutor = new SingleTwitterClient(twitterClients.get(i), i, commandLineArgs.get("mode"), commandLineArgs.get("date"));
            } else {
                twitterExecutor = new SingleTwitterClient(twitterClients.get(i), i, commandLineArgs.get("mode"), null);
            }
            Future<TreeMap<String, String>> submit = twitterExecutors.submit(twitterExecutor);
            LOGGER.debug("Successfully submitted Twitter token " + i + " to executor");
            twitterFutures.add(submit);
        }


        // Ugly code that prevents the executor from receiving new jobs, and then waits until all currently-submitted threads have finished to move on
        try {
            twitterExecutors.shutdown();
            twitterExecutors.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
            LOGGER.info("All Twitter data successfully retrieved!");
        } catch (InterruptedException e) {
            LOGGER.fatal("Interrupted before Twitter data could all be received! Error: " + e.getMessage());
            System.exit(1);
        }

        // Go through each TreeMap we received from the Twitter clients and print all of their data to stdout
        for (Future<TreeMap<String, String>> aMap : twitterFutures) {
            try {
                for (Map.Entry<String,String> hydratedTweet : aMap.get().entrySet()) {
                    System.out.println(hydratedTweet.getKey() + DELIMITER + hydratedTweet.getValue());
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.fatal("Interrupted while trying to merge results of data-scrape! Error: " + e.getMessage());
                e.printStackTrace();
            }
        }


    }

    // Reads an input file, splits it by newline, and stores each element in a queue that our threads will read from
    public static void readFileToQueue(String fileName) {
        try {
            LOGGER.info("Adding data from " + fileName);
            List<String> tempDataList = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
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
                        break;
                    default:
                        LOGGER.warn("Token " + token.getChild(2).getValue().toString() + " is invalid. Please remove it from your XML. Reason: " + e.getErrorMessage());
                        break;
                }
            }
        }

        // Log the number of tokens that were added. Logs as INFO if everything worked, otherwise logs as WARN
        int numBadTokens = 0;
        if ((numBadTokens = twitterTokens.getRoot().getChildren("token").size() - twitterClients.size()) == 0) {
            LOGGER.info("Successfully added all " + twitterClients.size() + " tokens!");
        } else {
            LOGGER.warn("Could not add " + numBadTokens + " to your config. Please check this log for the token(s) that were invalid and the error code given.");
        }

    }

    public static Queue<String> getTwitterData() {
        return twitterData;
    }


    // Takes in the user's arguments, makes sure that they haven't made any errors, and, if they haven't, returns a map of all of their arguments with their values
    // Returns null if the user made a mistake, and logs an error of type FATAL in the log describing the error
    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> argMap = new HashMap<>();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                int indexOfEquals = arg.indexOf('=');
                String key = arg.substring(2, indexOfEquals);
                String value = arg.substring(indexOfEquals + 1);
                argMap.put(key.trim(), value.replace('\\', '/').trim()); // Filepaths can sometimes get entered with backslashes, but Java likes it better when you use forward slashes '/'
            }
            else {
                LOGGER.fatal(arg + " is not a valid argument!\n" +
                        "Valid arguments (please note the \"--\" before each argument):\n" +
                        "--data=[datafile.txt]\n" +
                        "--tokens=[tokens.xml]\n" +
                        "--mode={brands,tweets,retweets}\n" +
                        "[--date=yyyyMMdd]");
                return null;
            }
        }

        // Put this check first because if the user wants to change the logger verbosity, it should be changed before we potentially print out a bunch of other messages to the log
        if (argMap.get("verbosity") != null) {
            argMap.put("verbosity", argMap.get("verbosity").toUpperCase());
            switch (argMap.get("verbosity")) {
                case "DEBUG":case "INFO":case "WARN":case "ERROR":case "FATAL":
                    LOGGER.info("Set logger verbosity to " + argMap.get("verbosity"));
                    loggerProps.setProperty("log4j.rootLogger", argMap.get("verbosity") + ", stderr");
                    break;
                default:
                    LOGGER.fatal("You entered an invalid verbosity setting. Valid values are: debug, info, warn, error, fatal.");
                    return null;
            }
        }


        for (String requiredParams : new String[] {"tokens", "data", "mode"}) {
            if (argMap.get(requiredParams) == null) {
                LOGGER.fatal("You are missing a required parameter: " + requiredParams);
                return null;
            }
        }

        if (!argMap.get("tokens").endsWith(".xml")) {
            LOGGER.fatal("Your tokens file must be in .xml format");
            return null;
        }
        if (!argMap.get("data").endsWith(".txt")) {
            LOGGER.fatal("Your data file must be in .txt format (newline-delimited)");
            return null;
        }

        if (!new File(argMap.get("data")).exists() || !new File(argMap.get("tokens")).exists()) {

            LOGGER.fatal("Could not find specified data and/or token files. Paths given: \n" +
                    "Data file: " + argMap.get("data") + "\n" +
                    "Token file: " + argMap.get("tokens"));
            return null;
        }

        if (argMap.get("mode").equals("retweets")) {
            if (argMap.get("date") == null) {
                LOGGER.fatal("You are in retweets mode. You must enter a date in yyyyMMdd format to retrieve retweets from");
                return null;
            } else {
                if (!argMap.get("date").matches("[0-9]{8}")) {
                    LOGGER.fatal("Your specified date parameter is not in \"yyyyMMdd\" format. \n" +
                        "Specified date: " + argMap.get("date"));
                    return null;
                }
            }
        }


        StringBuilder status = new StringBuilder();
        for (Map.Entry<String, String> arg : argMap.entrySet()) {
            status.append(arg.getKey()).append(": ").append(arg.getValue()).append("\n");
        }
        LOGGER.info("Successfully parsed the following command-line parameters:\n" + status.toString());

        return argMap;
    }
}
