package tokens;

import java.util.List;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class Token {
	private String[] credentials = new String[4]; // The 4 Twitter token credentials
	private Configuration config; // The actual Configuration object that we want
	private long refreshTime = 0; // The Unix timestamp of when we can use this token again
	
	public Token(String[] credentials) {
		this.credentials = credentials;
		createConfiguration();
	}
	public Token(String cred1, String cred2, String cred3, String cred4) {
		credentials[0] = cred1;
		credentials[1] = cred2;
		credentials[2] = cred3;
		credentials[3] = cred4;
		createConfiguration();
	}
	public Token(List<String> credentials) {
		for (int i = 0; i < this.credentials.length; i++) {
			this.credentials[i] = credentials.get(i);
		}
		createConfiguration();
	}
	
	public void createConfiguration() {
		config = new ConfigurationBuilder()
		.setDebugEnabled(true)
		.setPrettyDebugEnabled(true)
		.setOAuthConsumerKey(credentials[0])
		.setOAuthConsumerSecret(credentials[1])
		.setOAuthAccessToken(credentials[2])
		.setOAuthAccessTokenSecret(credentials[3])
		.build();
	}

	public void setRefreshTime(long refreshTime) {
		this.refreshTime = refreshTime;
	}
	public long getRefreshTime() {
		return refreshTime;
	}
	public boolean isValidToken() { // Returns true if the current time is greater than the refresh time (indicating that this token is refreshed)
		return System.currentTimeMillis()/1000 > refreshTime;
	}
	public Configuration getConfig() {
		return config;
	}
	
}
