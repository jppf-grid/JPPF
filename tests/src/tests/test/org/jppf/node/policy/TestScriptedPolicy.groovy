// total nodes in the grid from the server statistics
def totalNodes = jppfStats.getSnapshot("nodes").getLatest()
def prio = jppfSla.getPriority()
// determine max allowed nodes for the job, as % of total nodes
def maxPct = (prio <= 1) ? 0.1 : (prio >= 9 ? 0.9 : prio / 10.0)
// return true if current nodes for the job is less than max %
return jppfDispatches < (int) (totalNodes * maxPct)