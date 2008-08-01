var builder = null;
var page = option.findFirstWithName("/ChartsPage");
if (page != null)
{
	builder = page.findFirstWithName("/ChartsBuilder").getUIComponent();
}

function populateChartsList(tabConfig, chartConfig)
{
	var listOption = option.findFirstWithName("/ChartsList");
	if (tabConfig == null)
	{
		listOption.setItems(new ArrayList());
		listOption.setValue(null);
	}
	else
	{
		listOption.setItems(tabConfig.configs);
		values = new ArrayList();
		if (chartConfig != null) values.add(chartConfig);
		listOption.setValue(values);
	}
}

function populateTabsList(tab)
{
	var listOption = option.findFirstWithName("/TabsList");
	listOption.setItems(builder.getTabList());
	if (tab != null)
	{
		var value = new ArrayList();
		value.add(tab);
		listOption.setValue(value);
	}
}

function populateTabsCombo(tab)
{
	var comboOption = option.findFirstWithName("/TabName");
	comboOption.setItems(builder.getTabList());
	if (tab != null) comboOption.setValue(tab);
}

function populateFields(tab, config)
{
	option.findFirstWithName("/ChartName").setValue(config.name);
	option.findFirstWithName("/TabName").setValue(tab);
	option.findFirstWithName("/Unit").setValue(config.unit == null ? "" : config.unit);
	option.findFirstWithName("/Precision").setValue(config.precision);
	option.findFirstWithName("/ChartType").setValue(config.type);
	option.findFirstWithName("/FieldsList").setValue(CollectionUtils.list(config.fields));
}

function getPopulatedConfiguration()
{
	var config = new ChartConfiguration();
	config.name = option.findFirstWithName("/ChartName").getValue();
	config.unit = option.findFirstWithName("/Unit").getValue();
	if ("".equals(config.unit)) config.unit = null;
	config.precision = option.findFirstWithName("/Precision").getValue().intValue();
	config.type = option.findFirstWithName("/ChartType").getValue();
	var list = option.findFirstWithName("/FieldsList").getValue();
	var fields = java.lang.reflect.Array.newInstance(Fields, list.size());
	for (var i=0; i<fields.length; i++) fields[i] = list.get(i);
	config.fields = fields;
	return config;
}

function getTabConfig()
{
	var values = getListValues("TabsList");
	if (values.isEmpty()) return null;
	return values.get(0);
}

function getChartConfig()
{
	var values = getListValues("ChartsList");
	if (values.isEmpty()) return null;
	return values.get(0);
}

function getListValues(optionName)
{
	if (option == null) return new ArrayList();
	var listOption = option.findFirstWithName("/" + optionName);
	var values = listOption.getValue();
	if (values == null) return new ArrayList();
	return values;
}

function getListItems(optionName)
{
	if (option == null) return new ArrayList();
	var listOption = option.findFirstWithName("/" + optionName);
	var items = listOption.getItems();
	if (items == null) return new ArrayList();
	return items;
}

function changePreview(config)
{
	if (config != null)
	{
		var cfg = builder.createChart(config, true);
		var comp = option.findFirstWithName("/ChartPreview").getUIComponent();
		comp.removeAll();
		var c = new GridBagConstraints();
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		comp.add(cfg.chartPanel, c);
		cfg.chart.setBackgroundPaint(comp.getBackground());
		comp.updateUI();
	}
}

function resetAllEnabledStates()
{
	resetListEnabledStates("TabsList", "TabRemove", "TabUp", "TabDown");
	resetListEnabledStates("ChartsList", "ChartRemove", "ChartUp", "ChartDown");
}

function resetListEnabledStates(listName, btnName0, btnName1, btnName2)
{
	var list = getListValues(listName);
	var o = list.isEmpty() ? null : list.get(0);
	var items = getListItems(listName);
	var idx = (o == null) ? -1 : items.indexOf(o);
	option.findFirstWithName("/" + btnName0).setEnabled(o != null);
	option.findFirstWithName("/" + btnName1).setEnabled(idx > 0);
	option.findFirstWithName("/" + btnName2).setEnabled((idx >= 0) && (idx < items.size() - 1));
}

function tabMoved()
{
	root.setEventsEnabled(false);
	var tab = getTabConfig();
	var increment = "TabUp".equals(option.getName()) ? -1 : 1;
	builder.removeTab(tab);
	tab.position += increment;
	builder.addTab(tab);
	populateTabsList(tab);
	populateTabsCombo(tab);
	root.setEventsEnabled(true);
}

function chartMoved()
{
	root.setEventsEnabled(false);
	var chartConfig = getChartConfig();
	var tabConfig = getTabConfig();
	var increment = "ChartUp".equals(option.getName()) ? -1 : 1;
	builder.removeChart(tabConfig, chartConfig);
	chartConfig.position += increment;
	builder.addChart(tabConfig, chartConfig);
	populateChartsList(tabConfig, chartConfig);
	root.setEventsEnabled(true);
}

function initMain()
{
	option.setEventsEnabled(false);
	var values = CollectionUtils.list(ChartType.values());
	option.findFirstWithName("ChartType").setItems(values);
	values = CollectionUtils.list(StatsConstants.ALL_FIELDS);
	option.findFirstWithName("/FieldsList").setItems(values);
	populateTabsList(null);
	populateChartsList(null, null);
	populateTabsCombo(null);
	option.setEventsEnabled(true);
}

function doTabNew()
{
	root.setEventsEnabled(false);
	var BASE_NAME = "org/jppf/ui/options/xml/ChartsConfigPage";
	var s = JOptionPane.showInputDialog(option.getUIComponent(), StringUtils.getLocalized(BASE_NAME, "new.tab.name"),
		StringUtils.getLocalized(BASE_NAME, "new.tab.title"), JOptionPane.PLAIN_MESSAGE, null, null, null);
	if ((s != null) && !"".equals(s.trim()))
	{
		var tab = new TabConfiguration(s, -1);
		builder.addTab(tab);
		populateTabsList(tab);
		populateTabsCombo(tab);
		populateChartsList(tab, null);
	}
	root.setEventsEnabled(true);
}

function doTabRemove()
{
	root.setEventsEnabled(false);
	builder.removeTab(getTabConfig());
	populateTabsList(null);
	populateTabsCombo(null);
	populateChartsList(null, null);
	root.setEventsEnabled(true);
}

function doTabsList()
{
	root.setEventsEnabled(false);
	var values = option.getValue();
	if ((values != null) && !values.isEmpty())
	{
		populateChartsList(getTabConfig(), null);
		resetAllEnabledStates();
	}
	root.setEventsEnabled(true);
}

function doChartRemove()
{
	option.findElement("/").setEventsEnabled(false);
	var tabConfig = getTabConfig();
	var chartConfig = getChartConfig();
	builder.removeChart(tabConfig, chartConfig);
	populateChartsList(tabConfig, chartConfig);
	root.setEventsEnabled(true);
}

function doChartsList()
{
	root.setEventsEnabled(false);
	var values = option.getValue();
	if ((values != null) && !values.isEmpty())
	{
		var config = values.get(0);
		populateFields(getTabConfig(), config);
		changePreview(config);
		resetAllEnabledStates();
	}
	root.setEventsEnabled(true);
}

function doSaveNewChart()
{
	root.setEventsEnabled(false);
	var config = getPopulatedConfiguration();
	var tabConfig = getTabConfig();
	builder.addChart(tabConfig, config);
	populateTabsList(tabConfig);
	populateChartsList(tabConfig, config);
	root.setEventsEnabled(true);
}

function doUpdateChart()
{
	root.setEventsEnabled(false);
	var newTab = option.findFirstWithName("/TabName").getValue();
	var currentTab = getTabConfig();
	var oldConfig = getChartConfig();
	var newConfig = getPopulatedConfiguration();
	if (newTab == currentTab) newConfig.position = currentTab.configs.indexOf(oldConfig);
	builder.removeChart(currentTab, oldConfig);
	builder.addChart(newTab, newConfig);
	populateTabsList(newTab);
	populateChartsList(newTab, newConfig);
	root.setEventsEnabled(true);
}