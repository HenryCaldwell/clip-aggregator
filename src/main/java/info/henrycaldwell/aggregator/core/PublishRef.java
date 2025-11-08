package info.henrycaldwell.aggregator.core;

import java.net.URI;

/**
 * Record for referencing a publish result.
 * 
 * This record defines a contract for carrying the local clip identifier and a
 * URI representing publish location.
 */
public record PublishRef(
    String id,
    URI uri
) {}
