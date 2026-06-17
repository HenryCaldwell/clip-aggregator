package info.henrycaldwell.streamline.observe;

/**
 * Enumeration of pipeline stages.
 * 
 * Each value represents a discrete stage in the lifecycle of a clip.
 */
public enum PipelineStage {

  CLAIM,
  DOWNLOAD,
  TRANSFORM,
  STAGE,
  PUBLISH,

}
