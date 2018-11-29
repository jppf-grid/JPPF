// add an instance of TestPeerDriverDiscovery in each server
function setDiscovery() {
  var driver = serverDebug.getDriver();
  var discovery = new org.jppf.test.addons.discovery.TestPeerDriverDiscovery();
  driver.addDriverDiscovery(discovery);
  return "ok";
}
setDiscovery();