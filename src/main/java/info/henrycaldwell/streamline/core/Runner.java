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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

import info.henrycaldwell.streamline.download.Downloader;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.history.History;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.observe.RunStatus;
import info.henrycaldwell.streamline.publish.Publisher;
import info.henrycaldwell.streamline.retrieve.Retriever;
import info.henrycaldwell.streamline.stage.Stager;
import info.henrycaldwell.streamline.transform.Pipeline;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Class for orchestrating a single end-to-end media run.
 *
 * This class loads configuration, constructs retrievers, an optional history,
 * a downloader, optional pipelines, an optional stager, and publishers,
 * validates cross-references, and executes a fetch, download, transform,
 * publish flow.
 */
public final class Runner {

  private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

  private Runner() {
  }

  /**
   * Entry point for running a single media run.
   *
   * @param args An array of strings representing CLI arguments.
   * @throws IllegalArgumentException if the arguments are invalid or the config
   *                                  file does not exist or is not a file.
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      throw new SpecException("CLI", "Invalid arguments (expected exactly one config path argument)",
          MapUtils.ofNullable("argCount", args.length));
    }

    File file = new File(args[0]);

    if (!file.isFile()) {
      throw new SpecException("CLI", "Config file missing or not a regular file",
          MapUtils.ofNullable("configPath", file.toString()));
    }

    Config config = ConfigFactory.parseFile(file).resolve();

    try {
      run(config);
    } catch (Exception e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
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
        "Run started (runner={}, posts={}, workDir={}, preparationThreads={}, publisherThreads={}, failureLimit={}, observer={}, retrievers={}, history={}, downloader={}, pipelines={}, stager={}, publishers={})",
        context.name(),
        context.posts(),
        context.workDir(),
        context.preparationThreads(),
        context.publisherThreads(),
        context.failureLimit(),
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

        context.stager().purge();
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
      }

      RunSession session = new RunSession(runId, token);

      published = process(context, session);

      CancellationReason reason = token.getReason();
      RunStatus status = (reason == null) ? RunStatus.COMPLETED : reason.status();

      if (context.observer() != null) {
        context.observer().runEnd(runId, status, published);
      }

      LOG.info("Run completed (runner={}, posts={}, published={}, status={})",
          context.name(), context.posts(), published, status);
    } finally {
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
   * Builds the runner context from the root configuration.
   *
   * @param root A {@link Config} representing the root configuration.
   * @return A {@link RunnerContext} representing the assembled components.
   * @throws IllegalArgumentException if required configuration is missing,
   *                                  invalid, or cross-references do not resolve.
   */
  private static RunnerContext buildContext(Config root) {
    if (!root.hasPath("name") || root.getString("name").isBlank()) {
      throw new SpecException("ROOT", "Missing required key", MapUtils.ofNullable("key", "name"));
    }

    String name = root.getString("name");

    if (!root.hasPath("posts")) {
      throw new SpecException(name, "Missing required key", MapUtils.ofNullable("key", "posts"));
    }

    int posts;
    try {
      posts = root.getInt("posts");
    } catch (ConfigException.WrongType e) {
      throw new SpecException(name, "Incorrect key type (expected number)", MapUtils.ofNullable("key", "posts"), e);
    }

    if (posts <= 0) {
      throw new SpecException(name, "Invalid key value (expected posts to be greater than 0)",
          MapUtils.ofNullable("key", "posts", "value", posts));
    }

    if (!root.hasPath("workDir") || root.getString("workDir").isBlank()) {
      throw new SpecException(name, "Missing required key", MapUtils.ofNullable("key", "workDir"));
    }

    Path workDir = Paths.get(root.getString("workDir"));

    int preparationThreads = root.hasPath("preparationThreads") ? root.getInt("preparationThreads") : 1;
    if (preparationThreads <= 0) {
      throw new SpecException(name, "Invalid key value (expected preparationThreads to be greater than 0)",
          MapUtils.ofNullable("key", "preparationThreads", "value", preparationThreads));
    }

    int publisherThreads = root.hasPath("publisherThreads") ? root.getInt("publisherThreads") : 1;
    if (publisherThreads <= 0) {
      throw new SpecException(name, "Invalid key value (expected publisherThreads to be greater than 0)",
          MapUtils.ofNullable("key", "publisherThreads", "value", publisherThreads));
    }

    int failureLimit = root.hasPath("failureLimit") ? root.getInt("failureLimit") : 3;
    if (failureLimit <= 0) {
      throw new SpecException(name, "Invalid key value (expected failureLimit to be greater than 0)",
          MapUtils.ofNullable("key", "failureLimit", "value", failureLimit));
    }

    String configJson = root.root().render(ConfigRenderOptions.concise());

    Observer observer = buildObserver(root);
    Map<String, Retriever> retrievers = buildRetrievers(root);
    History history = buildHistory(root);
    Downloader downloader = buildDownloader(root);
    Map<String, Pipeline> pipelines = buildPipelines(root);
    Stager stager = buildStager(root);
    Map<String, Publisher> publishers = buildPublishers(root);

    if (retrievers.isEmpty()) {
      throw new SpecException(name, "Invalid key value (expected at least 1 retriever)",
          MapUtils.ofNullable("key", "retrievers"));
    }

    if (downloader == null) {
      throw new SpecException(name, "Invalid key value (expected exactly 1 downloader)",
          MapUtils.ofNullable("key", "downloader"));
    }

    if (publishers.isEmpty()) {
      throw new SpecException(name, "Invalid key value (expected at least 1 publisher)",
          MapUtils.ofNullable("key", "publishers"));
    }

    for (Retriever retriever : retrievers.values()) {
      String pipeline = retriever.getPipeline();

      if (pipeline != null && !pipelines.containsKey(pipeline)) {
        throw new SpecException(name, "Retriever references unknown pipeline",
            MapUtils.ofNullable("retriever", retriever.getName(), "pipeline", pipeline));
      }
    }

    LOG.info(
        "Built runner context (runner={}, posts={}, workDir={}, preparationThreads={}, publisherThreads={}, failureLimit={}, observer={}, retrievers={}, history={}, downloader={}, pipelines={}, stager={}, publishers={})",
        name,
        posts,
        workDir,
        preparationThreads,
        publisherThreads,
        failureLimit,
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
   * Processes clips using the configured runner context.
   *
   * @param context A {@link RunnerContext} representing the configured
   *                components.
   * @param session A {@link RunSession} representing the state of the run.
   * @return An integer representing the number of clips published.
   */
  private static int process(RunnerContext context, RunSession session) {
    PublisherWorkerPool publisherPool = new PublisherWorkerPool(context, session);
    PreparationWorkerPool preparationPool = new PreparationWorkerPool(context, session, publisherPool);

    Set<String> seen = new HashSet<>();

    for (Retriever retriever : context.retrievers().values()) {
      String retrieverName = retriever.getName();
      String pipelineName = retriever.getPipeline();
      Pipeline pipeline = (pipelineName != null) ? context.pipelines().get(pipelineName) : null;

      List<ClipRef> clips;
      try {
        clips = retriever.fetch();
      } catch (RuntimeException e) {
        LOG.error("Failed to fetch clips (runner={}, retriever={})", context.name(), retrieverName, e);
        continue;
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
   * Builds the observer from the observer configuration block.
   * 
   * @param config A {@link Config} representing the root configuration.
   * @return An {@link Observer} representing the observer, or {@code null}.
   * @throws IllegalArgumentException if the config type is invalid.
   */
  private static Observer buildObserver(Config root) {
    if (!root.hasPath("observer")) {
      return null;
    }

    Config config;
    try {
      config = root.getConfig("observer");
    } catch (ConfigException.WrongType e) {
      throw new SpecException("ROOT", "Incorrect key type (expected object)", MapUtils.ofNullable("key", "observer"),
          e);
    }

    return ObserverFactory.fromConfig(config);
  }

  /**
   * Builds retrievers from the retrievers configuration list.
   * 
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link LinkedHashMap} representing retrievers keyed by name.
   * @throws IllegalArgumentException if the config type is invalid or names
   *                                  collide.
   */
  private static Map<String, Retriever> buildRetrievers(Config root) {
    Map<String, Retriever> retrievers = new LinkedHashMap<>();

    if (!root.hasPath("retrievers")) {
      return retrievers;
    }

    List<? extends Config> configs;
    try {
      configs = root.getConfigList("retrievers");
    } catch (ConfigException.WrongType e) {
      throw new SpecException("ROOT", "Incorrect key type (expected list)", MapUtils.ofNullable("key", "retrievers"),
          e);
    }

    for (Config config : configs) {
      Retriever retriever = RetrieverFactory.fromConfig(config);
      String name = retriever.getName();

      if (retrievers.containsKey(name)) {
        throw new SpecException("ROOT", "Duplicate retriever name", MapUtils.ofNullable("name", name));
      }

      retrievers.put(name, retriever);
    }

    return retrievers;
  }

  /**
   * Builds the history from the history configuration block.
   * 
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link History} representing the history, or {@code null}.
   * @throws IllegalArgumentException if the config type is invalid.
   */
  private static History buildHistory(Config root) {
    if (!root.hasPath("history")) {
      return null;
    }

    Config config;
    try {
      config = root.getConfig("history");
    } catch (ConfigException.WrongType e) {
      throw new SpecException("ROOT", "Incorrect key type (expected object)", MapUtils.ofNullable("key", "history"), e);
    }

    return HistoryFactory.fromConfig(config);
  }

  /**
   * Builds the downloader from the downloader configuration block.
   * 
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link Downloader} representing the downloader, or {@code null}.
   * @throws IllegalArgumentException if the config type is invalid.
   */
  private static Downloader buildDownloader(Config root) {
    if (!root.hasPath("downloader")) {
      return null;
    }

    Config config;
    try {
      config = root.getConfig("downloader");
    } catch (ConfigException.WrongType e) {
      throw new SpecException("ROOT", "Incorrect key type (expected object)", MapUtils.ofNullable("key", "downloader"),
          e);
    }

    return DownloaderFactory.fromConfig(config);
  }

  /**
   * Builds pipelines from the pipelines configuration list.
   * 
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link LinkedHashMap} representing pipelines keyed by name.
   * @throws IllegalArgumentException if the config type is invalid or names
   *                                  collide.
   */
  private static Map<String, Pipeline> buildPipelines(Config root) {
    Map<String, Pipeline> pipelines = new LinkedHashMap<>();

    if (!root.hasPath("pipelines")) {
      return pipelines;
    }

    List<? extends Config> configs;
    try {
      configs = root.getConfigList("pipelines");
    } catch (ConfigException.WrongType e) {
      throw new SpecException("ROOT", "Incorrect key type (expected list)", MapUtils.ofNullable("key", "pipelines"), e);
    }

    for (Config config : configs) {
      Pipeline pipeline = PipelineFactory.fromConfig(config);
      String name = pipeline.getName();

      if (pipelines.containsKey(name)) {
        throw new SpecException("ROOT", "Duplicate pipeline name", MapUtils.ofNullable("name", name));
      }

      pipelines.put(name, pipeline);
    }

    return pipelines;
  }

  /**
   * Builds the stager from the stager configuration block.
   * 
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link Stager} representing the stager, or {@code null}.
   * @throws IllegalArgumentException if the config type is invalid.
   */
  private static Stager buildStager(Config root) {
    if (!root.hasPath("stager")) {
      return null;
    }

    Config config;
    try {
      config = root.getConfig("stager");
    } catch (ConfigException.WrongType e) {
      throw new SpecException("ROOT", "Incorrect key type (expected object)", MapUtils.ofNullable("key", "stager"), e);
    }

    return StagerFactory.fromConfig(config);
  }

  /**
   * Builds publishers from the publishers configuration list.
   * 
   * @param config A {@link Config} representing the root configuration.
   * @return A {@link LinkedHashMap} representing publishers keyed by name.
   * @throws IllegalArgumentException if the config type is invalid or names
   *                                  collide.
   */
  private static Map<String, Publisher> buildPublishers(Config root) {
    Map<String, Publisher> publishers = new LinkedHashMap<>();

    if (!root.hasPath("publishers")) {
      return publishers;
    }

    List<? extends Config> configs;
    try {
      configs = root.getConfigList("publishers");
    } catch (ConfigException.WrongType e) {
      throw new SpecException("ROOT", "Incorrect key type (expected list)", MapUtils.ofNullable("key", "publishers"),
          e);
    }

    for (Config config : configs) {
      Publisher publisher = PublisherFactory.fromConfig(config);
      String name = publisher.getName();

      if (publishers.containsKey(name)) {
        throw new SpecException("ROOT", "Duplicate publisher name", MapUtils.ofNullable("name", name));
      }

      publishers.put(name, publisher);
    }

    return publishers;
  }
}
