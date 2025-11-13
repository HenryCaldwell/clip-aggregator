package info.henrycaldwell.aggregator.stage;

import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Interface for staging a media artifact to a remote location.
 * 
 * This interface defines a contract for producing a new media reference with a
 * remotely accessible URL from an input media reference.
 */
public interface Stager {

  /**
   * Initializes any underlying resources required by the stager.
   */
  void start();

  /**
   * Releases any resources acquired by {@link #start()}.
   */
  void stop();

  /**
   * Returns the configured stager name.
   *
   * @return A string representing the stager name.
   */
  String getName();

  /**
   * Stages the input media to a remote location.
   *
   * @param media A {@code MediaRef} representing the artifact to stage.
   * @return A {@code MediaRef} representing the staged artifact.
   */
  MediaRef stage(MediaRef media);
}
