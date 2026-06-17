package info.henrycaldwell.streamline.stage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
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

public class AbstractStagerTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);

      assertDoesNotThrow(() -> new TestStager(config, null));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = test
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestStager(config, null));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = stager
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestStager(config, null));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestStager(config, null));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Start {

    @Test
    void doesNothingByDefault() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);
      TestStager stager = new TestStager(config, null);

      assertDoesNotThrow(stager::start);
    }
  }

  @Nested
  class Stop {

    @Test
    void doesNothingByDefault() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);
      TestStager stager = new TestStager(config, null);

      assertDoesNotThrow(stager::stop);
    }
  }

  @Nested
  class GetName {

    @Test
    void returnsConfiguredName() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);
      TestStager stager = new TestStager(config, null);

      String result = stager.getName();

      assertEquals("stager", result);
    }
  }

  @Nested
  class Stage {

    @Test
    void returnsStagedMediaAndDeletesSourceFile() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef(null, source, null);
      MediaRef staged = media.withUri(URI.create("https://example.com/source.mp4")).withFile(null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);
      TestStager stager = new TestStager(config, staged);

      MediaRef result = stager.stage(media, new CancellationToken());

      assertEquals(staged, result);
      assertFalse(Files.exists(source));
    }

    @Test
    void returnsStagedMediaWhenSourceFileIsNull() {
      MediaRef media = new MediaRef(null, null, null);
      MediaRef staged = media.withUri(URI.create("https://example.com/source.mp4"));
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);
      TestStager stager = new TestStager(config, staged);

      MediaRef result = stager.stage(media, new CancellationToken());

      assertEquals(staged, result);
    }

    @Test
    void throwsWhenApplyReturnsNullUri() {
      Path source = tempDir.resolve("source.mp4");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);
      TestStager stager = new TestStager(config, media.withUri(null));

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.stage(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Stager did not produce an HTTP(S) URI"));
      assertTrue(exception.getMessage().contains("uri=null"));
    }

    @Test
    void throwsWhenApplyReturnsNonHttpUri() {
      Path source = tempDir.resolve("source.mp4");

      MediaRef media = new MediaRef(null, source, null);
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);
      TestStager stager = new TestStager(config, media.withUri(URI.create("file:///tmp/source.mp4")));

      ComponentException exception = assertThrows(ComponentException.class,
          () -> stager.stage(media, new CancellationToken()));

      assertTrue(exception.getMessage().contains("Stager did not produce an HTTP(S) URI"));
      assertTrue(exception.getMessage().contains("uri=file:///tmp/source.mp4"));
    }
  }

  @Nested
  class Clean {

    @Test
    void doesNothingByDefault() {
      MediaRef media = new MediaRef(null, null, URI.create("https://example.com/source.mp4"));
      Config config = ConfigFactory.parseString("""
          name = stager
          type = test
          """);
      TestStager stager = new TestStager(config, null);

      assertDoesNotThrow(() -> stager.clean(media, new CancellationToken()));
    }
  }

  private static final class TestStager extends AbstractStager {

    private final MediaRef output;

    private TestStager(Config config, MediaRef output) {
      super(config, Spec.builder().build());
      this.output = output;
    }

    @Override
    protected MediaRef apply(MediaRef media, CancellationToken token) {
      return output;
    }
  }
}
