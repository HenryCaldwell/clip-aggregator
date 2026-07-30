package info.henrycaldwell.streamline.error;

/**
 * Enumeration of component families reported in errors.
 *
 * Each value indicates a component category.
 */
public enum ComponentType {

  ROOT,
  CLI,
  OBSERVER,
  RETRIEVER,
  HISTORY,
  DOWNLOADER,
  PIPELINE,
  TRANSFORMER,
  STAGER,
  PUBLISHER,

}
