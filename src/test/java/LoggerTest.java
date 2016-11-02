import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by walter on 16-11-2.
 */
public class LoggerTest {
    private static Logger logger = LoggerFactory.getLogger(LoggerTest.class);
    @Test
    public void testLog(){
        logger.debug("test logg is {}",new Object());
    }
}
