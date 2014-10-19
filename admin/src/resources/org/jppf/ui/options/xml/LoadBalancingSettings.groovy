void refresh() {
  StatsHandler.getInstance().getClientHandler().refreshLoadBalancer();
}

void apply() {
  def connection = StatsHandler.getInstance().getClientHandler().currentJmxConnection();
  if (connection == null) {
    option.findFirstWithName("/LoadBalancingMessages").setValue("Not connected to a server");
    return;
  }
  def algorithm =  option.findFirstWithName("/Algorithm").getValue();
  if (algorithm == null) {
    option.findFirstWithName("/LoadBalancingMessages").setValue("No algorithm selected, use the 'Refresh' button to fill the list of algorithms");
    return;
  }
  def params = new TypedProperties();
  params.load(new StringReader(option.findFirstWithName("/LoadBalancingParameters").getValue()));
  def msg = connection.changeLoadBalancerSettings(algorithm, params);
  if (msg != null) option.findFirstWithName("/LoadBalancingMessages").setValue(msg);
}
