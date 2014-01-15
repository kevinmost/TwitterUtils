package tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

// USAGE: Create a new TwitterTokenRefresher object, passing in the path to your config.xml which contains your tokens. Call ".createTwitterClientWithValidToken()" to return a Twitter client that has a valid 
public class TwitterTokenRefresher {
	private Twitter twitter = null;
	private List<Configuration> configs = new ArrayList<>();
	private XMLConfiguration config = null;
	int resetTime = 0;

	
	// Singleton
	private static TwitterTokenRefresher instance;
	public static synchronized TwitterTokenRefresher getTwitterTokenRefresher() { // This method can be called for convenience's sake without passing an XML config file, but you have to have first called the method below earlier in the program to initialize the config
		if (instance == null)
			System.err.println("ERROR: No config set yet. Please call .getTwitterTokenRefresher(String xmlConfig) with a filename for an XML config file before calling it with no parameters.");
		return instance;
	}
	public static synchronized TwitterTokenRefresher getTwitterTokenRefresher(String filename) throws ConfigurationException { // Make sure the first time that you call this class, you are using this version of the method (with the config's filepath passed as a parameter)
		if (instance == null)
			instance = new TwitterTokenRefresher(filename);
		return instance;
	}
	public Object clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
	private TwitterTokenRefresher(String filename) throws ConfigurationException { // Constructor
		config = new XMLConfiguration(filename);
		populateListOfTokens();
	}
	
	public Twitter createTwitterClientWithValidToken() throws ConfigurationException, TwitterException, InterruptedException {
		// Go through all of your tokens
		for (Configuration conf : configs) {
			twitter = new TwitterFactory(conf).getInstance(); // Rebuild the client to use the current token

			if (twitter.getRateLimitStatus().get("/statuses/show/:id").getRemaining() > 0) { // Test if the token can be used, return it if it is. Otherwise, it will try another token
				System.err.println("Success! Using token " + conf.getOAuthAccessToken());
				return twitter;
			}
			else { // If the token didn't work, also log its reset-time. We will keep the longest reset-time as the amount of time to wait once all tokens are exhausted
				System.err.println(conf.getOAuthAccessToken() + " failed. Trying next token...");
				int i = twitter.getRateLimitStatus().get("/statuses/show/:id").getSecondsUntilReset();
				if (i > resetTime) {
					resetTime = i;
				}
			}
		}

		// If none of the above tokens worked, we need to wait for our tokens to refresh
		System.err.println("ERROR: All of your tokens are currently rate-limited. Program will sleep until a new token is available in " + resetTime + " seconds.");
		
		// This is a progress bar that is 50 segments long. It doesn't actually work the way we want it to in Eclipse (it won't overwrite the old one, but displays under it). This actually works in a command line console, though.
		StringBuilder sb = new StringBuilder("|>                                                 |\r");
		while (sb.toString().replace(" ", "").length() < 50) {
			TimeUnit.SECONDS.sleep(resetTime/50);
			sb.insert(1, '=');
			sb.deleteCharAt(sb.toString().replace(" ", "").length() - 1);
			System.out.print(sb.toString());
		}
		
		// Re-call the method recursively so that a token is returned
		createTwitterClientWithValidToken();
		return null;
	}
	
	public void populateListOfTokens() throws ConfigurationException {
		for (ConfigurationNode tokenNode : config.getRoot().getChildren("token")) { // Take each "token" node in the XML and, one by one, use their data to create new Configuration objects. Store those into a List for later.
			configs.add(
					new ConfigurationBuilder()
					.setDebugEnabled(true)
					.setPrettyDebugEnabled(true)
					.setOAuthConsumerKey(tokenNode.getChildren().get(0).getValue().toString())
					.setOAuthConsumerSecret(tokenNode.getChildren().get(1).getValue().toString())
					.setOAuthAccessToken(tokenNode.getChildren().get(2).getValue().toString())
					.setOAuthAccessTokenSecret(tokenNode.getChildren().get(3).getValue().toString())
					.build());
		}
		System.err.println("ADDED " + configs.size() + " tokens!");
	}
}
