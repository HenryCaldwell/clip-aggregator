package info.henrycaldwell.aggregator.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import info.henrycaldwell.aggregator.core.MediaRef;

public class PipelineTest {

  private static final MediaRef MEDIA = new MediaRef(
      "clip-1",
      Path.of("input.mp4"),
      null,
      "Title",
      "Broadcaster",
      "en",
      null);

  @Nested
  class GetName {

    @Test
    void returnsConfiguredName() {
      Pipeline pipeline = new Pipeline("pipeline", List.of());

      String result = pipeline.getName();

      assertEquals("pipeline", result);
    }
  }

  @Nested
  class Run {

    @Test
    void returnsOriginalMediaWhenPipelineIsEmpty() {
      Pipeline pipeline = new Pipeline("pipeline", List.of());

      MediaRef result = pipeline.run(MEDIA, () -> false);

      assertSame(MEDIA, result);
    }

    @Test
    void appliesTransformersInOrder() {
      List<String> calls = new ArrayList<>();
      MediaRef firstOutput = MEDIA.withFile(Path.of("first.mp4"));
      MediaRef secondOutput = MEDIA.withFile(Path.of("second.mp4"));

      RecordingTransformer first = new RecordingTransformer("first", firstOutput, calls);
      RecordingTransformer second = new RecordingTransformer("second", secondOutput, calls);

      Pipeline pipeline = new Pipeline("pipeline", List.of(first, second));

      MediaRef result = pipeline.run(MEDIA, () -> false);

      assertEquals(secondOutput, result);
      assertEquals(List.of("first:input.mp4", "second:first.mp4"), calls);
    }

    @Test
    void passesEachTransformerThePreviousOutput() {
      MediaRef firstOutput = MEDIA.withFile(Path.of("first.mp4"));
      MediaRef secondOutput = MEDIA.withFile(Path.of("second.mp4"));

      CapturingTransformer first = new CapturingTransformer("first", firstOutput);
      CapturingTransformer second = new CapturingTransformer("second", secondOutput);

      Pipeline pipeline = new Pipeline("pipeline", List.of(first, second));

      pipeline.run(MEDIA, () -> false);

      assertSame(MEDIA, first.input());
      assertSame(firstOutput, second.input());
    }

    @Test
    void stopsBeforeFirstTransformerWhenCanceledImmediately() {
      List<String> calls = new ArrayList<>();

      Pipeline pipeline = new Pipeline("pipeline", List.of(
          new RecordingTransformer("first", MEDIA.withFile(Path.of("first.mp4")), calls)));

      MediaRef result = pipeline.run(MEDIA, () -> true);

      assertSame(MEDIA, result);
      assertEquals(List.of(), calls);
    }

    @Test
    void stopsBeforeNextTransformerWhenCanceledAfterFirstTransformer() {
      List<String> calls = new ArrayList<>();
      MediaRef firstOutput = MEDIA.withFile(Path.of("first.mp4"));

      BooleanSupplier canceled = new BooleanSupplier() {
        private int checks;

        @Override
        public boolean getAsBoolean() {
          checks++;
          return checks > 1;
        }
      };

      Pipeline pipeline = new Pipeline("pipeline", List.of(
          new RecordingTransformer("first", firstOutput, calls),
          new RecordingTransformer("second", MEDIA.withFile(Path.of("second.mp4")), calls)));

      MediaRef result = pipeline.run(MEDIA, canceled);

      assertSame(firstOutput, result);
      assertEquals(List.of("first:input.mp4"), calls);
    }
  }

  private static final class RecordingTransformer implements Transformer {

    private final String name;
    private final MediaRef output;
    private final List<String> calls;

    private RecordingTransformer(String name, MediaRef output, List<String> calls) {
      this.name = name;
      this.output = output;
      this.calls = calls;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public MediaRef transform(MediaRef media) {
      calls.add(name + ":" + media.file().getFileName());
      return output;
    }
  }

  private static final class CapturingTransformer implements Transformer {

    private final String name;
    private final MediaRef output;
    private MediaRef input;

    private CapturingTransformer(String name, MediaRef output) {
      this.name = name;
      this.output = output;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public MediaRef transform(MediaRef media) {
      input = media;
      return output;
    }

    private MediaRef input() {
      return input;
    }
  }
}
