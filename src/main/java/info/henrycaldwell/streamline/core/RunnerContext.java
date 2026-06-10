package info.henrycaldwell.streamline.core;

import java.nio.file.Path;
import java.util.Map;

import info.henrycaldwell.streamline.download.Downloader;
import info.henrycaldwell.streamline.history.History;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.publish.Publisher;
import info.henrycaldwell.streamline.retrieve.Retriever;
import info.henrycaldwell.streamline.stage.Stager;
import info.henrycaldwell.streamline.transform.Pipeline;

/**
 * Record for capturing the configuration of a runner.
 * 
 * This record defines a contract for carrying the resolved components required
 * to execute a single run.
 */
record RunnerContext(
    String name,
    int posts,
    Path workDir,
    int preparationThreads,
    int publisherThreads,
    int failureLimit,
    Observer observer,
    Map<String, Retriever> retrievers,
    History history,
    Downloader downloader,
    Map<String, Pipeline> pipelines,
    Stager stager,
    Map<String, Publisher> publishers) {
}
