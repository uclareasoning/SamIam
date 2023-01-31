package edu.ucla.belief.approx;

import edu.ucla.util.ChangeListener;
import edu.ucla.belief.InferenceEngine;

/** @author arthur choi
	@since  20050505 */
public interface PropagationInferenceEngine extends
  ChangeListener,
  InferenceEngine
{
	public String convergenceSummary( boolean identify );
}
