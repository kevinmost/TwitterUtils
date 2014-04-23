import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import tokens.TokenHandler;

/**
 * @author kmost
 * @date 4/23/14
 */
public class TwitterTester  {

    private static final Logger logger = Logger.getLogger(TwitterTester.class.getName());


    public static void main(String[] args) {
        PropertyConfigurator.configure("log4j.properties"); // Load log4j's properties

        TokenHandler tokens = new TokenHandler("res/tokens.xml");



    }
}
