//package tokens;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import org.apache.commons.configuration.ConfigurationException;
//import org.apache.commons.configuration.XMLConfiguration;
//import org.apache.commons.configuration.tree.ConfigurationNode;
//
//import twitter4j.Twitter;
//import twitter4j.TwitterException;
//import twitter4j.TwitterFactory;
//import twitter4j.conf.Configuration;
//import twitter4j.conf.ConfigurationBuilder;
//
//// USAGE: Create a new TwitterTokenRefresher object, passing in the path to your config.xml which contains your tokens. Call ".createTwitterClientWithValidToken()" to return a Twitter client that has a valid 
//public class TwitterTokenRefresher {
//	private Twitter twitter = null;
//	private List<Configuration> configs = new ArrayList<>();
//	private XMLConfiguration config = null;
//	private int resetTime = 0;
//	private String[] nodes = {"/statuses/show/:id", "/followers/ids", "/users/show/:id", "/statuses/user_timeline"};
//	
//	// Singleton
//	private static TwitterTokenRefresher instance;
//	public static synchronized TwitterTokenRefresher getTwitterTokenRefresher() { // This method can be called for convenience's sake without passing an XML config file, but you have to have first called the method below earlier in the program to initialize the config
//		if (instance == null)
//			System.err.println("[ERROR]: No config set yet. Please call .getTwitterTokenRefresher(String xmlConfig) with a filename for an XML config file before calling it with no parameters.");
//		return instance;
//	}
//	public static synchronized TwitterTokenRefresher getTwitterTokenRefresher(String filename) throws ConfigurationException { // Make sure the first time that you call this class, you are using this version of the method (with the config's filepath passed as a parameter)
//		if (instance == null)
//			instance = new TwitterTokenRefresher(filename);
//		return instance;
//	}
//	public Object clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
//	private TwitterTokenRefresher(String filename) throws ConfigurationException { // Constructor
//		config = new XMLConfiguration(filename);
//		populateListOfTokens();
//	}
//	
//	public Twitter createTwitterClientWithValidToken() throws ConfigurationException, TwitterException, InterruptedException {
//		// Go through all of your tokens
//		for (Configuration conf : configs) {
//			twitter = new TwitterFactory(conf).getInstance(); // Rebuild the client to use the current token
//			if (isTokenValidForAllNodes(twitter, nodes)) {
//				System.err.println("[INFO ]: Success! Using token " + conf.getOAuthAccessToken());
//				return twitter;
//			}
//			else { // If the token didn't work, also log its reset-time. We will keep the longest reset-time as the amount of time to wait once all tokens are exhausted
//				System.err.println("[INFO ]: " + conf.getOAuthAccessToken() + " failed. Trying next token...");
//				for (String node : nodes) {
//					int i = twitter.getRateLimitStatus().get(node).getSecondsUntilReset();
//					if (i > resetTime) {
//						resetTime = i;
//					}
//				}
//			}
//		}
//
//		// If none of the above tokens worked, we need to wait for our tokens to refresh
//		System.err.println("[INFO ]: All of your tokens are currently rate-limited. Program will sleep until a new token is available in " + resetTime + " seconds.");
//
//		pauseBar(resetTime);
//
//		// Re-call the method recursively so that a token is returned
//		return createTwitterClientWithValidToken();
//	}
//	// Makes the program pause for resetTime seconds, while displaying a linearly-filling progress-bar
//	public static void pauseBar(int resetTime) throws InterruptedException {
//			// This bar will actually appear to "fill up" when the program is invoked from the console. However, Eclipse's console is quirky so it will simply display the progress bar multiple times.
//			StringBuilder statusBar = new StringBuilder("|"); // Change the left-hand border of the progress bar
//			String statusBarSegment = "="; // Change the "segments" used by the bar
//			String statusBarArrow = ">"; // Change the "arrow" that appears at the end of the "currently-filled" section of the bar
//			String statusBarEnd = "|"; // Change the right-hand border of the progress bar
//			int numSegments = 50; // Change the number of segments shown in the progress bar
//			
//			// Do not modify these variables
//			int whitespace = 1;
//			int sleepTime = resetTime/numSegments; // "resetTime" is the amount of time the program should display this bar for in seconds (ex, resetTime = 60 would make this bar take 60 seconds to fill up)
//			
//			while ((whitespace = numSegments - statusBarEnd.length() - statusBar.append(statusBarSegment).length()) > 0) { // Runs until all empty space is filled
//				TimeUnit.SECONDS.sleep(sleepTime); // Pauses long enough to represent one segment of the bar
//				System.err.format("%-" + statusBar.length() + "s%-" + whitespace + "s%-" + statusBarEnd.length() + "s", statusBar.toString(), statusBarArrow, statusBarEnd + "\r"); // Prints the entire bar, formatted properly
//			}
//	}
//	public static boolean isTokenValidForAllNodes(Twitter twitter, String[] nodes) throws TwitterException {
//		boolean b = true;
//		for (String node : nodes) { // Goes through each node. If any of them are rate-limited, returns false
//			if (twitter.getRateLimitStatus().get(node).getRemaining() <= 0)
//				b = false;
//		}
//		return b;
//	}
//	public void populateListOfTokens() throws ConfigurationException {
//		for (ConfigurationNode tokenNode : config.getRoot().getChildren("token")) { // Take each "token" node in the XML and, one by one, use their data to create new Configuration objects. Store those into a List for later.
//			configs.add(
//					new ConfigurationBuilder()
//					.setDebugEnabled(true)
//					.setPrettyDebugEnabled(true)
//					.setOAuthConsumerKey(tokenNode.getChildren().get(0).getValue().toString())
//					.setOAuthConsumerSecret(tokenNode.getChildren().get(1).getValue().toString())
//					.setOAuthAccessToken(tokenNode.getChildren().get(2).getValue().toString())
//					.setOAuthAccessTokenSecret(tokenNode.getChildren().get(3).getValue().toString())
//					.build());
//		}
//		System.err.println("[INFO ]: ADDED " + configs.size() + " tokens!");
//	}
//}
