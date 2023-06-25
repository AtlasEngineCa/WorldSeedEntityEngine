import net.worldseed.resourcepack.PackBuilder;
import org.apache.commons.io.FileUtils;

import java.nio.charset.Charset;
import java.nio.file.Path;

public class BuildPack {
    private static final Path BASE_PATH = Path.of("src/test/resources");
    private static final Path MODEL_PATH = BASE_PATH.resolve("models");

    public static void main(String[] args) throws Exception {
        FileUtils.copyDirectory(BASE_PATH.resolve("resourcepack_template").toFile(), BASE_PATH.resolve("resourcepack").toFile());
        var config = PackBuilder.Generate(BASE_PATH.resolve("bbmodel"), BASE_PATH.resolve("resourcepack"), MODEL_PATH);
        FileUtils.writeStringToFile(BASE_PATH.resolve("model_mappings.json").toFile(), config.modelMappings(), Charset.defaultCharset());
    }
}
