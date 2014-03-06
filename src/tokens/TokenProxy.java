package tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;

import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

// Abstracts the Token objects away into one well-behaving TokenProxy. This TokenProxy exposes a Twitter client to the rest of the application that will attempt to alleviate rate-limiting issues.
public class TokenProxy {
	private Twitter twitter = null; // Our Twitter client
	private XMLConfiguration config = null; // Our XML configuration
	private Map<String, Token> tokens = new HashMap<>(); // Our tokens pulled from the XML configuration
	private long resetTime = 0; // If all of our tokens are invalid, we will be using this variable to determine how long the program should sleep
	// SINGLETON
	// USAGE: First call of this class must use getTokenProxy(String filename), where you supply a config.xml as the filename parameter
	// Any subsequent calls of this class can use the getTokenProxy() convenience method
	private static TokenProxy instance;
	public static synchronized TokenProxy getTokenProxy() { // This method can be called for convenience's sake without passing an XML config file, but you have to have first called the method below earlier in the program to initialize the config
		if (instance == null)
			System.err.println("[ERROR]: No config set yet. Please call .getTokenProxy(String xmlConfig) with a filename for an XML config file before calling it with no parameters.");
		return instance;
	}
	public static synchronized TokenProxy getTokenProxy(String filename) throws ConfigurationException { // Make sure the first time that you call this class, you are using this version of the method (with the config's filepath passed as a parameter)
		if (instance == null)
			instance = new TokenProxy(filename);
		return instance;
	}
	public Object clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
	private TokenProxy(String filename) throws ConfigurationException { // Constructor
		config = new XMLConfiguration(filename);

		for (ConfigurationNode tokenNode : config.getRoot().getChildren("token")) { // Take each "token" node in the XML and, one by one, use their data to create new Token objects.
			List<String> tokenCredentials = new ArrayList<>();
			for (ConfigurationNode node : tokenNode.getChildren()) {
				tokenCredentials.add(node.getValue().toString());
			}
			tokens.put(tokenCredentials.get(2), new Token(tokenCredentials)); // Adds the token to a Map where its key is the "oauth-access-token" node
		}

		for (Map.Entry<String, Token> token : tokens.entrySet()) {
			System.err.println("Setting refresh-time for " + token.getKey());
			token.getValue().setRefreshTime(getTokenRefreshTime(token.getValue()));
		}
	}
	// SINGLETON
	

	

	// This method will set the refreshTime variable on your current Twitter token to the Unix timestamp that corresponds to the moment when all of its endpoints are once again available for use
	private long getTokenRefreshTime() {
		long allEndpointRefreshTime = 0; // The Unix timestamp in seconds when every endpoint is refreshed
		Set<Map.Entry<String, RateLimitStatus>> temp = new HashSet<>();
		try {
			temp = twitter.getRateLimitStatus().entrySet();
		}
		catch (TwitterException te) {
			System.err.println("[INFO ]: Can't get rate-limit statuses. Setting token to expired for 20 mins.");
			allEndpointRefreshTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + TimeUnit.MINUTES.toSeconds(20);
		}
		for (Map.Entry<String, RateLimitStatus> rateLimit: temp) { // This loop sets the allEndpointRefreshTime to the Unix timestamp when this token is no longer rate-limited
			long endpointRefreshTime = 0;
			if (rateLimit.getValue().getRemaining() == 0) // If this endpoint is exhausted, its refresh time should be factored into the token's refresh time
				endpointRefreshTime = rateLimit.getValue().getResetTimeInSeconds();
			allEndpointRefreshTime = endpointRefreshTime > allEndpointRefreshTime ? endpointRefreshTime : allEndpointRefreshTime;
		}
		return allEndpointRefreshTime;
	}

	private long getTokenRefreshTime(Token token) {
		twitter = new TwitterFactory(token.getConfig()).getInstance();
		return getTokenRefreshTime();
	}
	
	// This method is called whenever the Twitter client is detected as being invalid; it will not end until the Twitter client is valid for ALL endpoints in the List<String> endpoints variable
	private Twitter getValidTwitterClient() {
		for (Map.Entry<String, Token> token : tokens.entrySet()) {
			if (token.getValue().isValidToken()) { // If the token is valid, create the Twitter client using it
				System.err.println("[INFO ]: NOW USING token " + token.getKey());
				return new TwitterFactory(token.getValue().getConfig()).getInstance();
			}
			else { // If the token is not valid, then we are going to log when it is valid again, so that if every token happens to be exhausted, we can know when there will be a valid token once again.
				long curr = token.getValue().getRefreshTime();
				resetTime = curr > resetTime ? curr : resetTime;
			}
		}
		// If we get to this point in the code, that means none of our tokens were valid, so we need to sleep for a bit
		pauseBar(resetTime);
		return getValidTwitterClient();
	}

	// The user should invoke this method in their catch block so that TokenProxy can gracefully handle the error
	public void exceptionHandler(Exception e) {
		if (e.getClass() == TwitterException.class) {
			TwitterException te = ((TwitterException)e);
			if (te.getErrorCode() == 88) { // This token is rate-limited. We will mark its refresh time in its Token object and move on
				rateLimitHandler();
			}
		}
	}
	
	// exceptionHandler(Exception e) will invoke this method when it detects that the exception that it received was due to a Twitter token rate-limit
	private void rateLimitHandler() {
		String currentToken = twitter.getConfiguration().getOAuthAccessToken();
		long currentTokenRefreshTime = getTokenRefreshTime();

		tokens.get(currentToken).setRefreshTime(currentTokenRefreshTime); // Mark the time when we will be able to use this token again
		System.err.println("[INFO ]: RATE-LIMITED on token " + currentToken + ". Getting new token..."); 
		twitter = getValidTwitterClient(); // Get another new token from our pool of tokens
	}
	
	public Twitter getTwitter() {
		return twitter;
	}
	private void printCurrentTokenRateLimits() throws TwitterException {
		for (Map.Entry<String, RateLimitStatus> rateLimit: twitter.getRateLimitStatus().entrySet()) {
			System.err.format("%-8s%40s%40s", "[INFO ]: ", rateLimit.getKey(), rateLimit.getValue().getResetTimeInSeconds());
			System.err.println();
		}

	}
	// Makes the program pause for resetTime seconds, while displaying a linearly-filling progress-bar
	public static void pauseBar(long resetTime) {
		resetTime -= TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()); // The difference in these Unix timestamps is the amount of time that we must sleep
		System.err.println("[INFO ]: All tokens are rate-limited. Program will now sleep for " + resetTime + " seconds to allow tokens to refresh.");
		// This bar will actually appear to "fill up" when the program is invoked from the console. However, Eclipse's console is quirky so it will simply display the progress bar multiple times.
		StringBuilder statusBar = new StringBuilder("|"); // Change the left-hand border of the progress bar
		String statusBarSegment = "="; // Change the "segments" used by the bar
		String statusBarArrow = ">"; // Change the "arrow" that appears at the end of the "currently-filled" section of the bar
		String statusBarEnd = "|"; // Change the right-hand border of the progress bar
		long numSegments = 50; // Change the number of segments shown in the progress bar
		
		// Do not modify these variables
		long whitespace = 1;
		long sleepTime = resetTime/numSegments; // "resetTime" is the amount of time the program should display this bar for in seconds (ex, resetTime = 60 would make this bar take 60 seconds to fill up)
		
		while ((whitespace = numSegments - statusBarEnd.length() - statusBar.append(statusBarSegment).length()) > 0) { // Runs until all empty space is filled
			try {
				TimeUnit.SECONDS.sleep(sleepTime); // Pauses long enough to represent one segment of the bar
			} catch(InterruptedException ie) {
				System.err.println("[ERROR]: Sleep was interrupted. Program will attempt to continue.");
			}
			System.err.format("%-" + statusBar.length() + "s%-" + whitespace + "s%-" + statusBarEnd.length() + "s", statusBar.toString(), statusBarArrow, statusBarEnd + "\r"); // Prints the entire bar, formatted properly
		}
		resetTime = 0;
	}
}
