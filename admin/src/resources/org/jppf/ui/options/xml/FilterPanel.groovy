import org.jppf.management.*
import org.jppf.ui.monitoring.data.*
import org.jppf.node.policy.*
import org.jppf.ui.utils.*
import org.jppf.utils.LocalizationUtils
import java.awt.*

BASE = "org.jppf.ui.i18n.FilterPanel"

void init() {
  def editor = option.findFirstWithName("/node.filter.policy")
  def text = editor.getValue()
  if ((text == null) || text.trim().isEmpty()) editor.setValue(NodeFilterUtils.DEFAULT_EMPTY_FILTER)
  apply()
}

void apply() {
  def activeOption = option.findFirstWithName("/node.filter.activate")
  def b = activeOption.getValue()
  def manager = StatsHandler.getInstance().getTopologyManager()
  if (!b) {
    manager.setNodeFilter(null)
  } else {
    def text = option.findFirstWithName("/node.filter.policy").getValue()
    if ((text != null) && !text.trim().isEmpty()) {
      try {
        def policy = PolicyParser.parsePolicy(text)
        def selector = new ExecutionPolicySelector(policy)
        manager.setNodeFilter(selector)
      } catch(Exception e) {
        e.printStackTrace()
      }
    } else manager.setNodeFilter(null)
  }
  updateTabColor(activeOption)
}

void loadOrSave(loadFlag) {
  def editor = option.findFirstWithName("/node.filter.policy")
  def file = option.getValue()
  if (loadFlag) editor.setValue(FileUtils.readTextFile(file));
  else FileUtils.writeTextFile(file, editor.getValue())
}

void updateTabColor(activeOption) {
  def label = GuiUtils.getTabComponent(activeOption)
  if (label != null) {
    def filter = localize("FilterPanel.label")
    def state = localize("node.filter." + (activeOption.getValue() ? "on" : "off"))
    def color = activeOption.getValue() ? "green" : "red";
    label.setText("<html>" + filter + " <font color='" + color + "'>" + state + "</font</html>")
  }
}

void validate() {
  def editor = option.findFirstWithName("/node.filter.policy")
  def text = editor.getValue()
  if ((text != null) && !text.trim().isEmpty()) {
    def result = NodeFilterUtils.validatePolicy(text)
    def valid = result.first()
    def title = localize("node.filter." + (valid ? "valid" : "invalid") + ".title")
    def msg = localize("node.filter." + (valid ? "valid" : "invalid") + ".message")
    if (!valid) msg = msg + "\n" + result.second()
    def textArea = new JTextArea(msg, valid ? 1 : 8, 50)
    textArea.setLineWrap(true)
    textArea.setEditable(false)
    def scrollPane = new JScrollPane(textArea)
    scrollPane.setOpaque(false)
    GuiUtils.adjustScrollbarsThickness(scrollPane)
    scrollPane.setBorder(BorderFactory.createEmptyBorder())
    def comp = option.findFirstWithName("/FilterPanel").getUIComponent()
    def pane = new JOptionPane(scrollPane, valid ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE)
    def dialog = pane.createDialog(comp, title)
    dialog.show()
  }
}

String localize(final key) {
  return LocalizationUtils.getLocalized(BASE, key)
}
