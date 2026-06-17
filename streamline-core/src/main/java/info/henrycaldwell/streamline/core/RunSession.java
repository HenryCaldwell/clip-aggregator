package info.henrycaldwell.streamline.core;

/**
 * Record for capturing the state of a run.
 * 
 * This record defines a contract for carrying the identifiers and signals
 * scoped to a single run.
 */
public record RunSession(
    long runId,
    CancellationToken token) {
}
