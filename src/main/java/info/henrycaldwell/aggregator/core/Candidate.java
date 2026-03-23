package info.henrycaldwell.aggregator.core;

import info.henrycaldwell.aggregator.retrieve.Retriever;
import info.henrycaldwell.aggregator.transform.Pipeline;

/**
 * Record for carrying a candidate clip.
 *
 * This record defines a contract for carrying a clip and its associated
 * retriever and pipeline for use by preparation workers.
 */
record Candidate(
    Retriever retriever,
    Pipeline pipeline,
    ClipRef clip) implements Comparable<Candidate> {

  /**
   * Compares this candidate to another by view count in descending order.
   *
   * @param other A {@link Candidate} representing the candidate to compare to.
   * @return An integer representing the comparison result.
   */
  @Override
  public int compareTo(Candidate other) {
    return Integer.compare(other.clip().views(), this.clip().views());
  }
}