void refresh()
{
	def connection = StatsHandler.getInstance().currentJmxConnection();
	if (connection == null)
	{
		option.findFirstWithName("/LoadBalancingMessages").setValue("Error : not connected to a server");
		return;
	}
	def info = connection.loadBalancerInformation();
	if (info != null)
	{
		def combo = option.findFirstWithName("/Algorithm");
		def items = combo.getItems();
		if ((items == null) || items.isEmpty())
		{
			combo.setItems(info.algorithmNames);
		}
		combo.setValue(info.algorithm);
		def params = option.findFirstWithName("/LoadBalancingParameters");
		params.setValue(info.parameters.asString());
	}
}

void apply()
{
	def connection = StatsHandler.getInstance().currentJmxConnection();
	if (connection == null)
	{
		option.findFirstWithName("/LoadBalancingMessages").setValue("Error : not connected to a server");
		return;
	}
	def algorithm =  option.findFirstWithName("/Algorithm").getValue();
	if (algorithm == null)
	{
		option.findFirstWithName("/LoadBalancingMessages").setValue("Error : no algorithm selected, use the 'Refresh' button to fill the list of algorithms");
		return;
	}
	def params = new TypedProperties();
	params.loadString(option.findFirstWithName("/LoadBalancingParameters").getValue());
	def msg = connection.changeLoadBalancerSettings(algorithm, params);
	if (msg != null) option.findFirstWithName("/LoadBalancingMessages").setValue(msg);
}
