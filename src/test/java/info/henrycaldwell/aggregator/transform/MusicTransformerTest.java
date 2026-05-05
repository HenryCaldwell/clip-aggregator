package info.henrycaldwell.aggregator.transform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;
import info.henrycaldwell.aggregator.error.SpecException;

public class MusicTransformerTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          """);

      assertDoesNotThrow(() -> new MusicTransformer(config));
    }

    @Test
    void acceptsMixMode() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          mode = mix
          """);

      assertDoesNotThrow(() -> new MusicTransformer(config));
    }

    @Test
    void acceptsReplaceMode() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          mode = replace
          """);

      assertDoesNotThrow(() -> new MusicTransformer(config));
    }

    @Test
    void acceptsConfiguredVolume() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          volume = 0.5
          """);

      assertDoesNotThrow(() -> new MusicTransformer(config));
    }

    @Test
    void acceptsConfiguredLoop() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          loop = false
          """);

      assertDoesNotThrow(() -> new MusicTransformer(config));
    }

    @Test
    void throwsOnMissingMusicPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new MusicTransformer(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=musicPath"));
    }

    @Test
    void throwsOnWrongTypeForMusicPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = [music.mp3]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new MusicTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=musicPath"));
    }

    @Test
    void throwsOnWrongTypeForMode() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          mode = [mix]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new MusicTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=mode"));
    }

    @Test
    void throwsOnInvalidMode() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          mode = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new MusicTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=mode"));
      assertTrue(exception.getMessage().contains("value=invalid"));
    }

    @Test
    void throwsOnWrongTypeForVolume() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          volume = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new MusicTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=volume"));
    }

    @Test
    void throwsOnInvalidVolume() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          volume = -0.1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new MusicTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=volume"));
      assertTrue(exception.getMessage().contains("value=-0.1"));
    }

    @Test
    void throwsOnWrongTypeForLoop() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          loop = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new MusicTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected boolean)"));
      assertTrue(exception.getMessage().contains("key=loop"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = music.mp3
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new MusicTransformer(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Apply {

    @Test
    void throwsWhenMusicFileIsMissing() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path music = tempDir.resolve("music.mp3");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = music
          ffmpegPath = ffmpeg
          musicPath = "%s"
          """.formatted(music.toString().replace("\\", "\\\\")));
      MusicTransformer transformer = new MusicTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> transformer.apply(media));

      assertTrue(exception.getMessage().contains("Music file missing or not a regular file"));
      assertTrue(exception.getMessage().contains("musicPath=" + music));
    }
  }
}
