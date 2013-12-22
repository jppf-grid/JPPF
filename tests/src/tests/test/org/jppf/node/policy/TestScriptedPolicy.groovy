import org.jppf.management.*
import org.jppf.node.protocol.*
import org.jppf.utils.*
import org.jppf.utils.collections.*
import org.jppf.utils.stats.*

def totalNodes = jppfStats.getSnapshot("nodes").getLatest()
def maxPct = 0.0
def priority = jppfSla.getPriority()
if (priority <= 1) maxPct = 0.1
else if (priority >= 9) maxPct = 0.9
else maxPct = priority / 10.0
println("totalNodes * maxPct = " + (totalNodes * maxPct))
println("jppfDispatches = " + jppfDispatches)
return jppfDispatches < (int) (totalNodes * maxPct)
