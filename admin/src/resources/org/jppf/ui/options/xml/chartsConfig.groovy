chartBuilder = option.findFirstWithName("/ChartsBuilder").getUIComponent();
pageRoot = option.findFirstWithName("/ChartsConfiguration");

void populateChartsList(tabConfig, chartConfig)
{
	def listOption = option.findFirstWithName("/ChartsList");
	if (tabConfig == null)
	{
		listOption.setItems(new ArrayList());
		listOption.setValue(null);
	}
	else
	{
		listOption.setItems(tabConfig.configs);
		def values = new ArrayList();
		if (chartConfig != null) values.add(chartConfig);
		listOption.setValue(values);
	}
}

void populateTabsList(tab)
{
	def listOption = option.findFirstWithName("/TabsList");
	listOption.setItems(chartBuilder.getTabList());
	if (tab != null)
	{
		def value = new ArrayList();
		value.add(tab);
		listOption.setValue(value);
	}
}

void populateTabsCombo(tab)
{
	def comboOption = option.findFirstWithName("/TabName");
	comboOption.setItems(chartBuilder.getTabList());
	if (tab != null) comboOption.setValue(tab);
}

void populateFields(tab, config)
{
	option.findFirstWithName("/ChartName").setValue(config.name);
	option.findFirstWithName("/TabName").setValue(tab);
	option.findFirstWithName("/Unit").setValue(config.unit == null ? "" : config.unit);
	option.findFirstWithName("/Precision").setValue(config.precision);
	option.findFirstWithName("/ChartType").setValue(config.type);
	option.findFirstWithName("/FieldsList").setValue(CollectionUtils.list(config.fields));
}

ChartConfiguration getPopulatedConfiguration()
{
	def config = new ChartConfiguration();
	config.name = option.findFirstWithName("/ChartName").getValue();
	config.unit = option.findFirstWithName("/Unit").getValue();
	if ("".equals(config.unit)) config.unit = null;
	config.precision = option.findFirstWithName("/Precision").getValue().intValue();
	config.type = option.findFirstWithName("/ChartType").getValue();
	def list = option.findFirstWithName("/FieldsList").getValue();
	def fields = java.lang.reflect.Array.newInstance(Fields, list.size());
	for (def i=0; i<fields.length; i++) fields[i] = list.get(i);
	config.fields = fields;
	return config;
}

TabConfiguration getTabConfig()
{
	def values = getListValues("TabsList");
	if (values.isEmpty()) return null;
	return values.get(0);
}

ChartConfiguration getChartConfig()
{
	def values = getListValues("ChartsList");
	if (values.isEmpty()) return null;
	return values.get(0);
}

List getListValues(optionName)
{
	if (option == null) return new ArrayList();
	def listOption = option.findFirstWithName("/" + optionName);
	def values = listOption.getValue();
	if (values == null) return new ArrayList();
	return values;
}

List getListItems(optionName)
{
	if (option == null) return new ArrayList();
	def listOption = option.findFirstWithName("/" + optionName);
	def items = listOption.getItems();
	if (items == null) return new ArrayList();
	return items;
}

void changePreview(config)
{
	if (config != null)
	{
		def cfg = chartBuilder.createChart(config, true);
		def comp = option.findFirstWithName("/ChartPreview").getUIComponent();
		comp.removeAll();
		comp.add(cfg.chartPanel);
		cfg.chart.setBackgroundPaint(comp.getBackground());
		comp.updateUI();
	}
}

void resetAllEnabledStates()
{
	resetListEnabledStates("TabsList", "TabRemove", "TabUp", "TabDown");
	resetListEnabledStates("ChartsList", "ChartRemove", "ChartUp", "ChartDown");
}

void resetListEnabledStates(listName, btnName0, btnName1, btnName2)
{
	def list = getListValues(listName);
	def o = list.isEmpty() ? null : list.get(0);
	def items = getListItems(listName);
	def idx = (o == null) ? -1 : items.indexOf(o);
	option.findFirstWithName("/" + btnName0).setEnabled(o != null);
	option.findFirstWithName("/" + btnName1).setEnabled(idx > 0);
	option.findFirstWithName("/" + btnName2).setEnabled((idx >= 0) && (idx < items.size() - 1));
}

void tabMoved()
{
	pageRoot.setEventsEnabled(false);
	def tab = getTabConfig();
	def increment = "TabUp".equals(option.getName()) ? -1 : 1;
	chartBuilder.removeTab(tab);
	tab.position += increment;
	chartBuilder.addTab(tab);
	populateTabsList(tab);
	populateTabsCombo(tab);
	pageRoot.setEventsEnabled(true);
}

void chartMoved()
{
	pageRoot.setEventsEnabled(false);
	def chartConfig = getChartConfig();
	def tabConfig = getTabConfig();
	def increment = "ChartUp".equals(option.getName()) ? -1 : 1;
	chartBuilder.removeChart(tabConfig, chartConfig);
	chartConfig.position += increment;
	chartBuilder.addChart(tabConfig, chartConfig);
	populateChartsList(tabConfig, chartConfig);
	pageRoot.setEventsEnabled(true);
}

void initMain()
{
	option.setEventsEnabled(false);
	def values = CollectionUtils.list(ChartType.values());
	option.findFirstWithName("ChartType").setItems(values);
	values = CollectionUtils.list(StatsConstants.ALL_FIELDS);
	option.findFirstWithName("/FieldsList").setItems(values);
	populateTabsList(null);
	populateChartsList(null, null);
	populateTabsCombo(null);
	option.setEventsEnabled(true);
}

void doTabNew()
{
	pageRoot.setEventsEnabled(false);
	def BASE_NAME = "org/jppf/ui/options/xml/ChartsConfigPage";
	def s = JOptionPane.showInputDialog(option.getUIComponent(), StringUtils.getLocalized(BASE_NAME, "new.tab.name"),
		StringUtils.getLocalized(BASE_NAME, "new.tab.title"), JOptionPane.PLAIN_MESSAGE, null, null, null);
	if ((s != null) && !"".equals(s.trim()))
	{
		def tab = new TabConfiguration(s, -1);
		chartBuilder.addTab(tab);
		populateTabsList(tab);
		populateTabsCombo(tab);
		populateChartsList(tab, null);
	}
	pageRoot.setEventsEnabled(true);
}

void doTabRemove()
{
	pageRoot.setEventsEnabled(false);
	chartBuilder.removeTab(getTabConfig());
	populateTabsList(null);
	populateTabsCombo(null);
	populateChartsList(null, null);
	pageRoot.setEventsEnabled(true);
}

void doTabsList()
{
	pageRoot.setEventsEnabled(false);
	def values = option.getValue();
	if ((values != null) && !values.isEmpty())
	{
		populateChartsList(getTabConfig(), null);
		resetAllEnabledStates();
	}
	pageRoot.setEventsEnabled(true);
}

void doChartRemove()
{
	option.findElement("/").setEventsEnabled(false);
	def tabConfig = getTabConfig();
	def chartConfig = getChartConfig();
	chartBuilder.removeChart(tabConfig, chartConfig);
	populateChartsList(tabConfig, chartConfig);
	pageRoot.setEventsEnabled(true);
}

void doChartsList()
{
	pageRoot.setEventsEnabled(false);
	def values = option.getValue();
	if ((values != null) && !values.isEmpty())
	{
		def config = values.get(0);
		populateFields(getTabConfig(), config);
		changePreview(config);
		resetAllEnabledStates();
	}
	pageRoot.setEventsEnabled(true);
}

void doSaveNewChart()
{
	pageRoot.setEventsEnabled(false);
	def config = getPopulatedConfiguration();
	def tabConfig = getTabConfig();
	chartBuilder.addChart(tabConfig, config);
	populateTabsList(tabConfig);
	populateChartsList(tabConfig, config);
	pageRoot.setEventsEnabled(true);
}

void doUpdateChart()
{
	pageRoot.setEventsEnabled(false);
	def newTab = option.findFirstWithName("/TabName").getValue();
	def currentTab = getTabConfig();
	def oldConfig = getChartConfig();
	def newConfig = getPopulatedConfiguration();
	if (newTab == currentTab) newConfig.position = currentTab.configs.indexOf(oldConfig);
	chartBuilder.removeChart(currentTab, oldConfig);
	chartBuilder.addChart(newTab, newConfig);
	populateTabsList(newTab);
	populateChartsList(newTab, newConfig);
	pageRoot.setEventsEnabled(true);
}