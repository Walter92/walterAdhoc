import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by walter on 16-11-2.
 */
public class LoggerTest {
//    private static Logger logger = LoggerFactory.getLogger(LoggerTest.class);
    @Test
    public void testLog(){
      //  logger.debug("test logg is {}",new Object());
//        Integer a = Integer.valueOf("21",16);
//        System.out.println(a);
//        Date date = new Date();
//        SimpleDateFormat
//        System.out.println(date);
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(simpleDateFormat.format(date));
    }



}
