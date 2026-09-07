package info.henrycaldwell.streamline.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.download.Downloader;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.history.History;
import info.henrycaldwell.streamline.observe.AbstractObserver;
import info.henrycaldwell.streamline.observe.AttemptStatus;
import info.henrycaldwell.streamline.observe.PipelineStage;
import info.henrycaldwell.streamline.observe.RunStatus;
import info.henrycaldwell.streamline.publish.Publisher;
import info.henrycaldwell.streamline.retrieve.Retriever;
import info.henrycaldwell.streamline.stage.Stager;
import info.henrycaldwell.streamline.transform.Pipeline;
import info.henrycaldwell.streamline.transform.Transformer;

public class RunnerTest {

  @Nested
  class BuildContext {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void acceptsConfiguredPreparationThreads() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          preparationThreads = 2
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void acceptsConfiguredPublisherThreads() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          publisherThreads = 2
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void acceptsConfiguredFailureLimit() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          failureLimit = 5
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void acceptsConfiguredHeartbeatInterval() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          heartbeatInterval = 30
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void acceptsConfiguredHistory() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          history = { name = h, type = no_op }
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void acceptsConfiguredObserver() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          observer = { name = o, type = no_op }
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void acceptsConfiguredStager() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          stager = { name = s, type = no_op }
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void acceptsConfiguredPipeline() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [
            { name = r, type = no_op, pipeline = pl }
          ]
          pipelines = [
            { name = pl, transformers = [] }
          ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void throwsOnInvalidConfig() {
      Config config = ConfigFactory.parseString("""
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> Runner.run(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }
  }

  @Nested
  class Validate {

    @Test
    void doesNotThrowOnValidConfig() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      assertTrue(Runner.validate(config).isEmpty());
    }

    @Test
    void accumulatesErrorsAcrossAllLayers() {
      Config config = ConfigFactory.parseString("""
          posts = 5
          workDir = work
          retrievers = [ { name = r, type = twitch } ]
          downloader = { name = d, type = yt-dlp }
          publishers = [ { name = p, type = instagram } ]
          """);

      List<SpecException> exceptions = Runner.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=name")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=ytDlpPath")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=clientId")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=accountId")));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingWorkDir() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=workDir"));
    }

    @Test
    void throwsOnBlankWorkDir() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = ""
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=workDir"));
    }

    @Test
    void throwsOnMissingPosts() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=posts"));
    }

    @Test
    void throwsOnWrongTypeForPosts() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = invalid
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
      assertTrue(exception.getMessage().contains("key=posts"));
    }

    @Test
    void throwsOnInvalidPosts() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 0
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=posts"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnInvalidPreparationThreads() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          preparationThreads = 0
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=preparationThreads"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnInvalidPublisherThreads() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          publisherThreads = 0
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=publisherThreads"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnInvalidFailureLimit() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          failureLimit = 0
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=failureLimit"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnInvalidHeartbeatInterval() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          heartbeatInterval = 0
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Invalid key value"));
      assertTrue(exception.getMessage().contains("key=heartbeatInterval"));
      assertTrue(exception.getMessage().contains("value=0"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          unknownKey = foo
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=unknownKey"));
    }

    @Test
    void throwsOnMissingRetrievers() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=retrievers"));
    }

    @Test
    void throwsOnWrongTypeForRetrievers() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = invalid
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list<object>)"));
      assertTrue(exception.getMessage().contains("key=retrievers"));
    }

    @Test
    void throwsOnEmptyRetrievers() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = []
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Invalid key value (expected retrievers to be non-empty)"));
      assertTrue(exception.getMessage().contains("key=retrievers"));
    }

    @Test
    void accumulatesRetrieverErrors() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = r, type = twitch } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      List<SpecException> exceptions = Runner.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=clientId")));
    }

    @Test
    void throwsOnDuplicateRetrieverName() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [
            { name = base_retriever, type = no_op }
            { name = base_retriever, type = no_op }
          ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Duplicate retriever name"));
      assertTrue(exception.getMessage().contains("index=1"));
      assertTrue(exception.getMessage().contains("name=base_retriever"));
    }

    @Test
    void throwsOnUnknownPipelineReference() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op, pipeline = nonexistent } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("References unknown pipeline"));
      assertTrue(exception.getMessage().contains("base_retriever"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=pipeline"));
      assertTrue(exception.getMessage().contains("value=nonexistent"));
    }

    @Test
    void throwsOnMissingDownloader() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=downloader"));
    }

    @Test
    void throwsOnWrongTypeForDownloader() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          downloader = invalid
          retrievers = [ { name = base_retriever, type = no_op } ]
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected object)"));
      assertTrue(exception.getMessage().contains("key=downloader"));
    }

    @Test
    void accumulatesDownloaderErrors() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = d, type = yt-dlp }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      List<SpecException> exceptions = Runner.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=ytDlpPath")));
    }

    @Test
    void throwsOnWrongTypeForPipelines() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          pipelines = invalid
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list<object>)"));
      assertTrue(exception.getMessage().contains("key=pipelines"));
    }

    @Test
    void accumulatesPipelineErrors() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          pipelines = [ { transformers = [] } ]
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      List<SpecException> exceptions = Runner.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=name")));
    }

    @Test
    void throwsOnDuplicatePipelineName() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          pipelines = [
            { name = base_pipeline, transformers = [] }
            { name = base_pipeline, transformers = [] }
          ]
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Duplicate pipeline name"));
      assertTrue(exception.getMessage().contains("index=1"));
      assertTrue(exception.getMessage().contains("name=base_pipeline"));
    }

    @Test
    void throwsOnWrongTypeForStager() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          stager = invalid
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected object)"));
      assertTrue(exception.getMessage().contains("key=stager"));
    }

    @Test
    void accumulatesStagerErrors() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          stager = { name = s, type = aws-s3 }
          """);

      List<SpecException> exceptions = Runner.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=accessKey")));
    }

    @Test
    void throwsOnMissingPublishers() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=publishers"));
    }

    @Test
    void throwsOnWrongTypeForPublishers() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          publishers = invalid
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list<object>)"));
      assertTrue(exception.getMessage().contains("key=publishers"));
    }

    @Test
    void throwsOnEmptyPublishers() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = []
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Invalid key value (expected publishers to be non-empty)"));
      assertTrue(exception.getMessage().contains("key=publishers"));
    }

    @Test
    void accumulatesPublisherErrors() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = p, type = instagram } ]
          """);

      List<SpecException> exceptions = Runner.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=accountId")));
    }

    @Test
    void throwsOnDuplicatePublisherName() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          publishers = [
            { name = base_publisher, type = no_op }
            { name = base_publisher, type = no_op }
          ]
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Duplicate publisher name"));
      assertTrue(exception.getMessage().contains("index=1"));
      assertTrue(exception.getMessage().contains("name=base_publisher"));
    }

    @Test
    void throwsOnWrongTypeForHistory() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          history = invalid
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected object)"));
      assertTrue(exception.getMessage().contains("key=history"));
    }

    @Test
    void accumulatesHistoryErrors() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          history = { name = h, type = sqlite }
          """);

      List<SpecException> exceptions = Runner.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=databasePath")));
    }

    @Test
    void throwsOnWrongTypeForObserver() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          observer = invalid
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          """);

      SpecException exception = Runner.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected object)"));
      assertTrue(exception.getMessage().contains("key=observer"));
    }

    @Test
    void accumulatesObserverErrors() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = base_retriever, type = no_op } ]
          downloader = { name = base_downloader, type = no_op }
          publishers = [ { name = base_publisher, type = no_op } ]
          observer = { name = o, type = sqlite }
          """);

      List<SpecException> exceptions = Runner.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=databasePath")));
    }
  }

  @Nested
  class Run {

    @Test
    void completesWithNoClips() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void completesWithObserver() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          observer = { name = o, type = no_op }
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void completesWithHistory() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          history = { name = h, type = no_op }
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }

    @Test
    void completesWithStager() {
      Config config = ConfigFactory.parseString("""
          name = test_runner
          posts = 5
          workDir = work
          stager = { name = s, type = no_op }
          retrievers = [ { name = r, type = no_op } ]
          downloader = { name = d, type = no_op }
          publishers = [ { name = p, type = no_op } ]
          """);

      assertDoesNotThrow(() -> Runner.run(config));
    }
  }

  @Nested
  class Process {

    @TempDir
    Path workDir;

    // Success

    @Test
    void publishesAllClips() {
      List<ClipRef> clips = List.of(
          new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null),
          new ClipRef("clip-2", null, "Title", "Broadcaster", "en", 90, null));
      TestRetriever retriever = new TestRetriever(clips);
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(2, tracker.published.get());
    }

    @Test
    void purgesStagerOnStart() {
      TestRetriever retriever = new TestRetriever(List.of());
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingStager tracker = new TrackingStager();
      NoOpPublisher publisher = new NoOpPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          tracker,
          Map.of("p", publisher));

      Runner.run(context);

      assertEquals(1, tracker.purged.get());
    }

    @Test
    void purgesWorkDirectoryOnStart() throws IOException {
      Path orphan = workDir.resolve("orphaned.mp4");
      Files.createFile(orphan);
      TestRetriever retriever = new TestRetriever(List.of());
      NoOpDownloader downloader = new NoOpDownloader();
      NoOpPublisher publisher = new NoOpPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", publisher));

      Runner.run(context);

      assertFalse(Files.exists(orphan));
    }

    @Test
    void deduplicatesClipsAcrossRetrievers() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever r1 = new TestRetriever(List.of(clip));
      TestRetriever r2 = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r1", r1, "r2", r2),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, tracker.published.get());
    }

    @Test
    void stopsAtPostsLimit() {
      List<ClipRef> clips = List.of(
          new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null),
          new ClipRef("clip-2", null, "Title", "Broadcaster", "en", 90, null),
          new ClipRef("clip-3", null, "Title", "Broadcaster", "en", 80, null),
          new ClipRef("clip-4", null, "Title", "Broadcaster", "en", 70, null),
          new ClipRef("clip-5", null, "Title", "Broadcaster", "en", 60, null));
      TestRetriever retriever = new TestRetriever(clips);
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 2, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(2, tracker.published.get());
    }

    @Test
    void publishesWhenOnePublisherThrows() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      ThrowingPublisher throwing = new ThrowingPublisher();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p1", throwing, "p2", tracker));

      Runner.run(context);

      assertEquals(1, tracker.published.get());
    }

    @Test
    void publishesWithPipelineApplied() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip), "test-pipeline");
      NoOpDownloader downloader = new NoOpDownloader();
      Pipeline pipeline = new Pipeline("test-pipeline", List.of());
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of("test-pipeline", pipeline),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, tracker.published.get());
    }

    @Test
    void publishesWithStagingApplied() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      NoOpStager stager = new NoOpStager();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          stager,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, tracker.published.get());
    }

    @Test
    void marksHistoryPreparedAndPublishedWhenClipSucceeds() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingHistory history = new TrackingHistory();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          history,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, history.contained.get());
      assertEquals(1, history.added.get());
      assertEquals(1, tracker.published.get());
    }

    // Retrieval failure

    @Test
    void publishesNothingWhenFetchThrows() {
      ThrowingRetriever retriever = new ThrowingRetriever();
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(0, tracker.published.get());
    }

    @Test
    void publishesNothingWhenHistoryRejects() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      RejectingHistory history = new RejectingHistory();
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          history,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(0, tracker.published.get());
    }

    // Preparation failure

    @Test
    void publishesNothingWhenDownloadThrows() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      ThrowingDownloader downloader = new ThrowingDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(0, tracker.published.get());
    }

    @Test
    void publishesNothingWhenPipelineThrows() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip), "test-pipeline");
      NoOpDownloader downloader = new NoOpDownloader();
      Pipeline pipeline = new Pipeline("test-pipeline", List.of(new ThrowingTransformer()));
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of("test-pipeline", pipeline),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(0, tracker.published.get());
    }

    @Test
    void publishesNothingWhenStagingThrows() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      ThrowingStager stager = new ThrowingStager();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          stager,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(0, tracker.published.get());
    }

    @Test
    void abortsAfterConsecutivePreparationFailures() {
      List<ClipRef> clips = List.of(
          new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null),
          new ClipRef("clip-2", null, "Title", "Broadcaster", "en", 90, null),
          new ClipRef("clip-3", null, "Title", "Broadcaster", "en", 80, null),
          new ClipRef("clip-4", null, "Title", "Broadcaster", "en", 70, null),
          new ClipRef("clip-5", null, "Title", "Broadcaster", "en", 60, null));
      TestRetriever retriever = new TestRetriever(clips);
      TrackingThrowingDownloader trackerThrower = new TrackingThrowingDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          trackerThrower,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(3, trackerThrower.attempts.get());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void marksHistoryFailedWhenDownloadThrows() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      ThrowingDownloader downloader = new ThrowingDownloader();
      TrackingHistory history = new TrackingHistory();
      TrackingPublisher tracker = new TrackingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          history,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, history.contained.get());
      assertEquals(0, history.added.get());
      assertEquals(0, tracker.published.get());
    }

    // Publishing failure

    @Test
    void abortsAfterConsecutivePublisherFailures() {
      List<ClipRef> clips = List.of(
          new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null),
          new ClipRef("clip-2", null, "Title", "Broadcaster", "en", 90, null),
          new ClipRef("clip-3", null, "Title", "Broadcaster", "en", 80, null),
          new ClipRef("clip-4", null, "Title", "Broadcaster", "en", 70, null),
          new ClipRef("clip-5", null, "Title", "Broadcaster", "en", 60, null));
      TestRetriever retriever = new TestRetriever(clips);
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingThrowingPublisher publisher = new TrackingThrowingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", publisher));

      Runner.run(context);

      assertEquals(3, publisher.attempts.get());
    }

    @Test
    void marksHistoryFailedWhenAllPublishersThrow() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingHistory history = new TrackingHistory();
      ThrowingPublisher publisher = new ThrowingPublisher();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          null,
          Map.of("r", retriever),
          history,
          downloader,
          Map.of(),
          null,
          Map.of("p", publisher));

      Runner.run(context);

      assertEquals(1, history.contained.get());
      assertEquals(0, history.added.get());
    }
  }

  @Nested
  class Lifecycle {

    @TempDir
    Path workDir;

    @Test
    void recordsRunLifecycle() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      TrackingHistory history = new TrackingHistory();
      NoOpDownloader downloader = new NoOpDownloader();
      NoOpStager stager = new NoOpStager();
      TrackingPublisher publisher = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, "{\"name\":\"test\"}",
          observer,
          Map.of("r", retriever),
          history,
          downloader,
          Map.of(),
          stager,
          Map.of("p", publisher));

      Runner.run(context);

      assertEquals(1, observer.startedRuns.size());
      assertEquals("test", observer.startedRuns.get(0).runner());
      assertEquals("{\"name\":\"test\"}", observer.startedRuns.get(0).config());
      assertEquals(1, observer.endedRuns.size());
      assertEquals(RunStatus.SUCCESS, observer.endedRuns.get(0).status());
      assertEquals(1, observer.endedRuns.get(0).published());

      assertEquals(1, observer.startedFetches.size());
      assertEquals("test_retriever", observer.startedFetches.get(0).retriever());
      assertEquals(1, observer.endedFetches.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedFetches.get(0).status());
      assertEquals(1, observer.endedFetches.get(0).clips());
      assertNull(observer.endedFetches.get(0).error());

      assertEquals(4, observer.startedAttempts.size());
      assertEquals(PipelineStage.CLAIM, observer.startedAttempts.get(0).stage());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(1).stage());
      assertEquals(PipelineStage.STAGE, observer.startedAttempts.get(2).stage());
      assertEquals(PipelineStage.PUBLISH, observer.startedAttempts.get(3).stage());

      assertEquals(4, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(0).status());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(1).status());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(2).status());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(3).status());

      assertEquals(1, observer.publishes.size());
      assertEquals("clip-1", observer.publishes.get(0).clipId());
      assertEquals("tracking_publisher", observer.publishes.get(0).publisher());
      assertNull(observer.publishes.get(0).uri());
    }

    @Test
    void recordsFailedFetch() {
      ThrowingRetriever retriever = new ThrowingRetriever();
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, observer.startedFetches.size());
      assertEquals(1, observer.endedFetches.size());
      assertEquals(AttemptStatus.FAILURE, observer.endedFetches.get(0).status());
      assertEquals(0, observer.endedFetches.get(0).clips());
      assertNotNull(observer.endedFetches.get(0).error());
    }

    @Test
    void recordsCanceledFetch() {
      CancelingRetriever retriever = new CancelingRetriever();
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, observer.startedFetches.size());
      assertEquals(1, observer.endedFetches.size());
      assertEquals(AttemptStatus.CANCELED, observer.endedFetches.get(0).status());
      assertNotNull(observer.endedFetches.get(0).error());
    }

    @Test
    void recordsSkippedAttemptForPublishedClip() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      RejectingHistory history = new RejectingHistory();
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          history,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, observer.startedAttempts.size());
      assertEquals(PipelineStage.CLAIM, observer.startedAttempts.get(0).stage());
      assertEquals(1, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SKIPPED, observer.endedAttempts.get(0).status());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void recordsFailureAttemptForFailedClaim() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      ThrowingHistory history = new ThrowingHistory();
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          history,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, observer.startedAttempts.size());
      assertEquals(PipelineStage.CLAIM, observer.startedAttempts.get(0).stage());
      assertEquals(1, observer.endedAttempts.size());
      assertEquals(AttemptStatus.FAILURE, observer.endedAttempts.get(0).status());
      assertNotNull(observer.endedAttempts.get(0).error());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void recordsFailureAttemptForFailedDownload() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      ThrowingDownloader downloader = new ThrowingDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, observer.startedAttempts.size());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(0).stage());
      assertEquals(1, observer.endedAttempts.size());
      assertEquals(AttemptStatus.FAILURE, observer.endedAttempts.get(0).status());
      assertNotNull(observer.endedAttempts.get(0).error());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void recordsFailureAttemptForFailedTransform() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip), "test-pipeline");
      NoOpDownloader downloader = new NoOpDownloader();
      Pipeline pipeline = new Pipeline("test-pipeline", List.of(new ThrowingTransformer()));
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of("test-pipeline", pipeline),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(2, observer.startedAttempts.size());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(0).stage());
      assertEquals(PipelineStage.TRANSFORM, observer.startedAttempts.get(1).stage());
      assertEquals(2, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(0).status());
      assertEquals(AttemptStatus.FAILURE, observer.endedAttempts.get(1).status());
      assertNotNull(observer.endedAttempts.get(1).error());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void recordsFailureAttemptForFailedStage() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      ThrowingStager stager = new ThrowingStager();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          stager,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(2, observer.startedAttempts.size());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(0).stage());
      assertEquals(PipelineStage.STAGE, observer.startedAttempts.get(1).stage());
      assertEquals(2, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(0).status());
      assertEquals(AttemptStatus.FAILURE, observer.endedAttempts.get(1).status());
      assertNotNull(observer.endedAttempts.get(1).error());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void recordsFailureAttemptForFailedPublish() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      ThrowingPublisher publisher = new ThrowingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", publisher));

      Runner.run(context);

      assertEquals(2, observer.startedAttempts.size());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(0).stage());
      assertEquals(PipelineStage.PUBLISH, observer.startedAttempts.get(1).stage());
      assertEquals(2, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(0).status());
      assertEquals(AttemptStatus.FAILURE, observer.endedAttempts.get(1).status());
      assertNotNull(observer.endedAttempts.get(1).error());
    }

    @Test
    void recordsCanceledAttemptForCanceledDownload() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      CancelingDownloader downloader = new CancelingDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, observer.startedAttempts.size());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(0).stage());
      assertEquals(1, observer.endedAttempts.size());
      assertEquals(AttemptStatus.CANCELED, observer.endedAttempts.get(0).status());
      assertNotNull(observer.endedAttempts.get(0).error());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void recordsCanceledAttemptForCanceledTransform() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip), "test-pipeline");
      NoOpDownloader downloader = new NoOpDownloader();
      Pipeline pipeline = new Pipeline("test-pipeline", List.of(new CancelingTransformer()));
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of("test-pipeline", pipeline),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(2, observer.startedAttempts.size());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(0).stage());
      assertEquals(PipelineStage.TRANSFORM, observer.startedAttempts.get(1).stage());
      assertEquals(2, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(0).status());
      assertEquals(AttemptStatus.CANCELED, observer.endedAttempts.get(1).status());
      assertNotNull(observer.endedAttempts.get(1).error());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void recordsCanceledAttemptForCanceledStage() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      CancelingStager stager = new CancelingStager();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          stager,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(2, observer.startedAttempts.size());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(0).stage());
      assertEquals(PipelineStage.STAGE, observer.startedAttempts.get(1).stage());
      assertEquals(2, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(0).status());
      assertEquals(AttemptStatus.CANCELED, observer.endedAttempts.get(1).status());
      assertNotNull(observer.endedAttempts.get(1).error());
      assertEquals(0, tracker.published.get());
    }

    @Test
    void recordsCanceledAttemptForCanceledPublish() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      CancelingPublisher publisher = new CancelingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", publisher));

      Runner.run(context);

      assertEquals(2, observer.startedAttempts.size());
      assertEquals(PipelineStage.DOWNLOAD, observer.startedAttempts.get(0).stage());
      assertEquals(PipelineStage.PUBLISH, observer.startedAttempts.get(1).stage());
      assertEquals(2, observer.endedAttempts.size());
      assertEquals(AttemptStatus.SUCCESS, observer.endedAttempts.get(0).status());
      assertEquals(AttemptStatus.CANCELED, observer.endedAttempts.get(1).status());
      assertNotNull(observer.endedAttempts.get(1).error());
    }

    @Test
    void recordsFailedRunOnPreparationFailureLimit() {
      List<ClipRef> clips = List.of(
          new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null),
          new ClipRef("clip-2", null, "Title", "Broadcaster", "en", 90, null),
          new ClipRef("clip-3", null, "Title", "Broadcaster", "en", 80, null),
          new ClipRef("clip-4", null, "Title", "Broadcaster", "en", 70, null),
          new ClipRef("clip-5", null, "Title", "Broadcaster", "en", 60, null));
      TestRetriever retriever = new TestRetriever(clips);
      TrackingThrowingDownloader downloader = new TrackingThrowingDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, observer.endedRuns.size());
      assertEquals(RunStatus.FAILURE, observer.endedRuns.get(0).status());
      assertEquals(0, observer.endedRuns.get(0).published());
    }

    @Test
    void recordsFailedRunOnPublisherFailureLimit() {
      List<ClipRef> clips = List.of(
          new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null),
          new ClipRef("clip-2", null, "Title", "Broadcaster", "en", 90, null),
          new ClipRef("clip-3", null, "Title", "Broadcaster", "en", 80, null),
          new ClipRef("clip-4", null, "Title", "Broadcaster", "en", 70, null),
          new ClipRef("clip-5", null, "Title", "Broadcaster", "en", 60, null));
      TestRetriever retriever = new TestRetriever(clips);
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingThrowingPublisher publisher = new TrackingThrowingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", publisher));

      Runner.run(context);

      assertEquals(1, observer.endedRuns.size());
      assertEquals(RunStatus.FAILURE, observer.endedRuns.get(0).status());
      assertEquals(0, observer.endedRuns.get(0).published());
    }

    @Test
    void recordsCanceledRunOnUserCancel() {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      CancelingDownloader downloader = new CancelingDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 10L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      assertEquals(1, observer.endedRuns.size());
      assertEquals(RunStatus.CANCELED, observer.endedRuns.get(0).status());
      assertEquals(0, observer.endedRuns.get(0).published());
    }

    @Test
    void recordsHeartbeatsDuringRun() throws InterruptedException {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      BlockingPublisher publisher = new BlockingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 1L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", publisher));

      Thread thread = new Thread(() -> Runner.run(context));
      thread.start();

      try {
        assertTrue(observer.heartbeatLatch.await(3, TimeUnit.SECONDS));
      } finally {
        publisher.release();
        thread.join();
      }

      assertFalse(observer.heartbeats.isEmpty());
      assertEquals(1L, observer.heartbeats.get(0));
    }

    @Test
    void stopsHeartbeatsAfterRun() throws InterruptedException {
      ClipRef clip = new ClipRef("clip-1", null, "Title", "Broadcaster", "en", 100, null);
      TestRetriever retriever = new TestRetriever(List.of(clip));
      NoOpDownloader downloader = new NoOpDownloader();
      TrackingPublisher tracker = new TrackingPublisher();
      RecordingObserver observer = new RecordingObserver();
      RunnerContext context = new RunnerContext("test", 5, workDir, 1, 1, 3, 1L, null,
          observer,
          Map.of("r", retriever),
          null,
          downloader,
          Map.of(),
          null,
          Map.of("p", tracker));

      Runner.run(context);

      int initialCount = observer.heartbeats.size();
      Thread.sleep(2000);

      assertEquals(initialCount, observer.heartbeats.size());
    }

  }

  private static final class TestRetriever implements Retriever {

    private final List<ClipRef> clips;
    private final String pipeline;

    private TestRetriever(List<ClipRef> clips) {
      this(clips, null);
    }

    private TestRetriever(List<ClipRef> clips, String pipeline) {
      this.clips = clips;
      this.pipeline = pipeline;
    }

    @Override
    public String getName() {
      return "test_retriever";
    }

    @Override
    public String getPipeline() {
      return pipeline;
    }

    @Override
    public List<ClipRef> fetch(CancellationToken token) {
      return clips;
    }
  }

  private static final class ThrowingRetriever implements Retriever {

    @Override
    public String getName() {
      return "throwing_retriever";
    }

    @Override
    public String getPipeline() {
      return null;
    }

    @Override
    public List<ClipRef> fetch(CancellationToken token) {
      throw new RuntimeException("fetch failed");
    }
  }

  private static final class CancelingRetriever implements Retriever {

    @Override
    public String getName() {
      return "canceling_retriever";
    }

    @Override
    public String getPipeline() {
      return null;
    }

    @Override
    public List<ClipRef> fetch(CancellationToken token) {
      token.cancel(CancellationReason.USER_CANCELED);
      throw new RuntimeException("fetch canceled");
    }
  }

  private static final class TrackingHistory implements History {

    private final AtomicInteger contained = new AtomicInteger();
    private final AtomicInteger added = new AtomicInteger();

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
      return "tracking_history";
    }

    @Override
    public boolean contains(ClipRef clip, String runner) {
      contained.incrementAndGet();
      return false;
    }

    @Override
    public void add(ClipRef clip, String runner) {
      added.incrementAndGet();
    }
  }

  private static final class RejectingHistory implements History {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
      return "rejecting_history";
    }

    @Override
    public boolean contains(ClipRef clip, String runner) {
      return true;
    }

    @Override
    public void add(ClipRef clip, String runner) {
    }
  }

  private static final class ThrowingHistory implements History {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
      return "throwing_history";
    }

    @Override
    public boolean contains(ClipRef clip, String runner) {
      throw new RuntimeException("contains failed");
    }

    @Override
    public void add(ClipRef clip, String runner) {
    }
  }

  private static final class ThrowingDownloader implements Downloader {

    @Override
    public String getName() {
      return "throwing_downloader";
    }

    @Override
    public MediaRef download(ClipRef clip, Path target, CancellationToken token) {
      throw new RuntimeException("download failed");
    }
  }

  private static final class TrackingThrowingDownloader implements Downloader {

    private final AtomicInteger attempts = new AtomicInteger();

    @Override
    public String getName() {
      return "tracking_throwing_downloader";
    }

    @Override
    public MediaRef download(ClipRef clip, Path target, CancellationToken token) {
      attempts.incrementAndGet();
      throw new RuntimeException("download failed");
    }
  }

  private static final class NoOpDownloader implements Downloader {

    @Override
    public String getName() {
      return "no_op_downloader";
    }

    @Override
    public MediaRef download(ClipRef clip, Path target, CancellationToken token) {
      return new MediaRef(clip, target, null);
    }
  }

  private static final class CancelingDownloader implements Downloader {

    @Override
    public String getName() {
      return "canceling_downloader";
    }

    @Override
    public MediaRef download(ClipRef clip, Path target, CancellationToken token) {
      token.cancel(CancellationReason.USER_CANCELED);
      throw new RuntimeException("download canceled");
    }
  }

  private static final class ThrowingTransformer implements Transformer {

    @Override
    public String getName() {
      return "throwing_transformer";
    }

    @Override
    public MediaRef transform(MediaRef media, CancellationToken token) {
      throw new RuntimeException("transform failed");
    }
  }

  private static final class CancelingTransformer implements Transformer {

    @Override
    public String getName() {
      return "canceling_transformer";
    }

    @Override
    public MediaRef transform(MediaRef media, CancellationToken token) {
      token.cancel(CancellationReason.USER_CANCELED);
      throw new RuntimeException("transform canceled");
    }
  }

  private static final class TrackingStager implements Stager {

    private final AtomicInteger purged = new AtomicInteger();

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
      return "tracking_stager";
    }

    @Override
    public MediaRef stage(MediaRef media, CancellationToken token) {
      return media;
    }

    @Override
    public void clean(MediaRef media, CancellationToken token) {
    }

    @Override
    public void purge(CancellationToken token) {
      purged.incrementAndGet();
    }
  }

  private static final class ThrowingStager implements Stager {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
      return "throwing_stager";
    }

    @Override
    public MediaRef stage(MediaRef media, CancellationToken token) {
      throw new RuntimeException("stage failed");
    }

    @Override
    public void clean(MediaRef media, CancellationToken token) {
    }

    @Override
    public void purge(CancellationToken token) {
    }
  }

  private static final class NoOpStager implements Stager {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
      return "no_op_stager";
    }

    @Override
    public MediaRef stage(MediaRef media, CancellationToken token) {
      return media;
    }

    @Override
    public void clean(MediaRef media, CancellationToken token) {
    }

    @Override
    public void purge(CancellationToken token) {
    }
  }

  private static final class CancelingStager implements Stager {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
      return "canceling_stager";
    }

    @Override
    public MediaRef stage(MediaRef media, CancellationToken token) {
      token.cancel(CancellationReason.USER_CANCELED);
      throw new RuntimeException("stage canceled");
    }

    @Override
    public void clean(MediaRef media, CancellationToken token) {
    }

    @Override
    public void purge(CancellationToken token) {
    }
  }

  private static final class TrackingPublisher implements Publisher {

    private final AtomicInteger published = new AtomicInteger();

    @Override
    public String getName() {
      return "tracking_publisher";
    }

    @Override
    public PublishRef publish(MediaRef media, CancellationToken token) {
      published.incrementAndGet();
      return new PublishRef(media.clip(), null);
    }
  }

  private static final class ThrowingPublisher implements Publisher {

    @Override
    public String getName() {
      return "throwing_publisher";
    }

    @Override
    public PublishRef publish(MediaRef media, CancellationToken token) {
      throw new RuntimeException("publish failed");
    }
  }

  private static final class TrackingThrowingPublisher implements Publisher {

    private final AtomicInteger attempts = new AtomicInteger();

    @Override
    public String getName() {
      return "tracking_throwing_publisher";
    }

    @Override
    public PublishRef publish(MediaRef media, CancellationToken token) {
      attempts.incrementAndGet();
      throw new RuntimeException("publish failed");
    }
  }

  private static final class CancelingPublisher implements Publisher {

    @Override
    public String getName() {
      return "canceling_publisher";
    }

    @Override
    public PublishRef publish(MediaRef media, CancellationToken token) {
      token.cancel(CancellationReason.USER_CANCELED);
      throw new RuntimeException("publish canceled");
    }
  }

  private static final class NoOpPublisher implements Publisher {

    @Override
    public String getName() {
      return "no_op_publisher";
    }

    @Override
    public PublishRef publish(MediaRef media, CancellationToken token) {
      return new PublishRef(media.clip(), null);
    }
  }

  private static final class BlockingPublisher implements Publisher {

    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public String getName() {
      return "blocking_publisher";
    }

    @Override
    public PublishRef publish(MediaRef media, CancellationToken token) {
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return new PublishRef(media.clip(), null);
    }

    void release() {
      latch.countDown();
    }
  }

  private static final class RecordingObserver extends AbstractObserver {

    record StartedRun(String runner, String config) {
    }

    record EndedRun(long runId, RunStatus status, int published) {
    }

    record StartedFetch(long runId, String retriever, String worker) {
    }

    record EndedFetch(long fetchId, AttemptStatus status, int clips, Throwable error) {
    }

    record StartedAttempt(long runId, ClipRef clip, PipelineStage stage, String component, String worker) {
    }

    record EndedAttempt(long attemptId, AttemptStatus status, Throwable error) {
    }

    record Publish(long runId, String clipId, String publisher, String uri) {
    }

    private final List<StartedRun> startedRuns = new ArrayList<>();
    private final List<EndedRun> endedRuns = new ArrayList<>();
    private final List<StartedFetch> startedFetches = new ArrayList<>();
    private final List<EndedFetch> endedFetches = new ArrayList<>();
    private final List<StartedAttempt> startedAttempts = new ArrayList<>();
    private final List<EndedAttempt> endedAttempts = new ArrayList<>();
    private final List<Publish> publishes = new ArrayList<>();
    private final List<Long> heartbeats = new ArrayList<>();
    private final CountDownLatch heartbeatLatch = new CountDownLatch(1);
    private long nextId = 1;

    private RecordingObserver() {
      super(ConfigFactory.parseString("""
          name = recording
          type = recording
          """));
    }

    @Override
    public long runStart(String runner, String config) {
      startedRuns.add(new StartedRun(runner, config));
      return nextId++;
    }

    @Override
    public void runEnd(long runId, RunStatus status, int published) {
      endedRuns.add(new EndedRun(runId, status, published));
    }

    @Override
    public long fetchStart(long runId, String retriever, String worker) {
      startedFetches.add(new StartedFetch(runId, retriever, worker));
      return nextId++;
    }

    @Override
    public void fetchEnd(long fetchId, AttemptStatus status, int clips, Throwable error) {
      endedFetches.add(new EndedFetch(fetchId, status, clips, error));
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
      publishes.add(new Publish(runId, clipId, publisher, uri));
    }

    @Override
    public void heartbeat(long runId) {
      heartbeats.add(runId);
      heartbeatLatch.countDown();
    }
  }
}
