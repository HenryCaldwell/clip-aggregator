package info.henrycaldwell.aggregator.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.download.Downloader;
import info.henrycaldwell.aggregator.publish.Publisher;
import info.henrycaldwell.aggregator.retrieval.Retriever;

/**
 * Class for orchestrating a single end-to-end media run.
 *
 * This class loads configuration, constructs retrievers, the downloader,
 * pipelines, and publishers, validates cross-references, and executes a simple
 * fetch, download, transform, publish flow.
 */
public final class Runner {

  private Runner() {
  }

  /**
   * Entry point for running a single pass of the aggregation pipeline.
   *
   * @param args An array of strings representing CLI arguments.
   * @throws IllegalArgumentException if configuration is missing required
   *                                  sections or wiring is invalid.
   */
  public static void main(String[] args) {
    Config config = ConfigFactory.load();

    Map<String, Retriever> retrievers = buildRetrievers(config);
    Downloader downloader = buildDownloader(config);
    Map<String, Pipeline> pipelines = buildPipelines(config);
    Map<String, Publisher> publishers = buildPublishers(config);

    if (retrievers.isEmpty()) {
      throw new IllegalArgumentException("At least one retriever is required (ROOT)");
    }

    if (downloader == null) {
      throw new IllegalArgumentException("Exactly one downloader is required (ROOT)");
    }

    if (publishers.isEmpty()) {
      throw new IllegalArgumentException("At least one publisher is required (ROOT)");
    }

    for (Retriever retriever : retrievers.values()) {
      String pipeline = retriever.getPipeline();

      if (pipeline != null && !pipelines.containsKey(pipeline)) {
        throw new IllegalArgumentException(
            "Retriever (" + retriever.getName() + ") references unknown pipeline (" + pipeline + ")");
      }
    }

    System.out.println("Configuration OK: "
        + retrievers.size() + " retriever(s), "
        + "1 downloader, "
        + pipelines.size() + " pipeline(s), "
        + publishers.size() + " publisher(s).");
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
      throw new IllegalArgumentException("Invalid type for key retrievers (ROOT)", e);
    }

    for (Config config : configs) {
      Retriever retriever = RetrieverFactory.fromConfig(config);
      String name = retriever.getName();

      if (retrievers.containsKey(name)) {
        throw new IllegalArgumentException("Duplicate retriever name (" + name + ")");
      }

      retrievers.put(name, retriever);
    }

    return retrievers;
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
      throw new IllegalArgumentException("Invalid type for key downloader (ROOT)", e);
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
      throw new IllegalArgumentException("Invalid type for key pipelines (ROOT)", e);
    }

    for (Config config : configs) {
      Pipeline pipeline = PipelineFactory.fromConfig(config);
      String name = pipeline.getName();

      if (pipelines.containsKey(name)) {
        throw new IllegalArgumentException("Duplicate pipeline name (" + name + ")");
      }

      pipelines.put(name, pipeline);
    }

    return pipelines;
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
      throw new IllegalArgumentException("Invalid type for key publishers (ROOT)", e);
    }

    for (Config config : configs) {
      Publisher publisher = PublisherFactory.fromConfig(config);
      String name = publisher.getName();

      if (publishers.containsKey(name)) {
        throw new IllegalArgumentException("Duplicate publisher name (" + name + ")");
      }

      publishers.put(name, publisher);
    }

    return publishers;
  }
}
