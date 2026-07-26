package info.henrycaldwell.streamline.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationReason;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.RunSession;
import info.henrycaldwell.streamline.observe.AbstractObserver;
import info.henrycaldwell.streamline.observe.AttemptStatus;
import info.henrycaldwell.streamline.observe.PipelineStage;
import info.henrycaldwell.streamline.observe.RunStatus;

public class PipelineTest {

  private static final MediaRef MEDIA = new MediaRef(null, Path.of("input.mp4"), null);

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

      MediaRef result = pipeline.run(MEDIA, null, new RunSession(0L, new CancellationToken()), "worker");

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

      MediaRef result = pipeline.run(MEDIA, null, new RunSession(0L, new CancellationToken()), "worker");

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

      pipeline.run(MEDIA, null, new RunSession(0L, new CancellationToken()), "worker");

      assertSame(MEDIA, first.input());
      assertSame(firstOutput, second.input());
    }

    @Test
    void stopsBeforeFirstTransformerWhenCanceledImmediately() {
      List<String> calls = new ArrayList<>();

      Pipeline pipeline = new Pipeline("pipeline", List.of(
          new RecordingTransformer("first", MEDIA.withFile(Path.of("first.mp4")), calls)));

      CancellationToken token = new CancellationToken();
      token.cancel(CancellationReason.USER_CANCELED);
      MediaRef result = pipeline.run(MEDIA, null, new RunSession(0L, token), "worker");

      assertSame(MEDIA, result);
      assertEquals(List.of(), calls);
    }

    @Test
    void stopsBeforeNextTransformerWhenCanceledAfterFirstTransformer() {
      List<String> calls = new ArrayList<>();
      MediaRef firstOutput = MEDIA.withFile(Path.of("first.mp4"));

      CancellationToken token = new CancellationToken();

      Pipeline pipeline = new Pipeline("pipeline", List.of(
          new CancelingTransformer("first", firstOutput, calls),
          new RecordingTransformer("second", MEDIA.withFile(Path.of("second.mp4")), calls)));

      MediaRef result = pipeline.run(MEDIA, null, new RunSession(0L, token), "worker");

      assertSame(firstOutput, result);
      assertEquals(List.of("first:input.mp4"), calls);
    }

    @Test
    void recordsSuccessOnTransformerSuccess() {
      RecordingObserver observer = new RecordingObserver();
      Pipeline pipeline = new Pipeline("pipeline", List.of(
          new RecordingTransformer("transformer", MEDIA.withFile(Path.of("out.mp4")), new ArrayList<>())));

      pipeline.run(MEDIA, observer, new RunSession(42L, new CancellationToken()), "worker");

      assertEquals(1, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(0).status());
    }

    @Test
    void recordsFailureOnTransformerFailure() {
      RecordingObserver observer = new RecordingObserver();
      RuntimeException error = new RuntimeException("transformer failed");
      ThrowingTransformer first = new ThrowingTransformer("first", error);
      RecordingTransformer second = new RecordingTransformer("second", MEDIA.withFile(Path.of("second.mp4")),
          new ArrayList<>());
      Pipeline pipeline = new Pipeline("pipeline", List.of(first, second));

      RuntimeException thrown = assertThrows(RuntimeException.class,
          () -> pipeline.run(MEDIA, observer, new RunSession(42L, new CancellationToken()), "worker"));

      assertSame(error, thrown);
      assertEquals(1, observer.startedAttempts.size());
      assertEquals("first", observer.startedAttempts.get(0).component());
      assertEquals(1, observer.endedAttempts.size());
      assertEquals(AttemptStatus.FAILURE, observer.endedAttempts.get(0).status());
      assertSame(error, observer.endedAttempts.get(0).error());
    }

    @Test
    void skipsAttemptForCanceledTransformer() {
      RecordingObserver observer = new RecordingObserver();
      List<String> calls = new ArrayList<>();
      MediaRef firstOutput = MEDIA.withFile(Path.of("first.mp4"));

      CancellationToken token = new CancellationToken();

      Pipeline pipeline = new Pipeline("pipeline", List.of(
          new CancelingTransformer("first", firstOutput, calls),
          new RecordingTransformer("second", MEDIA.withFile(Path.of("second.mp4")), calls)));

      pipeline.run(MEDIA, observer, new RunSession(42L, token), "worker");

      assertEquals(1, observer.startedAttempts.size());
      assertEquals("first", observer.startedAttempts.get(0).component());
      assertEquals(1, observer.endedAttempts.size());
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
    public MediaRef transform(MediaRef media, CancellationToken token) {
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
    public MediaRef transform(MediaRef media, CancellationToken token) {
      input = media;
      return output;
    }

    private MediaRef input() {
      return input;
    }
  }

  private static final class ThrowingTransformer implements Transformer {

    private final String name;
    private final RuntimeException error;

    private ThrowingTransformer(String name, RuntimeException error) {
      this.name = name;
      this.error = error;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public MediaRef transform(MediaRef media, CancellationToken token) {
      throw error;
    }
  }

  private static final class CancelingTransformer implements Transformer {

    private final String name;
    private final MediaRef output;
    private final List<String> calls;

    private CancelingTransformer(String name, MediaRef output, List<String> calls) {
      this.name = name;
      this.output = output;
      this.calls = calls;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public MediaRef transform(MediaRef media, CancellationToken token) {
      calls.add(name + ":" + media.file().getFileName());
      token.cancel(CancellationReason.USER_CANCELED);
      return output;
    }
  }

  private static final class RecordingObserver extends AbstractObserver {

    record StartedAttempt(long runId, ClipRef clip, PipelineStage stage, String component, String worker) {
    }

    record EndedAttempt(long attemptId, AttemptStatus status, Throwable error) {
    }

    private final List<StartedAttempt> startedAttempts = new ArrayList<>();
    private final List<EndedAttempt> endedAttempts = new ArrayList<>();
    private long nextId = 1;

    private RecordingObserver() {
      super(ConfigFactory.parseString("""
          name = recording
          type = recording
          """), Spec.builder().build());
    }

    @Override
    public long runStart(String runner, String config) {
      return nextId++;
    }

    @Override
    public void runEnd(long runId, RunStatus status, int published) {
    }

    @Override
    public long fetchStart(long runId, String retriever, String worker) {
      return 0L;
    }

    @Override
    public void fetchEnd(long fetchId, AttemptStatus status, int clips, Throwable error) {
    }

    @Override
    public long attemptStart(long runId, ClipRef clip, PipelineStage stage, String component, String worker) {
      startedAttempts.add(new StartedAttempt(runId, clip, stage, component, worker));
      return nextId++;
    }

    @Override
    public void attemptEnd(long attemptId, AttemptStatus status, Throwable error) {
      endedAttempts.add(new EndedAttempt(attemptId, status, error));
    }

    @Override
    public void publish(long runId, String clipId, String publisher, String uri) {
    }
  }
}
