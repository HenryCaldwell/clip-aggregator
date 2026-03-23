package info.henrycaldwell.aggregator.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.henrycaldwell.aggregator.publish.Publisher;

/**
 * Class for managing a pool of publisher worker threads.
 *
 * This class coordinates concurrent publishing and cleaning of media.
 */
public final class PublisherWorkerPool {
  private static final Logger LOG = LoggerFactory.getLogger(PublisherWorkerPool.class);
  private static final MediaRef SENTINEL = new MediaRef("SENTINEL", null, null, null, null, null, null);

  private final RunnerContext context;
  private final LinkedBlockingQueue<MediaRef> queue;
  private final AtomicInteger reserved;
  private final AtomicInteger pending;
  private final AtomicInteger published;
  private final List<Thread> threads;

  /**
   * Constructs a PublisherWorkerPool.
   *
   * @param context A {@link RunnerContext} representing the configured
   *                components.
   */
  public PublisherWorkerPool(RunnerContext context) {
    this.context = context;
    this.queue = new LinkedBlockingQueue<>(context.publisherThreads() * 2);
    this.reserved = new AtomicInteger(0);
    this.pending = new AtomicInteger(0);
    this.published = new AtomicInteger(0);
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
    while (true) {
      MediaRef media;
      try {
        media = queue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Failed to poll from publish queue (runner={}, thread={})",
            context.name(), Thread.currentThread().getName(), e);
        break;
      }

      if (media == SENTINEL) {
        LOG.info("Stopped publisher thread (runner={}, thread={})",
            context.name(), Thread.currentThread().getName());
        break;
      }

      int slot = reserved.getAndIncrement();
      if (slot >= context.posts()) {
        reserved.decrementAndGet();

        while (pending.get() > 0) {
          Thread.onSpinWait();
        }

        if (published.get() < context.posts()) {
          continue;
        }

        clean(media);
        continue;
      }

      pending.incrementAndGet();

      String clipId = media.id();
      boolean success = false;

      for (Publisher publisher : context.publishers().values()) {
        String publisherName = publisher.getName();

        try {
          PublishRef ref = publisher.publish(media);
          LOG.info("Published clip (runner={}, publisher={}, clipId={}, publishId={}, thread={})",
              context.name(), publisherName, clipId, ref.id(), Thread.currentThread().getName());
          success = true;
        } catch (RuntimeException e) {
          LOG.error("Failed to publish clip (runner={}, publisher={}, clipId={}, thread={})",
              context.name(), publisherName, clipId, Thread.currentThread().getName(), e);

          if (context.history() != null) {
            context.history().fail(clipId, context.name(), e.getMessage());
          }
        }
      }

      if (success) {
        published.incrementAndGet();

        if (context.history() != null) {
          context.history().publish(clipId, context.name());
        }
      } else {
        reserved.decrementAndGet();
      }

      pending.decrementAndGet();
      clean(media);
    }
  }

  /**
   * Removes local or staged media associated with the input media.
   *
   * @param media A {@link MediaRef} representing the media to clean.
   */
  private void clean(MediaRef media) {
    String clipId = media.id();

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
        context.stager().clean(media);
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
