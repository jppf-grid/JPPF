void init() {
  def editor = option.findFirstWithName("/ExecutionPolicy")
  def text = editor.getValue()
  if ((text == null) || text.trim().isEmpty()) {
    editor.setValue(GuiUtils.DEFAULT_EMPTY_FILTER)
  }
}

void apply() {
  def activeOption = option.findFirstWithName("/ActivateFilter")
  def b = activeOption.getUIComponent().isSelected();
  def manager = StatsHandler.getInstance().getTopologyManager()
  if (!b) {
    manager.setNodeFilter(null)
  } else {
    def text = option.findFirstWithName("/ExecutionPolicy").getValue()
    if ((text != null) && !text.trim().isEmpty()) {
      try {
        def policy = PolicyParser.parsePolicy(text)
        def selector = new ExecutionPolicySelector(policy)
        manager.setNodeFilter(selector)
      } catch(Exception e) {
        e.printStackTrace()
      }
    } else {
      manager.setNodeFilter(null)
    }
  }
}

void loadOrSave(loadFlag) {
  def editor = option.findFirstWithName("/ExecutionPolicy")
  def file = option.getValue()
  if (loadFlag) editor.setValue(FileUtils.readTextFile(file));
  else FileUtils.writeTextFile(file, editor.getValue())
}
