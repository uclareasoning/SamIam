package edu.ucla.belief.inference;

import edu.ucla.util.ChangeListener;
import edu.ucla.belief.InferenceEngine;
import il2.inf.structure.JoinTreeStats;

/** @author keith cascio
	@since  20030929 */
public interface JoinTreeInferenceEngine extends
  InferenceEngine,
  ChangeListener
{
	public JoinTreeStats.StatsSource getJoinTreeStats();
}
