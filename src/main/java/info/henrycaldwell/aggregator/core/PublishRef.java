package info.henrycaldwell.aggregator.core;

import java.net.URI;

/**
 * Record for referencing a published clip.
 * 
 * This record defines a contract for carrying metadata used to identify a clip
 * published to an external platform.
 */
public record PublishRef(
    String id,
    URI uri) {
}
