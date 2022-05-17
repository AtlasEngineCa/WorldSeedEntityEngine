import net.worldseed.multipart.parser.ModelParser;

import javax.naming.SizeLimitExceededException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class CreatePack {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, SizeLimitExceededException {
        ModelParser.parse();
    }
}
