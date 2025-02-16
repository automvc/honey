/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.util.StringUtils;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingConfigParse {

	public ShardingConfigMeta parseForSharding(String str, int assignType) {
//		String str="ds[0..2].orders[0..5]";

		if (StringUtils.isBlank(str)) return null;

		NodeBean bean = parse(str, assignType);

		Map<String, Map<String, Set<String>>> fullNodes = new HashMap<>();// 1
		Map<String, String> tabToDsMap = new LinkedHashMap<>(); // 2
		Map<String, String> tabToBaseMap = new HashMap<>();

		List<String> dsList = bean.getDsList();
		List<String> tabList = bean.getTabList();

		int dsNum = dsList.size();
		int tabNum = tabList.size();
		int size = (tabNum + dsNum - 1) / dsNum;

		Map<String, Set<String>> ds2TabIndexSet = new LinkedHashMap<>(); // 1

		for (int i = 0; i < dsList.size(); i++) {
			Set<String> tabIndexSet = new TreeSet<>();
			int n = size;
			for (int k = size * i; n > 0 && k < tabNum; k++, n--) {
				tabIndexSet.add(bean.getSepTab() + tabList.get(k)); // 加分隔
				String tab = bean.getTabBaseName() + bean.getSepTab() + tabList.get(k);
				tabToDsMap.put(tab, bean.getDsBaseName() + dsList.get(i));
				tabToBaseMap.put(tab, bean.getTabBaseName());// 2.4.2
			}
			ds2TabIndexSet.put(bean.getDsBaseName() + dsList.get(i), tabIndexSet);
		}

		fullNodes.put(bean.getTabBaseName().toLowerCase(), ds2TabIndexSet);

		Logger.info("[Bee] fullNodes: " + fullNodes.toString());
		Logger.info("[Bee] tabToDsMap: " + tabToDsMap.toString()); // 只分库时,map只会保留最后一个的
		if (StringUtils.isNotEmpty(bean.getSepTab()))
			Logger.info("[Bee] " + bean.getTabBaseName() + ", its sepTab is: '" + bean.getSepTab() + "'");

		ShardingConfigMeta shardingConfigMeta = new ShardingConfigMeta();
		shardingConfigMeta.setFullNodes(fullNodes);
		shardingConfigMeta.setTabToDsMap(tabToDsMap);
		shardingConfigMeta.setTabSize(tabList.size());
		shardingConfigMeta.setTabBaseName(bean.getTabBaseName());
		shardingConfigMeta.setTabToBase(tabToBaseMap);// 2.4.2

		shardingConfigMeta.setSepTab(bean.getSepTab());

		return shardingConfigMeta;
	}

	public ShardingConfigMeta parseForSharding(String str) {
		return parseForSharding(str, 0); // table 节点按顺序平均分配
	}

	private NodeBean parse(String str, int assignType) {
		NodeBean nodes = new NodeBean();

//		String str="ds[0..2].orders[0..5]";
		str = str.replace(" ", "");
		Logger.info(str);

		int mid = str.indexOf("].");

		int index1 = str.indexOf('[');
		String dsNum = str.substring(index1 + 1, mid);

		nodes.setDsBaseName(str.substring(0, index1));

		String dsNumArray[] = dsNum.split("\\.\\.");
		if (dsNumArray.length == 2) {
			nodes.setDsIndex0(Integer.parseInt(dsNumArray[0]));
			nodes.setDsIndex1(Integer.parseInt(dsNumArray[1]));
		} else {
			String dsNumArray2[] = dsNum.split(",");
			List<String> dsList = new ArrayList<>();
			for (int i = 0; i < dsNumArray2.length; i++) {
				dsList.add(dsNumArray2[i]);
			}
			nodes.setDsList(dsList);

		}

		int dsMin = nodes.getDsIndex0();
		int dsMax = nodes.getDsIndex1();
		if (dsMin != -1) {
			nodes.setDsList(Assign.order(dsMin, dsMax));
		}

		int index2 = str.indexOf('[', mid + 2);
		String tabNum = str.substring(index2 + 1, str.length() - 1);
		String tabNumArray[] = tabNum.split("\\.\\.");
		if (tabNumArray.length == 2) {
			nodes.setTabIndex0(Integer.parseInt(tabNumArray[0]));
			nodes.setTabIndex1(Integer.parseInt(tabNumArray[1]));
		} else {
			String tabNumArray2[] = tabNum.split(",");
			int len = tabNumArray2.length;
			List<String> tabList = new ArrayList<>();
			if (len > 1) {
				for (int i = 0; i < len; i++) {
					tabList.add(tabNumArray2[i]);
				}
			} else {// 为了只分库
				len = nodes.getDsList().size();
				for (int i = 0; i < len; i++) {
					tabList.add("");
				}
			}
			nodes.setTabList(tabList);
		}
		String temp_tabBaseName = str.substring(mid + 2, index2);
		String sepTab = "";
		if (temp_tabBaseName != null && (temp_tabBaseName.endsWith("_") || temp_tabBaseName.endsWith("-"))) {
			int len = temp_tabBaseName.length();
			sepTab = temp_tabBaseName.substring(len - 1, len);
			nodes.setSepTab(sepTab);
			temp_tabBaseName = temp_tabBaseName.substring(0, len - 1);
		}
		nodes.setTabBaseName(temp_tabBaseName);

		// 通过顺序号设置

		int tabMin = nodes.getTabIndex0();
		int tabMax = nodes.getTabIndex1();
		if (tabMin != -1) {
			if (assignType == 1)
				nodes.setTabList(Assign.polling(tabMin, tabMax, nodes.getDsList().size())); // polling
			else
				nodes.setTabList(Assign.order(tabMin, tabMax)); // 按顺序平均分配
		}

		return nodes;
	}

	private class NodeBean {

		private String dsBaseName;
		private String tabBaseName;

		// 分隔符,只支持"_"; "-"很多数据库都不支持，不要用
		private String sepTab = ""; // separator between table and index, like orders_1; but recommand use orders1

		private int dsIndex0 = -1;
		private int dsIndex1;
		private List<String> dsList;// 保存列举的

		private int tabIndex0 = -1;
		private int tabIndex1;
		private List<String> tabList;// 保存列举的

		public String toString() {
			String str = "dsBaseName:" + dsBaseName;
			str += ", tabBaseName:" + tabBaseName;
			if (dsIndex0 != -1) {
				str += ", dsIndex0:" + dsIndex0;
				str += ", dsIndex1:" + dsIndex1;
			} else {
				str += ", dsList:" + dsList.toString();
			}
			if (tabIndex0 != -1) {
				str += ", tabIndex0:" + tabIndex0;
				str += ", tabIndex1:" + tabIndex1;
			} else {
				str += ", tabList:" + tabList.toString();
			}
			return str;
		}

		public String getDsBaseName() {
			return dsBaseName;
		}

		public void setDsBaseName(String dsBaseName) {
			this.dsBaseName = dsBaseName;
		}

		public String getTabBaseName() {
			return tabBaseName;
		}

		public void setTabBaseName(String tabBaseName) {
			this.tabBaseName = tabBaseName;
		}

		/**
		 * separator between table and index, like orders_1;  
		 * but recommand donot use separator,like: orders1
		 * @return
		 */
		public String getSepTab() {
			return sepTab;
		}

		/**
		 * separator between table and index, like orders_1;  
		 * but recommand donot use separator,like: orders1
		 * @param sepTab separator between table and index
		 */
		public void setSepTab(String sepTab) {
			this.sepTab = sepTab;
		}

		public int getDsIndex0() {
			return dsIndex0;
		}

		public void setDsIndex0(int dsIndex0) {
			this.dsIndex0 = dsIndex0;
		}

		public int getDsIndex1() {
			return dsIndex1;
		}

		public void setDsIndex1(int dsIndex1) {
			this.dsIndex1 = dsIndex1;
		}

		public int getTabIndex0() {
			return tabIndex0;
		}

		public void setTabIndex0(int tabIndex0) {
			this.tabIndex0 = tabIndex0;
		}

		public int getTabIndex1() {
			return tabIndex1;
		}

		public void setTabIndex1(int tabIndex1) {
			this.tabIndex1 = tabIndex1;
		}

		public List<String> getDsList() {
			return dsList;
		}

		public void setDsList(List<String> dsList) {
			this.dsList = dsList;
		}

		public List<String> getTabList() {
			return tabList;
		}

		public void setTabList(List<String> tabList) {
			this.tabList = tabList;
		}
	}

}
