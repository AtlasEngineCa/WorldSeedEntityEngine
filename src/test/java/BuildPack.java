import net.worldseed.resourcepack.PackBuilder;
import org.apache.commons.io.FileUtils;

private static final Path BASE_PATH = Path.of("src/test/resources");
private static final Path MODEL_PATH = BASE_PATH.resolve("models");

void main() throws Exception {
    FileUtils.copyDirectory(BASE_PATH.resolve("resourcepack_template").toFile(), BASE_PATH.resolve("resourcepack").toFile());
    var config = PackBuilder.generate(BASE_PATH.resolve("bbmodel"), BASE_PATH.resolve("resourcepack"), MODEL_PATH);
    FileUtils.writeStringToFile(BASE_PATH.resolve("model_mappings.json").toFile(), config.modelMappings(), Charset.defaultCharset());
}
