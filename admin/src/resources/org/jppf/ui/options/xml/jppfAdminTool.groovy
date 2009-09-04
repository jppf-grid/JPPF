def comp = option.findFirstWithName("/AdminPanels").getUIComponent();
def frame = new JFrame(option.getLabel());
frame.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/jppf-icon.gif").getImage());
frame.addWindowListener(new WindowClosingListener());
def statsHandler = StatsHandler.getInstance();
comp.setSelectedIndex(0);
frame.getContentPane().add(option.getUIComponent());
frame.setSize(700, 768);
frame.setVisible(true);
