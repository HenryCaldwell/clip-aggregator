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

public class TextTransformerTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredText() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Configured text"
          targetWidth = 800
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsTopLeftPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = top_left
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsTopRightPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = top_right
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsBottomLeftPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = bottom_left
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsBottomRightPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = bottom_right
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsTopCenterPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = top_center
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsBottomCenterPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = bottom_center
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsCenterPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = center
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsLeftTextAlign() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textAlign = left
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsCenterTextAlign() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textAlign = center
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsRightTextAlign() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textAlign = right
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredFontColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          fontColor = yellow
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredBorderColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          borderColor = blue
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredBoxColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          boxColor = white
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredTargetWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 1200
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredFontSize() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          fontSize = 48
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredTextOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textOpacity = 0.8
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredTextBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textBorderWidth = 2
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredTextOffsetX() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textOffsetX = 10
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredTextOffsetY() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textOffsetY = -10
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredLineSpacing() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          lineSpacing = 12
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredMaxLines() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          maxLines = 2
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredBoxOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          boxOpacity = 0.5
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void acceptsConfiguredBoxBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          boxBorderWidth = 4
          """);

      assertDoesNotThrow(() -> new TextTransformer(config));
    }

    @Test
    void throwsOnMissingFontPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          text = "Hello, World!"
          targetWidth = 800
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=fontPath"));
    }

    @Test
    void throwsOnWrongTypeForFontPath() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = [font.ttf]
          text = "Hello, World!"
          targetWidth = 800
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=fontPath"));
    }

    @Test
    void throwsOnMissingText() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          targetWidth = 800
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=text"));
    }

    @Test
    void throwsOnWrongTypeForText() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = [hello]
          targetWidth = 800
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=text"));
    }

    @Test
    void throwsOnInvalidText() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = " "
          targetWidth = 800
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=text"));
    }

    @Test
    void throwsOnWrongTypeForPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = [center]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=position"));
    }

    @Test
    void throwsOnInvalidPosition() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          position = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=position"));
      assertTrue(exception.getMessage().contains("value=invalid"));
    }

    @Test
    void throwsOnWrongTypeForTextAlign() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textAlign = [center]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=textAlign"));
    }

    @Test
    void throwsOnInvalidTextAlign() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textAlign = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=textAlign"));
      assertTrue(exception.getMessage().contains("value=invalid"));
    }

    @Test
    void throwsOnWrongTypeForFontColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          fontColor = [white]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=fontColor"));
    }

    @Test
    void throwsOnWrongTypeForBorderColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          borderColor = [black]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=borderColor"));
    }

    @Test
    void throwsOnWrongTypeForBoxColor() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          boxColor = [black]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=boxColor"));
    }

    @Test
    void throwsOnMissingTargetWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=targetWidth"));
    }

    @Test
    void throwsOnWrongTypeForTargetWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=targetWidth"));
    }

    @Test
    void throwsOnInvalidTargetWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=targetWidth"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForFontSize() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          fontSize = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=fontSize"));
    }

    @Test
    void throwsOnInvalidFontSize() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          fontSize = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=fontSize"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForTextOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textOpacity = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=textOpacity"));
    }

    @Test
    void throwsOnInvalidTextOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textOpacity = -0.1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=textOpacity"));
      assertTrue(exception.getMessage().contains("value=-0.1"));
    }

    @Test
    void throwsOnWrongTypeForTextBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textBorderWidth = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=textBorderWidth"));
    }

    @Test
    void throwsOnInvalidTextBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textBorderWidth = -1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=textBorderWidth"));
      assertTrue(exception.getMessage().contains("value=-1"));
    }

    @Test
    void throwsOnWrongTypeForTextOffsetX() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textOffsetX = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=textOffsetX"));
    }

    @Test
    void throwsOnWrongTypeForTextOffsetY() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          textOffsetY = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=textOffsetY"));
    }

    @Test
    void throwsOnWrongTypeForLineSpacing() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          lineSpacing = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=lineSpacing"));
    }

    @Test
    void throwsOnWrongTypeForMaxLines() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          maxLines = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=maxLines"));
    }

    @Test
    void throwsOnInvalidMaxLines() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          maxLines = 0
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=maxLines"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnWrongTypeForBoxOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          boxOpacity = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=boxOpacity"));
    }

    @Test
    void throwsOnInvalidBoxOpacity() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          boxOpacity = -0.1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=boxOpacity"));
      assertTrue(exception.getMessage().contains("value=-0.1"));
    }

    @Test
    void throwsOnWrongTypeForBoxBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          boxBorderWidth = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=boxBorderWidth"));
    }

    @Test
    void throwsOnInvalidBoxBorderWidth() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          boxBorderWidth = -1
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=boxBorderWidth"));
      assertTrue(exception.getMessage().contains("value=-1"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "Hello, World!"
          targetWidth = 800
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TextTransformer(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Apply {

    @Test
    void throwsWhenTextIsEmptyAfterFormatting() throws IOException {
      Path source = tempDir.resolve("source.mp4");
      Files.writeString(source, "source");

      MediaRef media = new MediaRef("clip-1", source, null, "Title", "Broadcaster", "en", null);
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = text
          ffmpegPath = ffmpeg
          fontPath = font.ttf
          text = "\uD83D\uDE00"
          targetWidth = 800
          """);
      TextTransformer transformer = new TextTransformer(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> transformer.apply(media));

      assertTrue(exception.getMessage().contains("Text empty after formatting"));
      assertTrue(exception.getMessage().contains("clipId=clip-1"));
      assertTrue(exception.getMessage().contains("text=\uD83D\uDE00"));
      assertTrue(exception.getMessage().contains("formattedText="));
    }
  }
}
