package tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

		createValidTwitterClient();
	}
	// SINGLETON
	

	

	// This method will set the refreshTime variable on your current Twitter token to the Unix timestamp that corresponds to the moment when all of its endpoints are once again available for use
	public long getCurrentTokenRefreshTime() throws TwitterException {
		long allEndpointRefreshTime = 0; // The Unix timestamp in seconds when every endpoint is refreshed

		for (Map.Entry<String, RateLimitStatus> rateLimit: twitter.getRateLimitStatus().entrySet()) { // This loop sets the allEndpointRefreshTime to the Unix timestamp when this token is no longer rate-limited
			long endpointRefreshTime = 0;
			if (rateLimit.getValue().getRemaining() > 0) // If this endpoint is exhausted, its refresh time should be factored into the token's refresh time
				endpointRefreshTime = rateLimit.getValue().getResetTimeInSeconds();
			allEndpointRefreshTime = endpointRefreshTime > allEndpointRefreshTime ? endpointRefreshTime : allEndpointRefreshTime;
		}
		return allEndpointRefreshTime;
	}

	// This method is called whenever the Twitter client is detected as being invalid; it will not end until the Twitter client is valid for ALL endpoints in the List<String> endpoints variable
	public void createValidTwitterClient() {
		for (Map.Entry<String, Token> token : tokens.entrySet()) {
			if (token.getValue().isValidToken()) { // If the token is valid, create the Twitter client using it
				twitter = new TwitterFactory(token.getValue().getConfig()).getInstance();
				break;
			}
		}
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
	public void rateLimitHandler() {
		String currentToken = twitter.getConfiguration().getOAuthAccessToken();
		long currentTokenRefreshTime;
		try {
			currentTokenRefreshTime = getCurrentTokenRefreshTime();
		} catch (TwitterException e) {
			System.err.println("[ERROR]: Polling for rate-limit too often. This token will be set as invalid for 20 minutes.");
			currentTokenRefreshTime = System.currentTimeMillis()/1000 + TimeUnit.MINUTES.toSeconds(20);
		}
		tokens.get(currentToken).setRefreshTime(currentTokenRefreshTime); // Mark the time when we will be able to use this token again
		System.err.println("[INFO ]: RATE LIMITED. Marking this token as unusable until " + currentTokenRefreshTime);
		createValidTwitterClient();
	}
	public Twitter getTwitter() {
		return twitter;
	}
	
	public void printCurrentTokenRateLimits() throws TwitterException {
		for (Map.Entry<String, RateLimitStatus> rateLimit: twitter.getRateLimitStatus().entrySet()) {
			System.err.format("%-8s%40s%40s", "[INFO ]: ", rateLimit.getKey(), rateLimit.getValue().getResetTimeInSeconds());
			System.err.println();
		}

	}
}
