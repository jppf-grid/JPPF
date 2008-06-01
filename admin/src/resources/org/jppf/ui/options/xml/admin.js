function doPerform_Now()
{
	shutdownDelay = option.findElement("../Shutdown_delay").getValue();
	restartDelay = 0;
	var command;
	if (option.findElement("../Restart").getValue().booleanValue())
	{
		restartDelay = option.findElement("../Restart_delay").getValue();
		command = BundleParameter.SHUTDOWN_RESTART;
	}
	else command = BundleParameter.SHUTDOWN;
	pwd = option.findFirstWithName("/actualPwd").getValue();
	msg = StatsHandler.getInstance().requestShutdownRestart(pwd, command, shutdownDelay, restartDelay);
	option.findFirstWithName("/msgText").setValue(msg);
}

function doChange_password()
{
	pwd = option.findFirstWithName("/actualPwd").getValue();
	newPwd = option.findFirstWithName("/newPwd").getValue();
	confirmPwd = option.findFirstWithName("/confirmPwd").getValue();
	if (validateNewPassword(newPwd, confirmPwd))
	{
		msg = StatsHandler.getInstance().changeAdminPassword(pwd, newPwd);
		option.findFirstWithName("/msgText").setValue(msg);
	}
}

function validateNewPassword(newPwd, confirmPwd)
{
	var msg;
	result = true;
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

function doApplyInterval()
{
	interval = option.findElement("../Interval").getValue().longValue();
	handler = StatsHandler.getInstance();
	if (interval != handler.getRefreshInterval())
	{
		handler.setRefreshInterval(interval);
		handler.stopRefreshTimer();
		handler.startRefreshTimer();
	}
}

