package info.henrycaldwell.aggregator.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.download.Downloader;
import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.history.History;
import info.henrycaldwell.aggregator.publish.Publisher;
import info.henrycaldwell.aggregator.retrieve.Retriever;
import info.henrycaldwell.aggregator.stage.Stager;
import info.henrycaldwell.aggregator.transform.Pipeline;

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
          Map.of("argCount", args.length));
    }

    File file = new File(args[0]);

    if (!file.isFile()) {
      throw new SpecException("CLI", "Config file missing or not a regular file",
          Map.of("configPath", file.toString()));
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
    RunnerContext context = buildContext(config);

    LOG.info(
        "Built runner context (runner={}, posts={}, retrievers={}, history={}, downloader={}, pipelines={}, stager={}, publishers={})",
        context.name(),
        context.posts(),
        context.retrievers().keySet(),
        context.history() != null ? context.history().getName() : null,
        context.downloader().getName(),
        context.pipelines().keySet(),
        context.stager() != null ? context.stager().getName() : null,
        context.publishers().keySet());

    try {
      if (context.history() != null) {
        context.history().start();
        LOG.info("Started history (runner={}, history={})",
            context.name(), context.history().getName());
      }

      if (context.stager() != null) {
        context.stager().start();
        LOG.info("Started stager (runner={}, stager={})",
            context.name(), context.stager().getName());
      }

      LOG.info("Starting run (runner={}, posts={})", context.name(), context.posts());

      int published = process(context);

      LOG.info("Run completed (runner={}, posts={}, published={}, publishers={})",
          context.name(), context.posts(), published, context.publishers().size());
    } finally {
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
      throw new SpecException("ROOT", "Missing required key", Map.of("key", "name"));
    }

    String name = root.getString("name");

    if (!root.hasPath("posts")) {
      throw new SpecException(name, "Missing required key", Map.of("key", "posts"));
    }

    int posts;
    try {
      posts = root.getInt("posts");
    } catch (ConfigException.WrongType e) {
      throw new SpecException(name, "Incorrect key type (expected number)", Map.of("key", "posts"), e);
    }

    if (posts <= 0) {
      throw new SpecException(name, "Invalid key value (expected posts to be greater than 0)",
          Map.of("key", "posts", "value", posts));
    }

    Map<String, Retriever> retrievers = buildRetrievers(root);
    History history = buildHistory(root);
    Downloader downloader = buildDownloader(root);
    Map<String, Pipeline> pipelines = buildPipelines(root);
    Stager stager = buildStager(root);
    Map<String, Publisher> publishers = buildPublishers(root);

    if (retrievers.isEmpty()) {
      throw new SpecException(name, "Invalid key value (expected at least 1 retriever)", Map.of("key", "retrievers"));
    }

    if (downloader == null) {
      throw new SpecException(name, "Invalid key value (expected at exactly 1 downloader)",
          Map.of("key", "downloader"));
    }

    if (publishers.isEmpty()) {
      throw new SpecException(name, "Invalid key value (expected at least 1 publisher)", Map.of("key", "publishers"));
    }

    for (Retriever retriever : retrievers.values()) {
      String pipeline = retriever.getPipeline();

      if (pipeline != null && !pipelines.containsKey(pipeline)) {
        throw new SpecException(name, "Retriever references unknown pipeline",
            Map.of("retriever", retriever.getName(), "pipeline", pipeline));
      }
    }

    return new RunnerContext(
        name,
        posts,
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
   * @return An integer representing the number of clips published.
   */
  private static int process(RunnerContext context) {
    int published = 0;
    int posts = context.posts();

    for (Retriever retriever : context.retrievers().values()) {
      if (published >= posts) {
        break;
      }

      List<ClipRef> clips = retriever.fetch();
      String retrieverName = retriever.getName();
      String pipelineName = retriever.getPipeline();
      Pipeline pipeline = (pipelineName != null) ? context.pipelines().get(pipelineName) : null;

      LOG.info("Processing retriever (runner={}, retriever={}, pipeline={}, clips={})",
          context.name(), retrieverName, pipelineName, clips.size());

      for (ClipRef clip : clips) {
        if (published >= posts) {
          break;
        }

        String clipId = clip.id();

        if (context.history() != null) {
          boolean claimed = context.history().claim(clipId, context.name());

          if (!claimed) {
            LOG.info("Skipping claimed clip (runner={}, retriever={}, clipId={})",
                context.name(), retrieverName, clipId);
            continue;
          }
        }

        MediaRef media;
        try {
          Path target = Paths.get("work", clipId + ".mp4");
          media = context.downloader().download(clip, target);

          if (pipeline != null) {
            media = pipeline.run(media);
          }

          if (context.stager() != null) {
            media = context.stager().stage(media);
          }
        } catch (RuntimeException e) {
          LOG.error("Failed to prepare clip (runner={}, retriever={}, clipId={})",
              context.name(), retrieverName, clipId, e);
          continue;
        }

        LOG.info(
            "Prepared clip (runner={}, retriever={}, pipeline={}, stager={}, clipId={})",
            context.name(),
            retrieverName,
            pipelineName,
            context.stager() != null ? context.stager().getName() : null,
            clipId);

        for (Publisher publisher : context.publishers().values()) {
          String publisherName = publisher.getName();

          try {
            PublishRef ref = publisher.publish(media);

            LOG.info("Published clip (runner={}, retriever={}, publisher={}, clipId={}, publishId={})",
                context.name(), retrieverName, publisherName, clipId, ref.id());
          } catch (RuntimeException e) {
            LOG.error("Failed to publish clip (runner={}, retriever={}, publisher={}, clipId={})",
                context.name(), retrieverName, publisherName, clipId, e);
          }
        }

        if (context.stager() == null) {
          Path file = media.file();

          if (file != null) {
            try {
              if (Files.isRegularFile(file)) {
                Files.delete(file);
                LOG.info("Deleted local file (runner={}, retriever={}, clipId={}, path={})",
                    context.name(), retrieverName, clipId, file);
              }
            } catch (Exception e) {
              LOG.warn("Failed to delete local file (runner={}, retriever={}, clipId={}, path={})",
                  context.name(), retrieverName, clipId, file, e);
            }
          }
        } else {
          try {
            context.stager().clean(media);
            LOG.info("Deleted staged media (runner={}, retriever={}, stager={}, clipId={})",
                context.name(), retrieverName, context.stager().getName(), clipId);
          } catch (RuntimeException e) {
            LOG.warn("Failed to delete staged media (runner={}, retriever={}, stager={}, clipId={})",
                context.name(), retrieverName, context.stager().getName(), clipId, e);
          }
        }

        published++;
      }
    }

    return published;
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
      throw new SpecException("ROOT", "Incorrect key type (expected list)", Map.of("key", "retrievers"), e);
    }

    for (Config config : configs) {
      Retriever retriever = RetrieverFactory.fromConfig(config);
      String name = retriever.getName();

      if (retrievers.containsKey(name)) {
        throw new SpecException("ROOT", "Duplicate retriever name", Map.of("name", name));
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
      throw new SpecException("ROOT", "Incorrect key type (expected object)", Map.of("key", "history"), e);
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
      throw new SpecException("ROOT", "Incorrect key type (expected object)", Map.of("key", "downloader"), e);
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
      throw new SpecException("ROOT", "Incorrect key type (expected list)", Map.of("key", "pipelines"), e);
    }

    for (Config config : configs) {
      Pipeline pipeline = PipelineFactory.fromConfig(config);
      String name = pipeline.getName();

      if (pipelines.containsKey(name)) {
        throw new SpecException("ROOT", "Duplicate pipeline name", Map.of("name", name));
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
      throw new SpecException("ROOT", "Incorrect key type (expected object)", Map.of("key", "stager"), e);
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
      throw new SpecException("ROOT", "Incorrect key type (expected list)", Map.of("key", "publishers"), e);
    }

    for (Config config : configs) {
      Publisher publisher = PublisherFactory.fromConfig(config);
      String name = publisher.getName();

      if (publishers.containsKey(name)) {
        throw new SpecException("ROOT", "Duplicate publisher name", Map.of("name", name));
      }

      publishers.put(name, publisher);
    }

    return publishers;
  }

  /**
   * Record for capturing the configuration for a runner.
   * 
   * This record defines a contract for carrying the resolved components required
   * to execute a single run.
   */
  private static final record RunnerContext(
      String name,
      int posts,
      Map<String, Retriever> retrievers,
      History history,
      Downloader downloader,
      Map<String, Pipeline> pipelines,
      Stager stager,
      Map<String, Publisher> publishers) {
  }
}
