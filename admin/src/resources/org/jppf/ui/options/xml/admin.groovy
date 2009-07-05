void doPerform_Now()
{
	def shutdownDelay = option.findElement("../Shutdown_delay").getValue();
	def restartDelay = -1;
	if (option.findElement("../Restart").getValue().booleanValue())
	{
		restartDelay = option.findElement("../Restart_delay").getValue();
	}
	def msg = StatsHandler.getInstance().requestShutdownRestart(shutdownDelay, restartDelay);
	option.findFirstWithName("/msgText").setValue(msg);
}

void doApplyInterval()
{
	def interval = option.findElement("../Interval").getValue().longValue();
	def handler = StatsHandler.getInstance();
	if (interval != handler.getRefreshInterval())
	{
		handler.setRefreshInterval(interval);
		handler.stopRefreshTimer();
		handler.startRefreshTimer();
	}
}
