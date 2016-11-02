import cn.edu.uestc.Adhoc.entity.factory.AdhocNodeFactory;
import org.junit.Test;

/**
 * Created by walter on 15-12-18.
 */
public class FactoryTest {
//    @Test
    public void testFactory(){
        System.out.println(AdhocNodeFactory.getInstance("usb0"));
    }
}
