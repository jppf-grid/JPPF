function accepts() {
  // total nodes in the grid from the server statistics
  var totalNodes = jppfStats.getSnapshot("nodes").getLatest();
  var prio = jppfSla.getPriority();
  // determine max allowed nodes for the job, as % of total nodes
  var maxPct = (prio <= 1) ? 0.1 : (prio >= 9 ? 0.9 : prio / 10.0);
  // return true if current nodes for the job is less than max %
  return jppfDispatches < totalNodes * maxPct;
}
accepts();
