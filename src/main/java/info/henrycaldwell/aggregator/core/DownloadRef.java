package info.henrycaldwell.aggregator.core;

import java.nio.file.Path;

/**
 * Record for referencing a downloaded clip.
 * 
 * This record defines a contract for exposing the destination file and its size
 * in bytes.
 */
public record DownloadRef(
    String id,
    Path file,
    long bytes
) {}
