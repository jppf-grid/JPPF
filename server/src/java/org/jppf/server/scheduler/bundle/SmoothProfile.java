package org.jppf.server.scheduler.bundle;

/**
 * This class implements a smooth changing profile.
 * @author Domingos Creado
 */
public class SmoothProfile extends AnnealingTuneProfile {

	@Override
	float getSizeRatioDevitation() {
		return 1.5f; 
	}

	@Override
	float getDecreaseRatio() {
		return 0.2f; //will make it 
	}

	public long getMinSamplesToAnalyse() {
		return 500;
	}

	public long getMinSamplesToCheckConvergency() {
		return 300;
	}

	public double getMaxDevitation() {
		return 0.2;
	}

	public int getMaxGuessToStable() {
		return 10;
	}
}
