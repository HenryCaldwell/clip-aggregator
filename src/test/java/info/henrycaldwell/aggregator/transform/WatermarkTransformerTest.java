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

public class WatermarkTransformerTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredLogoPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoPath = logo.png
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsUpperCenterPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          position = upper_center
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsLowerCenterPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          position = lower_center
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsCenterPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          position = center
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredFontColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          fontColor = yellow
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredBorderColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          borderColor = blue
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredFontSize() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          fontSize = 48
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredTextOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textOpacity = 0.8
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredTextBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textBorderWidth = 2
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredTextOffsetX() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textOffsetX = 10
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredTextOffsetY() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textOffsetY = -10
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredLogoHeight() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoHeight = 120
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredLogoOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoOpacity = 0.5
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredLogoOffsetX() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoOffsetX = 12
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void acceptsConfiguredLogoOffsetY() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoOffsetY = -12
          """);

      assertDoesNotThrow(() -> new WatermarkTransformer(config));
    }

    @Test
    void throwsOnMissingFontPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=fontPath"));
    }

    @Test
    void throwsOnWrongTypeForFontPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = [font.ttf]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=fontPath"));
    }

    @Test
    void throwsOnWrongTypeForLogoPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoPath = [logo.png]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=logoPath"));
    }

    @Test
    void throwsOnWrongTypeForPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          position = [center]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=position"));
    }

    @Test
    void throwsOnInvalidPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          position = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=position"));
      assertTrue(exception.getMessage().contains("value=invalid"));
    }

    @Test
    void throwsOnWrongTypeForFontColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          fontColor = [white]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=fontColor"));
    }

    @Test
    void throwsOnWrongTypeForBorderColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          borderColor = [black]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=borderColor"));
    }

    @Test
    void throwsOnWrongTypeForFontSize() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          fontSize = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=fontSize"));
    }

    @Test
    void throwsOnInvalidFontSize() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          fontSize = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=fontSize"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForTextOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textOpacity = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=textOpacity"));
    }

    @Test
    void throwsOnInvalidTextOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textOpacity = -0.1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=textOpacity"));
      assertTrue(exception.getMessage().contains("value=-0.1"));
    }

    @Test
    void throwsOnWrongTypeForTextBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textBorderWidth = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=textBorderWidth"));
    }

    @Test
    void throwsOnInvalidTextBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textBorderWidth = -1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=textBorderWidth"));
      assertTrue(exception.getMessage().contains("value=-1"));
    }

    @Test
    void throwsOnWrongTypeForTextOffsetX() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textOffsetX = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=textOffsetX"));
    }

    @Test
    void throwsOnWrongTypeForTextOffsetY() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          textOffsetY = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=textOffsetY"));
    }

    @Test
    void throwsOnWrongTypeForLogoHeight() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoHeight = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=logoHeight"));
    }

    @Test
    void throwsOnInvalidLogoHeight() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoHeight = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=logoHeight"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForLogoOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoOpacity = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=logoOpacity"));
    }

    @Test
    void throwsOnInvalidLogoOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoOpacity = -0.1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=logoOpacity"));
      assertTrue(exception.getMessage().contains("value=-0.1"));
    }

    @Test
    void throwsOnWrongTypeForLogoOffsetX() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoOffsetX = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=logoOffsetX"));
    }

    @Test
    void throwsOnWrongTypeForLogoOffsetY() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoOffsetY = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=logoOffsetY"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new WatermarkTransformer(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Apply {

    @Test
    void throwsWhenBroadcasterIsNull() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", null, "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          """);
      WatermarkTransformer transformer = new WatermarkTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> transformer.apply(media));

      assertTrue(exception.getMessage().contains("Broadcaster name missing"));
      assertTrue(exception.getMessage().contains("clipId=clip-1"));
      assertTrue(exception.getMessage().contains("broadcaster=null"));
    }

    @Test
    void throwsWhenBroadcasterIsBlank() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", " ", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          """);
      WatermarkTransformer transformer = new WatermarkTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> transformer.apply(media));

      assertTrue(exception.getMessage().contains("Broadcaster name missing"));
      assertTrue(exception.getMessage().contains("clipId=clip-1"));
      assertTrue(exception.getMessage().contains("broadcaster= "));
    }

    @Test
    void throwsWhenBroadcasterIsEmptyAfterFiltering() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "\uD83D\uDE00", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          """);
      WatermarkTransformer transformer = new WatermarkTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> transformer.apply(media));

      assertTrue(exception.getMessage().contains("Broadcaster name empty after filtering"));
      assertTrue(exception.getMessage().contains("clipId=clip-1"));
      assertTrue(exception.getMessage().contains("broadcaster=\uD83D\uDE00"));
    }

    @Test
    void throwsWhenLogoFileIsMissing() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Path logo = tempDir.resolve("logo.png");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = watermark
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          logoPath = "%s"
          """.formatted(logo.toString().replace("\\", "\\\\")));
      WatermarkTransformer transformer = new WatermarkTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> transformer.apply(media));

      assertTrue(exception.getMessage().contains("Logo file missing or not a regular file"));
      assertTrue(exception.getMessage().contains("clipId=clip-1"));
      assertTrue(exception.getMessage().contains("logoPath=" + logo));
    }
  }
}
