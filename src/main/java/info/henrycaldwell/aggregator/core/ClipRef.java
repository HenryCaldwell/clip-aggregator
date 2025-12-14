package info.henrycaldwell.aggregator.core;

import java.util.List;

/**
 * Record for referencing a source clip.
 * 
 * This record defines a contract for carrying clip metadata used to fetch
 * and process clips from external platforms.
 */
public record ClipRef(
    String id,
    String url,
    String title,
    String broadcaster,
    String language,
    List<String> tags) {
}
