void doPerform_Now()
{
	def shutdownDelay = option.findElement("../Shutdown_delay").getValue();
	def restartDelay = 0;
	def command;
	if (option.findElement("../Restart").getValue().booleanValue())
	{
		restartDelay = option.findElement("../Restart_delay").getValue();
		command = BundleParameter.SHUTDOWN_RESTART;
	}
	else command = BundleParameter.SHUTDOWN;
	def pwd = option.findFirstWithName("/actualPwd").getValue();
	def msg = StatsHandler.getInstance().requestShutdownRestart(pwd, command, shutdownDelay, restartDelay);
	option.findFirstWithName("/msgText").setValue(msg);
}

void doChange_password()
{
	def pwd = option.findFirstWithName("/actualPwd").getValue();
	def newPwd = option.findFirstWithName("/newPwd").getValue();
	def confirmPwd = option.findFirstWithName("/confirmPwd").getValue();
	if (validateNewPassword(newPwd, confirmPwd))
	{
		def msg = StatsHandler.getInstance().changeAdminPassword(pwd, newPwd);
		option.findFirstWithName("/msgText").setValue(msg);
	}
}

boolean  validateNewPassword(newPwd, confirmPwd)
{
	def msg;
	def result = true;
	if ((newPwd == null) || "".equals(newPwd))
	{
		msg = LocalizationUtils.getLocalized(BASE, "empty.pwd.msg");
		result = false;
	}
	else if (!newPwd.equals(confirmPwd))
	{
		msg = LocalizationUtils.getLocalized(BASE, "pwd.match.err.msg");
		result = false;
	}
	if (!result) option.findFirstWithName("/msgText").setValue(msg);
	return result;
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
