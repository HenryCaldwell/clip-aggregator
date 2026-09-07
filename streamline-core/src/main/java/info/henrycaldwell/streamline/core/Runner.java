package info.henrycaldwell.streamline.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

import info.henrycaldwell.streamline.config.NumberConstraint;
import info.henrycaldwell.streamline.config.ObjectListConstraint;
import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.download.Downloader;
import info.henrycaldwell.streamline.error.AggregateException;
import info.henrycaldwell.streamline.error.ComponentType;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.history.History;
import info.henrycaldwell.streamline.observe.AttemptStatus;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.observe.RunStatus;
import info.henrycaldwell.streamline.publish.Publisher;
import info.henrycaldwell.streamline.retrieve.Retriever;
import info.henrycaldwell.streamline.stage.Stager;
import info.henrycaldwell.streamline.transform.Pipeline;
import info.henrycaldwell.streamline.util.MapUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Class for orchestrating a single end-to-end media run.
 *
 * This class loads configuration, constructs retrievers, an optional history,
 * a downloader, optional pipelines, an optional stager, and publishers,
 * validates cross-references, and executes a fetch, download, transform,
 * publish flow.
 */
@Command(name = "streamline", description = "Run or validate a streamline configuration")
public final class Runner implements Callable<Integer> {

  private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

  private static final Spec ROOT_SPEC = Spec.builder()
      .requiredString("name", "workDir")
      .requiredNumber(NumberConstraint.greaterThan(0), "posts")
      .optionalNumber(NumberConstraint.greaterThan(0),
          "preparationThreads", "publisherThreads", "failureLimit", "heartbeatInterval")
      .requiredObject("downloader")
      .optionalObject("stager", "history", "observer")
      .requiredObjectList(ObjectListConstraint.nonEmpty(), "retrievers", "publishers")
      .optionalObjectList("pipelines")
      .build();

  @Option(names = "--validate", description = "Validate the configuration")
  private boolean validateOnly;

  @Parameters(index = "0", description = "Path to the configuration")
  private String configPath;

  private Runner() {
  }

  /**
   * Entry point for the CLI.
   *
   * @param args An array of strings representing CLI arguments.
   */
  public static void main(String[] args) {
    System.exit(new CommandLine(new Runner()).execute(args));
  }

  /**
   * Loads the configuration and dispatches to a run or validation.
   * 
   * @return An {@link Integer} representing the exit code, {@code 0} on success
   *         or {@code 1} if validation errors are present.
   * @throws SpecException if the config file is missing or not a regular file.
   */
  @Override
  public Integer call() {
    File file = new File(configPath);

    if (!file.isFile()) {
      throw new SpecException(ComponentType.CLI, null, null, "Config file missing or not a regular file",
          MapUtils.ofNullable("configPath", file.toString()));
    }

    Config config = ConfigFactory.parseFile(file).resolve();

    if (validateOnly) {
      List<SpecException> exceptions = validate(config);

      if (!exceptions.isEmpty()) {
        new AggregateException(exceptions).printStackTrace(System.err);

        return 1;
      }

      return 0;
    }

    run(config);

    return 0;
  }

  /**
   * Executes a single media run using the provided configuration.
   *
   * @param config A {@link Config} representing the root configuration.
   */
  public static void run(Config config) {
    run(buildContext(config));
  }

  /**
   * Executes a single media run using the provided runner context.
   *
   * @param context A {@link RunnerContext} representing the configured
   *                components.
   */
  public static void run(RunnerContext context) {
    LOG.info(
        "Run started (runner={}, posts={}, workDir={}, preparationThreads={}, publisherThreads={}, failureLimit={}, heartbeatInterval={}, observer={}, retrievers={}, history={}, downloader={}, pipelines={}, stager={}, publishers={})",
        context.name(),
        context.posts(),
        context.workDir(),
        context.preparationThreads(),
        context.publisherThreads(),
        context.failureLimit(),
        context.heartbeatInterval(),
        context.observer() != null ? context.observer().getName() : null,
        context.retrievers().keySet(),
        context.history() != null ? context.history().getName() : null,
        context.downloader().getName(),
        context.pipelines().keySet(),
        context.stager() != null ? context.stager().getName() : null,
        context.publishers().keySet());

    long runId = -1;
    CancellationToken token = new CancellationToken();
    int published = 0;

    ScheduledExecutorService heartbeats = null;

    try {
      if (context.observer() != null) {
        context.observer().start();
        LOG.info("Started observer (runner={}, observer={})", context.name(), context.observer().getName());
      }

      if (context.history() != null) {
        context.history().start();
        LOG.info("Started history (runner={}, history={})",
            context.name(), context.history().getName());
      }

      if (context.stager() != null) {
        context.stager().start();
        LOG.info("Started stager (runner={}, stager={})",
            context.name(), context.stager().getName());

        context.stager().purge(token);
        LOG.info("Purged stager directory (runner={}, stager={})",
            context.name(), context.stager().getName());
      }

      if (Files.isDirectory(context.workDir())) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(context.workDir())) {
          for (Path entry : stream) {
            if (Files.isRegularFile(entry)) {
              Files.delete(entry);
            }
          }
        } catch (IOException e) {
          LOG.warn("Failed to purge work directory (runner={}, workDir={})", context.name(), context.workDir(), e);
        }

        LOG.info("Purged work directory (runner={}, workDir={})", context.name(), context.workDir());
      }

      if (context.observer() != null) {
        runId = context.observer().runStart(context.name(), context.configJson());
        heartbeats = startHeartbeats(context.observer(), context.name(), runId, context.heartbeatInterval());
      }

      RunSession session = new RunSession(runId, token);

      published = process(context, session);

      CancellationReason reason = token.getReason();
      RunStatus status = (reason == null) ? RunStatus.SUCCESS : reason.status();

      if (context.observer() != null) {
        heartbeats.shutdown();
        heartbeats = null;
        context.observer().runEnd(runId, status, published);
      }

      LOG.info("Run completed (runner={}, posts={}, published={}, status={})",
          context.name(), context.posts(), published, status);
    } finally {
      if (heartbeats != null) {
        heartbeats.shutdownNow();
      }

      if (context.observer() != null) {
        context.observer().stop();
        LOG.info("Stopped observer (runner={}, observer={})", context.name(), context.observer().getName());
      }

      if (context.history() != null) {
        context.history().stop();
        LOG.info("Stopped history (runner={}, history={})",
            context.name(), context.history().getName());
      }

      if (context.stager() != null) {
        context.stager().stop();
        LOG.info("Stopped stager (runner={}, stager={})",
            context.name(), context.stager().getName());
      }
    }
  }

  /**
   * Validates the root configuration block.
   *
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config) {
    List<SpecException> exceptions = ROOT_SPEC.validate(config, ComponentType.ROOT, null, null);

    try {
      exceptions.addAll(DownloaderFactory.validate(config.getConfig("downloader")));
    } catch (ConfigException.Missing | ConfigException.WrongType e) {
      // Already surfaced by ROOT_SPEC
    }

    if (config.hasPath("stager")) {
      try {
        exceptions.addAll(StagerFactory.validate(config.getConfig("stager")));
      } catch (ConfigException.WrongType e) {
        // Already surfaced by ROOT_SPEC
      }
    }

    if (config.hasPath("history")) {
      try {
        exceptions.addAll(HistoryFactory.validate(config.getConfig("history")));
      } catch (ConfigException.WrongType e) {
        // Already surfaced by ROOT_SPEC
      }
    }

    if (config.hasPath("observer")) {
      try {
        exceptions.addAll(ObserverFactory.validate(config.getConfig("observer")));
      } catch (ConfigException.WrongType e) {
        // Already surfaced by ROOT_SPEC
      }
    }

    Set<String> pipelineNames = new HashSet<>();
    if (config.hasPath("pipelines")) {
      try {
        List<? extends Config> configs = config.getConfigList("pipelines");

        for (int i = 0; i < configs.size(); i++) {
          Config entry = configs.get(i);
          List<SpecException> pipelineExceptions = PipelineFactory.validate(entry, i);
          exceptions.addAll(pipelineExceptions);

          String name = entry.hasPath("name") && !entry.getString("name").isBlank()
              ? entry.getString("name")
              : null;

          if (name != null && !pipelineNames.add(name)) {
            exceptions.add(new SpecException(ComponentType.PIPELINE, null, name, "Duplicate pipeline name",
                MapUtils.ofNullable("index", i, "name", name)));
          }
        }
      } catch (ConfigException.WrongType e) {
        // Already surfaced by ROOT_SPEC
      }
    }

    Set<String> retrieverNames = new HashSet<>();
    try {
      List<? extends Config> configs = config.getConfigList("retrievers");

      for (int i = 0; i < configs.size(); i++) {
        Config entry = configs.get(i);
        List<SpecException> retrieverExceptions = RetrieverFactory.validate(entry, i);
        exceptions.addAll(retrieverExceptions);

        String name = entry.hasPath("name") && !entry.getString("name").isBlank()
            ? entry.getString("name")
            : null;

        if (name != null && !retrieverNames.add(name)) {
          exceptions.add(new SpecException(ComponentType.RETRIEVER, null, name, "Duplicate retriever name",
              MapUtils.ofNullable("index", i, "name", name)));
        }

        if (entry.hasPath("pipeline")) {
          String displayName = name != null ? name : "UNNAMED_RETRIEVER";

          try {
            String pipelineName = entry.getString("pipeline");

            if (!pipelineNames.contains(pipelineName)) {
              exceptions
                  .add(new SpecException(ComponentType.RETRIEVER, null, displayName, "References unknown pipeline",
                      MapUtils.ofNullable("index", i, "key", "pipeline", "value", pipelineName)));
            }
          } catch (ConfigException.WrongType ignored) {
            // Already surfaced by factory
          }
        }
      }
    } catch (ConfigException.Missing | ConfigException.WrongType e) {
      // Already surfaced by ROOT_SPEC
    }

    try {
      Set<String> publisherNames = new HashSet<>();
      List<? extends Config> configs = config.getConfigList("publishers");

      for (int i = 0; i < configs.size(); i++) {
        Config entry = configs.get(i);
        List<SpecException> publisherExceptions = PublisherFactory.validate(entry, i);
        exceptions.addAll(publisherExceptions);

        String name = entry.hasPath("name") && !entry.getString("name").isBlank()
            ? entry.getString("name")
            : null;

        if (name != null && !publisherNames.add(name)) {
          exceptions.add(new SpecException(ComponentType.PUBLISHER, null, name, "Duplicate publisher name",
              MapUtils.ofNullable("index", i, "name", name)));
        }
      }
    } catch (ConfigException.Missing | ConfigException.WrongType e) {
      // Already surfaced by ROOT_SPEC
    }

    return exceptions;
  }

  /**
   * Builds the runner context from the root configuration.
   *
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link RunnerContext} representing the assembled components.
   * @throws SpecException if the root configuration is invalid.
   */
  private static RunnerContext buildContext(Config config) {
    List<SpecException> exceptions = validate(config);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    String name = config.getString("name");
    int posts = config.getInt("posts");
    Path workDir = Paths.get(config.getString("workDir"));
    int preparationThreads = config.hasPath("preparationThreads") ? config.getInt("preparationThreads") : 1;
    int publisherThreads = config.hasPath("publisherThreads") ? config.getInt("publisherThreads") : 1;
    int failureLimit = config.hasPath("failureLimit") ? config.getInt("failureLimit") : 3;
    long heartbeatInterval = config.hasPath("heartbeatInterval") ? config.getNumber("heartbeatInterval").longValue()
        : 10L;

    String configJson = config.root().render(ConfigRenderOptions.concise());

    Observer observer = config.hasPath("observer") ? ObserverFactory.fromConfig(config.getConfig("observer")) : null;
    Map<String, Retriever> retrievers = buildRetrievers(config);
    History history = config.hasPath("history") ? HistoryFactory.fromConfig(config.getConfig("history")) : null;
    Downloader downloader = DownloaderFactory.fromConfig(config.getConfig("downloader"));
    Map<String, Pipeline> pipelines = buildPipelines(config);
    Stager stager = config.hasPath("stager") ? StagerFactory.fromConfig(config.getConfig("stager")) : null;
    Map<String, Publisher> publishers = buildPublishers(config);

    LOG.info(
        "Built runner context (runner={}, posts={}, workDir={}, preparationThreads={}, publisherThreads={}, failureLimit={}, heartbeatInterval={}, observer={}, retrievers={}, history={}, downloader={}, pipelines={}, stager={}, publishers={})",
        name,
        posts,
        workDir,
        preparationThreads,
        publisherThreads,
        failureLimit,
        heartbeatInterval,
        observer != null ? observer.getName() : null,
        retrievers.keySet(),
        history != null ? history.getName() : null,
        downloader.getName(),
        pipelines.keySet(),
        stager != null ? stager.getName() : null,
        publishers.keySet());

    return new RunnerContext(
        name,
        posts,
        workDir,
        preparationThreads,
        publisherThreads,
        failureLimit,
        heartbeatInterval,
        configJson,
        observer,
        retrievers,
        history,
        downloader,
        pipelines,
        stager,
        publishers);
  }

  /**
   * Starts a scheduler that periodically records heartbeats.
   * 
   * @param observer An {@link Observer} representing the observer recording the
   *                 heartbeats.
   * @param runner   A string representing the runner name.
   * @param runId    A long representing the run identifier.
   * @param interval A long representing the heartbeat interval in seconds.
   * @return A {@link ScheduledExecutorService} representing the heartbeat
   *         scheduler.
   */
  private static ScheduledExecutorService startHeartbeats(Observer observer, String runner, long runId, long interval) {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
      Thread thread = new Thread(task, runner + "-heartbeat");
      thread.setDaemon(true);
      return thread;
    });

    executor.scheduleAtFixedRate(() -> {
      try {
        observer.heartbeat(runId);
      } catch (RuntimeException e) {
        LOG.warn("Failed to record heartbeat (runner={}, runId={})", runner, runId, e);
      }
    }, interval, interval, TimeUnit.SECONDS);

    LOG.info("Started runner heartbeats (runner={}, observer={}, interval={})",
        runner, observer.getName(), interval);

    return executor;
  }

  /**
   * Processes clips using the configured runner context.
   *
   * @param context A {@link RunnerContext} representing the configured
   *                components.
   * @param session A {@link RunSession} representing the state of the run.
   * @return An integer representing the number of clips published.
   */
  private static int process(RunnerContext context, RunSession session) {
    CancellationToken token = session.token();
    PublisherWorkerPool publisherPool = new PublisherWorkerPool(context, session);
    PreparationWorkerPool preparationPool = new PreparationWorkerPool(context, session, publisherPool);

    Set<String> seen = new HashSet<>();

    for (Retriever retriever : context.retrievers().values()) {
      String retrieverName = retriever.getName();
      String pipelineName = retriever.getPipeline();
      Pipeline pipeline = (pipelineName != null) ? context.pipelines().get(pipelineName) : null;

      long fetchId = -1;
      if (context.observer() != null) {
        fetchId = context.observer().fetchStart(session.runId(), retrieverName, "fetcher-worker-1");
      }

      List<ClipRef> clips;
      try {
        clips = retriever.fetch(token);
      } catch (RuntimeException e) {
        if (context.observer() != null) {
          AttemptStatus status = token.getReason() != null ? AttemptStatus.CANCELED : AttemptStatus.FAILURE;
          context.observer().fetchEnd(fetchId, status, 0, e);
        }

        LOG.error("Failed to fetch clips (runner={}, retriever={})", context.name(), retrieverName, e);
        continue;
      }

      if (context.observer() != null) {
        context.observer().fetchEnd(fetchId, AttemptStatus.SUCCESS, clips.size(), null);
      }

      LOG.info("Fetched retriever clips (runner={}, retriever={}, pipeline={}, clips={})",
          context.name(), retrieverName, pipelineName, clips.size());

      for (ClipRef clip : clips) {
        if (seen.add(clip.id())) {
          preparationPool.submit(new Candidate(retriever, pipeline, clip));
        }
      }
    }

    try {
      publisherPool.start();
      preparationPool.start();
    } finally {
      preparationPool.stop();
      publisherPool.stop();
    }

    return publisherPool.getPublished();
  }

  /**
   * Builds retrievers from the retrievers configuration list.
   *
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link LinkedHashMap} representing retrievers keyed by name.
   */
  private static Map<String, Retriever> buildRetrievers(Config config) {
    Map<String, Retriever> retrievers = new LinkedHashMap<>();
    List<? extends Config> configs = config.getConfigList("retrievers");

    for (int i = 0; i < configs.size(); i++) {
      Retriever retriever = RetrieverFactory.fromConfig(configs.get(i), i);
      retrievers.put(retriever.getName(), retriever);
    }

    return retrievers;
  }

  /**
   * Builds pipelines from the pipelines configuration list.
   *
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link LinkedHashMap} representing pipelines keyed by name.
   */
  private static Map<String, Pipeline> buildPipelines(Config config) {
    Map<String, Pipeline> pipelines = new LinkedHashMap<>();

    if (!config.hasPath("pipelines")) {
      return pipelines;
    }

    List<? extends Config> configs = config.getConfigList("pipelines");

    for (int i = 0; i < configs.size(); i++) {
      Pipeline pipeline = PipelineFactory.fromConfig(configs.get(i), i);
      pipelines.put(pipeline.getName(), pipeline);
    }

    return pipelines;
  }

  /**
   * Builds publishers from the publishers configuration list.
   *
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link LinkedHashMap} representing publishers keyed by name.
   */
  private static Map<String, Publisher> buildPublishers(Config config) {
    Map<String, Publisher> publishers = new LinkedHashMap<>();
    List<? extends Config> configs = config.getConfigList("publishers");

    for (int i = 0; i < configs.size(); i++) {
      Publisher publisher = PublisherFactory.fromConfig(configs.get(i), i);
      publishers.put(publisher.getName(), publisher);
    }

    return publishers;
  }
}
