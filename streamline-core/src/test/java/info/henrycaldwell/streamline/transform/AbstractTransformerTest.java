package info.henrycaldwell.streamline.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;

public class AbstractTransformerTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);

      assertDoesNotThrow(() -> new TestTransformer(config, null));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = test
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestTransformer(config, null));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestTransformer(config, null));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestTransformer(config, null));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class GetName {

    @Test
    void returnsConfiguredName() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);
      TestTransformer transformer = new TestTransformer(config, null);

      String result = transformer.getName();

      assertEquals("transformer", result);
    }
  }

  @Nested
  class Transform {

    @Test
    void replacesOriginalFileWithOutputFile() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path output = tempDir.resolve("output.mp4");
      Files.writeString(source, "source");
      Files.writeString(output, "output");

      MediaRef media = new MediaRef(null, source, null);
      MediaRef applied = media.withFile(output);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);
      TestTransformer transformer = new TestTransformer(config, applied);

      MediaRef result = transformer.transform(media, new CancellationToken());

      assertEquals(source, result.file());
      assertEquals("output", Files.readString(source));
      assertFalse(Files.exists(output));
    }

    @Test
    void throwsWhenSourceFileIsNull() throws IOException {
      Path output = tempDir.resolve("output.mp4");
      Files.writeString(output, "output");

      MediaRef media = new MediaRef(null, null, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);
      TestTransformer transformer = new TestTransformer(config, media.withFile(output));

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.transform(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Transformer did not produce a new output file"));
      assertTrue(exception.getMessage().contains("sourcePath=null"));
      assertTrue(exception.getMessage().contains("outputPath=" + output));
    }

    @Test
    void throwsWhenApplyReturnsNullOutputFile() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);
      TestTransformer transformer = new TestTransformer(config, media.withFile(null));

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.transform(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Transformer did not produce a new output file"));
      assertTrue(exception.getMessage().contains("sourcePath=" + source));
      assertTrue(exception.getMessage().contains("outputPath=null"));
    }

    @Test
    void throwsWhenApplyReturnsSourceFile() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);
      TestTransformer transformer = new TestTransformer(config, media);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.transform(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Transformer did not produce a new output file"));
      assertTrue(exception.getMessage().contains("sourcePath=" + source));
      assertTrue(exception.getMessage().contains("outputPath=" + source));
    }

    @Test
    void throwsWhenApplyReturnsMissingOutputFile() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path output = tempDir.resolve("output.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);
      TestTransformer transformer = new TestTransformer(config, media.withFile(output));

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.transform(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Transformer produced a non-regular output file"));
      assertTrue(exception.getMessage().contains("outputPath=" + output));
    }

    @Test
    void throwsWhenApplyReturnsDirectoryOutputFile() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path output = tempDir.resolve("output.mp4");
      Files.writeString(source, "source");
      Files.createDirectory(output);

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = test
          """);
      TestTransformer transformer = new TestTransformer(config, media.withFile(output));

      ComponentException exception = assertThrows(ComponentException.class,
          () -> transformer.transform(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Transformer produced a non-regular output file"));
      assertTrue(exception.getMessage().contains("outputPath=" + output));
    }
  }

  private static final class TestTransformer extends AbstractTransformer {

    private final MediaRef output;

    private TestTransformer(Config config, MediaRef output) {
      super(config, Spec.builder().build());
      this.output = output;
    }

    @Override
    protected MediaRef apply(MediaRef media, CancellationToken token) {
      return output;
    }
  }
}
