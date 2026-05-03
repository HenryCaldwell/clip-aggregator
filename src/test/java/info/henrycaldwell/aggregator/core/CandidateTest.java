package info.henrycaldwell.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CandidateTest {

  @Nested
  class CompareTo {

    @Test
    void sortsCandidatesByViewsDescending() {
      Candidate lowViews = new Candidate(null, null, new ClipRef("low", null, null, null, null, 100, null));
      Candidate highViews = new Candidate(null, null, new ClipRef("high", null, null, null, null, 500, null));
      Candidate mediumViews = new Candidate(null, null, new ClipRef("medium", null, null, null, null, 300, null));
      List<Candidate> candidates = new ArrayList<>(List.of(lowViews, highViews, mediumViews));

      candidates.sort(null);

      assertEquals("high", candidates.get(0).clip().id());
      assertEquals("medium", candidates.get(1).clip().id());
      assertEquals("low", candidates.get(2).clip().id());
    }

    @Test
    void returnsZeroForCandidatesWithSameViews() {
      Candidate first = new Candidate(null, null, new ClipRef("first", null, null, null, null, 100, null));
      Candidate second = new Candidate(null, null, new ClipRef("second", null, null, null, null, 100, null));

      int result = first.compareTo(second);

      assertEquals(0, result);
    }

    @Test
    void returnsNegativeWhenThisCandidateHasMoreViews() {
      Candidate highViews = new Candidate(null, null, new ClipRef("high", null, null, null, null, 500, null));
      Candidate lowViews = new Candidate(null, null, new ClipRef("low", null, null, null, null, 100, null));

      int result = highViews.compareTo(lowViews);

      assertTrue(result < 0);
    }
  }
}
