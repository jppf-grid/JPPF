// get info on discovered peer drivers
function retrieve() {
  var driver = serverDebug.getDriver();
  var peers = new java.util.ArrayList(driver.getInitializer().getDiscoveredPeers());
  if (peers.size() != 1) return "ko: size=" + peers.size();
  var peer = peers.get(0); 
  var sb = new java.lang.StringBuilder();
  sb.append("host=").append(peer.getHost()).append("\n");
  sb.append("port=").append(peer.getPort()).append("\n");
  sb.append("name=").append(peer.getName()).append("\n");
  sb.append("secure=").append(peer.isSecure()).append("\n");
  var channels = driver.getAsyncNodeNioServer().getAllChannels();
  var size = channels.size();
  if (size != 2) return "ko: nbChannels = " + size;
  var peerCount = 0;
  for (i=0; i<size; i++) {
    var channel = channels.get(i);
    if (channel.isPeer()) {
      peerCount++;
      if (peerCount > 1) return "ko: peerCount = " + peerCount;
      sb.append("channel.secure=").append(channel.isSecure()).append("\n");
      var key = channel.getSelectionKey();
      var port = key.channel().socket().getLocalPort();
      sb.append("channel.local.port=").append(port).append("\n");
    }
  }
  return sb.toString();
}
retrieve();