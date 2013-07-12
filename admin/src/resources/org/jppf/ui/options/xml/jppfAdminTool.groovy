def comp = option.findFirstWithName("/AdminPanels").getUIComponent();
def frame = new JFrame(option.getLabel());
OptionsHandler.setMainWindow(frame);
DockingManager.getInstance().setMainView(frame);
frame.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/jppf-icon.gif").getImage());
frame.addWindowListener(new WindowClosingListener());
def statsHandler = StatsHandler.getInstance();
comp.setSelectedIndex(0);
frame.getContentPane().add(option.getUIComponent());
OptionsHandler.loadMainWindowAttributes(OptionsHandler.getPreferences().node("JPPFAdminTool"));
//frame.setVisible(true);
