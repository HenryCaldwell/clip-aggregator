package info.henrycaldwell.aggregator.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.henrycaldwell.aggregator.retrieve.Retriever;
import info.henrycaldwell.aggregator.transform.Pipeline;

/**
 * Class for managing a pool of preparation worker threads.
 * 
 * This class coordinates concurrent claiming, downloading, transforming, and
 * staging of clips.
 */
public final class PreparationWorkerPool {

  private static final Logger LOG = LoggerFactory.getLogger(PreparationWorkerPool.class);
  private static final Candidate SENTINEL = new Candidate(null, null,
      new ClipRef("SENTINEL", null, null, null, null, Integer.MIN_VALUE, null));

  private final RunnerContext context;
  private final PublisherWorkerPool publisherPool;
  private final PriorityBlockingQueue<Candidate> queue;
  private final List<Thread> threads;

  /**
   * Constructs a PreparationWorkerPool.
   *
   * @param context       A {@link RunnerContext} representing the configured
   *                      components.
   * @param publisherPool A {@link PublisherWorkerPool} representing the publisher
   *                      worker pool.
   */
  public PreparationWorkerPool(RunnerContext context, PublisherWorkerPool publisherPool) {
    this.context = context;
    this.publisherPool = publisherPool;
    this.queue = new PriorityBlockingQueue<>();
    this.threads = new ArrayList<>();
  }

  /**
   * Initializes the configured preparation worker threads.
   */
  public void start() {
    for (int i = 0; i < context.preparationThreads(); i++) {
      int index = i + 1;
      Thread thread = new Thread(() -> run());
      thread.setName("preparation-worker-" + index);
      thread.start();
      threads.add(thread);
    }
  }

  /**
   * Releases and cleans up the configured preparation worker threads.
   */
  public void stop() {
    for (int i = 0; i < threads.size(); i++) {
      queue.put(SENTINEL);
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Failed to stop preparation thread (runner={}, thread={})",
            context.name(), thread.getName(), e);
      }
    }
  }

  /**
   * Submits the input candidate to the candidate queue.
   * 
   * @param candidate A {@link Candidate} representing the candidate to prepare.
   */
  public void submit(Candidate candidate) {
    queue.put(candidate);
  }

  /**
   * Prepares clips from the candidate queue.
   */
  private void run() {
    while (true) {
      Candidate candidate;
      try {
        candidate = queue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Failed to poll from candidate queue (runner={}, thread={})",
            context.name(), Thread.currentThread().getName(), e);
        break;
      }

      if (candidate == SENTINEL) {
        LOG.info("Stopped preparation thread (runner={}, thread={})",
            context.name(), Thread.currentThread().getName());
        break;
      }

      if (publisherPool.getPublished() >= context.posts()) {
        continue;
      }

      Retriever retriever = candidate.retriever();
      Pipeline pipeline = candidate.pipeline();
      ClipRef clip = candidate.clip();

      String retrieverName = retriever.getName();
      String pipelineName = retriever.getPipeline();
      String clipId = clip.id();

      if (context.history() != null) {
        boolean claimed = context.history().claim(clipId, context.name());

        if (!claimed) {
          LOG.info("Skipping published clip (runner={}, retriever={}, clipId={}, thread={})",
              context.name(), retrieverName, clipId, Thread.currentThread().getName());
          continue;
        }
      }

      MediaRef media;
      try {
        Path target = Paths.get("work", clipId + ".mp4");
        media = context.downloader().download(clip, target);

        if (pipeline != null) {
          media = pipeline.run(media, () -> publisherPool.getPublished() >= context.posts());
        }

        if (context.stager() != null) {
          media = context.stager().stage(media);
        }
      } catch (RuntimeException e) {
        LOG.error("Failed to prepare clip (runner={}, retriever={}, clipId={}, thread={})",
            context.name(), retrieverName, clipId, Thread.currentThread().getName(), e);

        if (context.history() != null) {
          context.history().fail(clipId, context.name(), e.getMessage());
        }

        continue;
      }

      if (context.history() != null) {
        context.history().prepare(clipId, context.name());
      }

      LOG.info(
          "Prepared clip (runner={}, retriever={}, pipeline={}, stager={}, clipId={}, views={}, thread={})",
          context.name(),
          retrieverName,
          pipelineName,
          context.stager() != null ? context.stager().getName() : null,
          clipId,
          clip.views(),
          Thread.currentThread().getName());

      publisherPool.submit(media);
    }
  }
}
