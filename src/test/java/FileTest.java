import java.io.File;

/**
 * Created by walter on 17-5-4.
 */
public class FileTest {
    public  static void main(String[] args){
        File file = new File("/home/walter/IdeaProjects/WalterAdhoc/src/test/java/FileTest.java");
        if(file.exists()&&file.isFile())
        System.out.println(file.length());
    }
}
