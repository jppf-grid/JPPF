void algorithmChanged()
{
	def value = option.findFirstWithName("/Algorithm").getValue();
	option.findFirstWithName("/ManualConfig").setEnabled("manual".equals(value));
	option.findFirstWithName("/AutoConfig").setEnabled("autotuned".equals(value));
	option.findFirstWithName("/ProportionalConfig").setEnabled("proportional".equals(value));
}

void refreshManual()
{
	def n = StatsHandler.getInstance().getLatestStats().bundleSize;
	if (n > 0)
	{
		option.findFirstWithName("/bundleSize").setValue(new Long(n));
	}
}

void applyManual()
{
	def page = option.findFirstWithName("/Admin");
	def pwd = page.findFirstWithName("/actualPwd").getValue();
	def params = new HashMap();
	params.put(BundleParameter.BUNDLE_SIZE_PARAM, option.findFirstWithName("/bundleSize").getValue());
	params.put(BundleParameter.BUNDLE_TUNING_TYPE_PARAM, "manual");
	def msg = StatsHandler.getInstance().changeSettings(pwd, params);
	if (msg != null) option.findFirstWithName("/TuningMessages").setValue(msg);
}

void applyAutotuned()
{
	def page = option.findFirstWithName("/Admin");
	def pwd = page.findFirstWithName("actualPwd").getValue();
	def params = new HashMap();
	params.put(BundleParameter.BUNDLE_TUNING_TYPE_PARAM, "autotuned");
	params.put(BundleParameter.MIN_SAMPLES_TO_ANALYSE, option.findFirstWithName("/MinSamplesToAnalyse").getValue());
	params.put(BundleParameter.MIN_SAMPLES_TO_CHECK_CONVERGENCE, option.findFirstWithName("/MinSamplesToCheckConvergence").getValue());
	params.put(BundleParameter.MAX_DEVIATION, option.findFirstWithName("/MaxDeviation").getValue());
	params.put(BundleParameter.MAX_GUESS_TO_STABLE, option.findFirstWithName("/MaxGuessToStable").getValue());
	params.put(BundleParameter.SIZE_RATIO_DEVIATION, option.findFirstWithName("/SizeRatioDeviation").getValue());
	params.put(BundleParameter.DECREASE_RATIO, option.findFirstWithName("/DecreaseRatio").getValue());
	def msg = StatsHandler.getInstance().changeSettings(pwd, params);
	if (msg != null) option.findFirstWithName("/TuningMessages").setValue(msg);
}

void applyProportional()
{
	def page = option.findFirstWithName("/Admin");
	def pwd = page.findFirstWithName("actualPwd").getValue();
	def params = new HashMap();
	params.put(BundleParameter.BUNDLE_TUNING_TYPE_PARAM, "proportional");
	params.put(BundleParameter.PERFORMANCE_CACHE_SIZE, option.findFirstWithName("/PerformanceCacheSize").getValue());
	params.put(BundleParameter.PROPORTIONALITY_FACTOR, option.findFirstWithName("/ProportionalityFactor").getValue());
	def msg = StatsHandler.getInstance().changeSettings(pwd, params);
	if (msg != null) option.findFirstWithName("/TuningMessages").setValue(msg);
}
