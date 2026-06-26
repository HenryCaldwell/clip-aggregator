package info.henrycaldwell.streamline.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.henrycaldwell.streamline.observe.AttemptStatus;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.observe.PipelineStage;
import info.henrycaldwell.streamline.retrieve.Retriever;
import info.henrycaldwell.streamline.transform.Pipeline;

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
  private final RunSession session;
  private final PublisherWorkerPool publisherPool;
  private final PriorityBlockingQueue<Candidate> queue;
  private final AtomicInteger failures;
  private final List<Thread> threads;

  /**
   * Constructs a PreparationWorkerPool.
   *
   * @param context       A {@link RunnerContext} representing the configured
   *                      components.
   * @param session       A {@link RunSession} representing the state of the run.
   * @param publisherPool A {@link PublisherWorkerPool} representing the publisher
   *                      worker pool.
   */
  public PreparationWorkerPool(RunnerContext context, RunSession session, PublisherWorkerPool publisherPool) {
    this.context = context;
    this.session = session;
    this.publisherPool = publisherPool;
    this.queue = new PriorityBlockingQueue<>();
    this.failures = new AtomicInteger();
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
    String worker = Thread.currentThread().getName();
    Observer observer = context.observer();
    long runId = session.runId();
    CancellationToken token = session.token();

    while (true) {
      Candidate candidate;
      try {
        candidate = queue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Failed to poll from candidate queue (runner={}, thread={})", context.name(), worker, e);
        break;
      }

      if (candidate == SENTINEL) {
        LOG.info("Stopped preparation thread (runner={}, thread={})", context.name(), worker);
        break;
      }

      if (token.getReason() != null) {
        continue;
      }

      Retriever retriever = candidate.retriever();
      Pipeline pipeline = candidate.pipeline();
      ClipRef clip = candidate.clip();

      String retrieverName = retriever.getName();
      String pipelineName = retriever.getPipeline();
      String clipId = clip.id();

      if (context.history() != null) {
        long claimAttemptId = -1;
        if (observer != null) {
          claimAttemptId = observer.attemptStart(runId, worker, clip, PipelineStage.CLAIM, context.history().getName());
        }

        boolean published;
        try {
          published = context.history().contains(clip, context.name());
        } catch (RuntimeException e) {
          if (observer != null) {
            AttemptStatus status = (token.getReason() != null) ? AttemptStatus.CANCELED : AttemptStatus.FAILURE;
            observer.attemptEnd(claimAttemptId, status, e);
          }

          throw e;
        }

        if (published) {
          if (observer != null) {
            observer.attemptEnd(claimAttemptId, AttemptStatus.SKIPPED, null);
          }

          LOG.info("Skipping published clip (runner={}, retriever={}, clipId={}, thread={})",
              context.name(), retrieverName, clipId, worker);
          continue;
        }

        if (observer != null) {
          observer.attemptEnd(claimAttemptId, AttemptStatus.SUCCESS, null);
        }
      }

      MediaRef media;
      try {
        Path target = context.workDir().resolve(clipId + ".mp4");

        long downloadAttemptId = -1;
        if (observer != null) {
          downloadAttemptId = observer.attemptStart(runId, worker, clip, PipelineStage.DOWNLOAD,
              context.downloader().getName());
        }

        try {
          media = context.downloader().download(clip, target, token);

          if (observer != null) {
            observer.attemptEnd(downloadAttemptId, AttemptStatus.SUCCESS, null);
          }
        } catch (RuntimeException e) {
          if (observer != null) {
            AttemptStatus status = (token.getReason() != null) ? AttemptStatus.CANCELED : AttemptStatus.FAILURE;
            observer.attemptEnd(downloadAttemptId, status, e);
          }

          throw e;
        }

        if (pipeline != null) {
          media = pipeline.run(media, observer, session, worker);
        }

        if (context.stager() != null) {
          long stageAttemptId = -1;
          if (observer != null) {
            stageAttemptId = observer.attemptStart(runId, worker, clip, PipelineStage.STAGE,
                context.stager().getName());
          }

          try {
            media = context.stager().stage(media, token);

            if (observer != null) {
              observer.attemptEnd(stageAttemptId, AttemptStatus.SUCCESS, null);
            }
          } catch (RuntimeException e) {
            if (observer != null) {
              AttemptStatus status = (token.getReason() != null) ? AttemptStatus.CANCELED : AttemptStatus.FAILURE;
              observer.attemptEnd(stageAttemptId, status, e);
            }

            throw e;
          }
        }
      } catch (RuntimeException e) {
        LOG.error("Failed to prepare clip (runner={}, retriever={}, clipId={}, thread={})", context.name(),
            retrieverName, clipId, worker, e);

        if (failures.incrementAndGet() >= context.failureLimit()) {
          LOG.error("Reached preparation failure limit (runner={}, limit={}, thread={})", context.name(),
              context.failureLimit(), worker);
          token.cancel(CancellationReason.PREPARATION_FAILURE_LIMIT);
        }

        continue;
      }

      failures.set(0);

      LOG.info(
          "Prepared clip (runner={}, retriever={}, pipeline={}, stager={}, clipId={}, views={}, thread={})",
          context.name(),
          retrieverName,
          pipelineName,
          context.stager() != null ? context.stager().getName() : null,
          clipId,
          clip.views(),
          worker);

      publisherPool.submit(media);
    }
  }
}
