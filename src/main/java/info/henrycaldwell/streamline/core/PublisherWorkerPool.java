package info.henrycaldwell.streamline.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.henrycaldwell.streamline.observe.AttemptStatus;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.observe.PipelineStage;
import info.henrycaldwell.streamline.publish.Publisher;

/**
 * Class for managing a pool of publisher worker threads.
 *
 * This class coordinates concurrent publishing and cleaning of media.
 */
public final class PublisherWorkerPool {

  private static final Logger LOG = LoggerFactory.getLogger(PublisherWorkerPool.class);
  private static final MediaRef SENTINEL = new MediaRef(null, null, null);

  private final RunnerContext context;
  private final RunSession session;
  private final LinkedBlockingQueue<MediaRef> queue;
  private final AtomicInteger reserved;
  private final AtomicInteger pending;
  private final AtomicInteger published;
  private final AtomicInteger failures;
  private final List<Thread> threads;

  /**
   * Constructs a PublisherWorkerPool.
   *
   * @param context A {@link RunnerContext} representing the configured
   *                components.
   * @param session A {@link RunSession} representing the state of the run.
   */
  public PublisherWorkerPool(RunnerContext context, RunSession session) {
    this.context = context;
    this.session = session;
    this.queue = new LinkedBlockingQueue<>(context.publisherThreads() * 2);
    this.reserved = new AtomicInteger(0);
    this.pending = new AtomicInteger(0);
    this.published = new AtomicInteger(0);
    this.failures = new AtomicInteger(0);
    this.threads = new ArrayList<>();
  }

  /**
   * Initializes the configured publisher worker threads.
   */
  public void start() {
    for (int i = 0; i < context.publisherThreads(); i++) {
      int index = i + 1;
      Thread thread = new Thread(() -> run());
      thread.setName("publisher-worker-" + index);
      thread.start();
      threads.add(thread);
    }
  }

  /**
   * Releases and cleans up the configured publisher worker threads.
   */
  public void stop() {
    for (int i = 0; i < threads.size(); i++) {
      try {
        queue.put(SENTINEL);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Failed to send sentinel (runner={})", context.name(), e);
      }
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Failed to stop publisher thread (runner={}, thread={})",
            context.name(), thread.getName(), e);
      }
    }
  }

  /**
   * Submits the input media to the publish queue.
   * 
   * @param media A {@link MediaRef} representing the media to publish.
   */
  public void submit(MediaRef media) {
    try {
      queue.put(media);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.error("Failed to submit to publish queue (runner={})", context.name(), e);
    }
  }

  /**
   * Publishes clips from the publish queue.
   */
  private void run() {
    String worker = Thread.currentThread().getName();
    Observer observer = context.observer();
    long runId = session.runId();
    CancellationToken token = session.token();

    while (true) {
      MediaRef media;
      try {
        media = queue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Failed to poll from publish queue (runner={}, thread={})", context.name(), worker, e);
        break;
      }

      if (media == SENTINEL) {
        LOG.info("Stopped publisher thread (runner={}, thread={})", context.name(), worker);
        break;
      }

      if (failures.get() >= context.failureLimit()) {
        clean(media, token);
        continue;
      }

      int slot = reserved.getAndIncrement();
      if (slot >= context.posts()) {
        reserved.decrementAndGet();

        while (pending.get() > 0) {
          Thread.onSpinWait();
        }

        if (published.get() < context.posts()) {
          clean(media, token);
          continue;
        }

        clean(media, token);
        continue;
      }

      pending.incrementAndGet();

      ClipRef clip = media.clip();
      String clipId = clip.id();
      boolean success = false;

      for (Publisher publisher : context.publishers().values()) {
        String publisherName = publisher.getName();

        long publishAttemptId = -1;
        if (observer != null) {
          publishAttemptId = observer.attemptStart(runId, worker, clip, PipelineStage.PUBLISH, publisherName);
        }

        try {
          PublishRef ref = publisher.publish(media, token);
          LOG.info("Published clip (runner={}, publisher={}, clipId={}, URI={}, thread={})", context.name(),
              publisherName, clipId, ref.uri(), worker);

          if (context.history() != null) {
            context.history().publish(ref, context.name(), publisherName);
          }

          success = true;

          if (observer != null) {
            observer.attemptEnd(publishAttemptId, AttemptStatus.SUCCESS, null);
          }
        } catch (RuntimeException e) {
          LOG.error("Failed to publish clip (runner={}, publisher={}, clipId={}, thread={})", context.name(),
              publisherName, clipId, worker, e);

          if (context.history() != null) {
            context.history().fail(clip, context.name(), e.getMessage());
          }

          if (observer != null) {
            AttemptStatus status = (token.getReason() != null) ? AttemptStatus.CANCELED : AttemptStatus.FAILURE;
            observer.attemptEnd(publishAttemptId, status, e);
          }
        }
      }

      if (success) {
        failures.set(0);

        if (published.incrementAndGet() >= context.posts()) {
          token.cancel(CancellationReason.POSTS_REACHED);
        }
      } else {
        reserved.decrementAndGet();

        if (failures.incrementAndGet() >= context.failureLimit()) {
          LOG.error("Reached publisher failure limit (runner={}, limit={}, thread={})", context.name(),
              context.failureLimit(), worker);
          token.cancel(CancellationReason.PUBLISHER_FAILURE_LIMIT);
        }
      }

      pending.decrementAndGet();
      clean(media, token);
    }
  }

  /**
   * Removes local or staged media associated with the input media.
   *
   * @param media A {@link MediaRef} representing the media to clean.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   */
  private void clean(MediaRef media, CancellationToken token) {
    String clipId = media.clip().id();

    if (context.stager() == null) {
      Path file = media.file();

      if (file != null) {
        try {
          if (Files.isRegularFile(file)) {
            Files.delete(file);
            LOG.info("Deleted local file (runner={}, clipId={}, path={})",
                context.name(), clipId, file);
          }
        } catch (IOException e) {
          LOG.warn("Failed to delete local file (runner={}, clipId={}, path={})",
              context.name(), clipId, file, e);
        }
      }
    } else {
      try {
        context.stager().clean(media, token);
        LOG.info("Deleted staged file (runner={}, stager={}, clipId={})",
            context.name(), context.stager().getName(), clipId);
      } catch (RuntimeException e) {
        LOG.warn("Failed to delete staged file (runner={}, stager={}, clipId={})",
            context.name(), context.stager().getName(), clipId, e);
      }
    }
  }

  /**
   * Returns the number of clips published.
   *
   * @return An integer representing the number of clips published.
   */
  public int getPublished() {
    return published.get();
  }
}
