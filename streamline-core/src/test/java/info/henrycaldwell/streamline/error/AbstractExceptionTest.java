package info.henrycaldwell.streamline.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AbstractExceptionTest {

  @Nested
  class GetMessage {

    @Test
    void formatsCategoryAndName() {
      TestException ex = new TestException("CAT", null, null, "name", "msg");

      assertTrue(ex.getMessage().startsWith("[CAT:name] msg"));
    }

    @Test
    void formatsWithNullCategory() {
      TestException ex = new TestException(null, null, null, "name", "msg");

      assertTrue(ex.getMessage().startsWith("[name] msg"));
    }

    @Test
    void formatsWithNullName() {
      TestException ex = new TestException("CAT", null, null, null, "msg");

      assertTrue(ex.getMessage().startsWith("[CAT] msg"));
    }

    @Test
    void omitsBracketsWhenAllFieldsAreNull() {
      TestException ex = new TestException(null, null, null, null, "msg");

      assertEquals("msg", ex.getMessage());
    }

    @Test
    void formatsWithType() {
      TestException ex = new TestException(null, ComponentType.RETRIEVER, null, null, "msg");

      assertTrue(ex.getMessage().startsWith("[retriever] msg"));
    }

    @Test
    void formatsWithCategoryAndType() {
      TestException ex = new TestException("CAT", ComponentType.RETRIEVER, null, null, "msg");

      assertTrue(ex.getMessage().startsWith("[CAT:retriever] msg"));
    }

    @Test
    void formatsWithTypeAndName() {
      TestException ex = new TestException(null, ComponentType.RETRIEVER, null, "name", "msg");

      assertTrue(ex.getMessage().startsWith("[retriever:name] msg"));
    }

    @Test
    void formatsWithCategoryTypeAndName() {
      TestException ex = new TestException("CAT", ComponentType.RETRIEVER, null, "name", "msg");

      assertTrue(ex.getMessage().startsWith("[CAT:retriever:name] msg"));
    }

    @Test
    void formatsWithParentAndName() {
      TestException ex = new TestException(null, null, "parent", "name", "msg");

      assertTrue(ex.getMessage().startsWith("[parent/name] msg"));
    }

    @Test
    void formatsWithCategoryParentAndName() {
      TestException ex = new TestException("CAT", null, "parent", "name", "msg");

      assertTrue(ex.getMessage().startsWith("[CAT:parent/name] msg"));
    }

    @Test
    void formatsWithTypeParentAndName() {
      TestException ex = new TestException(null, ComponentType.RETRIEVER, "parent", "name", "msg");

      assertTrue(ex.getMessage().startsWith("[retriever:parent/name] msg"));
    }

    @Test
    void formatsWithAllFields() {
      TestException ex = new TestException("CAT", ComponentType.RETRIEVER, "parent", "name", "msg");

      assertTrue(ex.getMessage().startsWith("[CAT:retriever:parent/name] msg"));
    }

    @Test
    void omitsParentWhenNameIsNull() {
      TestException ex = new TestException("CAT", null, "parent", null, "msg");

      assertTrue(ex.getMessage().startsWith("[CAT] msg"));
    }

    @Test
    void treatsBlankCategoryAsMissing() {
      TestException ex = new TestException("", ComponentType.RETRIEVER, null, "name", "msg");

      assertTrue(ex.getMessage().startsWith("[retriever:name] msg"));
    }

    @Test
    void treatsBlankParentAsMissing() {
      TestException ex = new TestException("CAT", null, "", "name", "msg");

      assertTrue(ex.getMessage().startsWith("[CAT:name] msg"));
    }

    @Test
    void treatsBlankNameAsMissing() {
      TestException ex = new TestException("CAT", null, null, "", "msg");

      assertTrue(ex.getMessage().startsWith("[CAT] msg"));
    }

    @Test
    void includesDetailsWhenPresent() {
      TestException ex = new TestException("CAT", null, null, "name", "msg", Map.of("key", "value"));

      assertTrue(ex.getMessage().contains("(key=value)"));
    }

    @Test
    void omitsDetailsWhenEmpty() {
      TestException ex = new TestException("CAT", null, null, "name", "msg", Map.of());

      assertFalse(ex.getMessage().contains("("));
    }

    @Test
    void omitsDetailsWhenNull() {
      TestException ex = new TestException("CAT", null, null, "name", "msg");

      assertFalse(ex.getMessage().contains("("));
    }
  }

  @Nested
  class GetCause {

    @Test
    void returnsCauseWhenProvided() {
      Throwable cause = new RuntimeException("cause");
      TestException ex = new TestException("CAT", null, null, "name", "msg", null, cause);

      assertEquals(cause, ex.getCause());
    }

    @Test
    void returnsNullWhenNoCause() {
      TestException ex = new TestException("CAT", null, null, "name", "msg");

      assertNull(ex.getCause());
    }
  }

  private static final class TestException extends AbstractException {

    TestException(String category, ComponentType type, String parent, String name, String message) {
      super(category, type, parent, name, message);
    }

    TestException(String category, ComponentType type, String parent, String name, String message, Map<String, ?> details) {
      super(category, type, parent, name, message, details);
    }

    TestException(String category, ComponentType type, String parent, String name, String message, Map<String, ?> details, Throwable cause) {
      super(category, type, parent, name, message, details, cause);
    }
  }
}
