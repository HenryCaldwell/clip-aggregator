package info.henrycaldwell.aggregator.core;

/**
 * Record for referencing a clip to download.
 * 
 * This record defines a contract for carrying minimal fields required by
 * downloaders.
 */
public record ClipRef(
    String id,
    String url,
    String title,
    String broadcaster,
    String language
) {}
