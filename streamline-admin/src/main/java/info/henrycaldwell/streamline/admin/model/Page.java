package info.henrycaldwell.streamline.admin.model;

import java.util.List;

public record Page<T>(
    List<T> items,
    String cursor,
    boolean hasMore) {
}
