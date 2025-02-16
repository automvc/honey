/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import java.lang.reflect.Modifier;

import org.teasoft.bee.osql.annotation.DsTabHandler;
import org.teasoft.bee.sharding.DsTabStruct;
import org.teasoft.bee.sharding.ShardingBean;
import org.teasoft.bee.sharding.ShardingSimpleStruct;
import org.teasoft.bee.sharding.algorithm.Calculate;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.sharding.algorithm.CalculateFactory;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * 分片总的处理类. Sharding注解,不在此处处理.
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingDsTabHandler implements DsTabHandler {

	private static final String dsRuleConst = "${dsRule}";
	private static final String tabRuleConst = "${tabRule}";

	public DsTabStruct process(ShardingSimpleStruct sharding) {

//		shardingValue  之前是DS,Tab两种分片都是同一字段, 所以使用一个就够了.现在可以支持不同,需要两种

		ShardingBean bean = (ShardingBean) sharding;

		String dsShardingValue = ObjectUtils.string(bean.getDsShardingValue());
		String tabShardingValue = ObjectUtils.string(bean.getTabShardingValue());

		if (dsShardingValue == null && tabShardingValue == null) { // 要用全域查询. 像select * from tableName;
			return null; // 不是一库一表
		}
		DsTabStruct dsTabStruct = new DsTabStruct();
		int dsAlgorithm = bean.getDsAlgorithm();
		String dsRule = bean.getDsRule();
		String dsName = bean.getDsName();
		int tabAlgorithm = bean.getTabAlgorithm();
		String tabRule = bean.getTabRule();
		String tabName = bean.getTabName();

		if (StringUtils.isBlank(tabRule)) { // tabRule可以不写, 默认是: "tabField%tableListSize"
			tabRule = "tabField%" + ShardingRegistry.getTabSize(tabName);
		}

		// ---------ds--------------start
//		顺序: DsAlgorithmClass>dsAlgorithm
		if (StringUtils.isNotBlank(dsRule) && StringUtils.isNotBlank(dsName) && dsShardingValue != null) {
			Class c1 = bean.getDsAlgorithmClass();
			Calculate calculate1 = createObj(c1);
			// 通过算法类别dsAlgorithm,获取具体的算法.
			if (calculate1 == null) {
				calculate1 = CalculateFactory.getCalculate(dsAlgorithm);
				// dsAlgorithm是基本类型,默认会是0;
				// 要是想用getDsAlgorithmClass设置就无法实现.
				// 改为默认值为-1,看是否会对原来有影响?? 不需要;
				// 旧的是DsAlgorithmClass>dsAlgorithm
				if (dsAlgorithm == 0 && !isNumber(dsShardingValue)) {
					dsShardingValue = ShardingUtil.hashInt(dsShardingValue) + "";
				}
			}

			String suffix = calculate1.process(dsRule, dsShardingValue);
			if (dsName.contains(dsRuleConst)) { // test
				dsName = dsName.replace(dsRuleConst, suffix);
			} else {
				dsName = dsName + suffix;
			}
			dsTabStruct.setDsName(dsName);
		} else if (StringUtils.isNotBlank(dsName) && dsShardingValue != null) {
			dsTabStruct.setDsName(dsName);
		}
		// ---------ds--------------end

		// ---------Tab--------------start
		String tabSuffix = "";
		boolean hasTabRule = false;
		if (StringUtils.isNotBlank(tabRule) && tabShardingValue != null) {
			Class c2 = bean.getTabAlgorithmClass();
			Calculate calculate2 = createObj(c2);
			if (calculate2 == null) {
				calculate2 = CalculateFactory.getCalculate(tabAlgorithm);
				if (tabAlgorithm == 0 && !isNumber(tabShardingValue)) {
					tabShardingValue = ShardingUtil.hashInt(tabShardingValue) + "";
				}
			}

			tabSuffix = calculate2.process(tabRule, tabShardingValue);
//			tabSuffix="_"+tabSuffix;  //分隔符在DsTabHandler实现类加
			hasTabRule = true;
		}
		if (hasTabRule && StringUtils.isNotBlank(tabName) && tabShardingValue != null) {
			if (tabName.contains(tabRuleConst)) {
				tabName = tabName.replace(tabRuleConst, tabSuffix);
			} else {

				String sepTab = ShardingRegistry.getSepTab(tabName); // 2.1.5.20
				if (StringUtils.isNotEmpty(sepTab)) tabSuffix = sepTab + tabSuffix; // 分隔符在DsTabHandler实现类加

				tabName = tabName + tabSuffix;
//				tabName = tabName +"_"+ tabSuffix; //加分隔
			}
			dsTabStruct.setTabName(tabName);
		} else if (StringUtils.isNotBlank(tabName) && tabShardingValue != null) {
			dsTabStruct.setTabName(tabName);
		} else if (hasTabRule) {
			// 只设置tabRule,不设置tabName 是否允许????
			// 允许, 表示tabName使用实体名转换得来
//			dsTabStruct.setTabSuffix(tabSuffix);
		}
		dsTabStruct.setTabSuffix(tabSuffix); // 下标都要用到. 全域查询时,则不需要在这计算. 2022-09-20
		// ---------Tab--------------end

		return dsTabStruct;
	}

	private Calculate createObj(Class c) {
		Calculate calculate = null;
//		Class c1 = bean.getDsAlgorithmClass();
		if (!c.equals(Calculate.class) && !c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {// 不是默认接口,不是接口和不是抽象类
			try {
				calculate = (Calculate) c.newInstance();
			} catch (Exception e) {
				Logger.debug(e.getMessage(), e);
			}
		}
		return calculate;
	}

	private boolean isNumber(String value) {
//		if(StringUtils.isBlank(value)) return false; //上游已判断
		try {
			if (value.contains(".")) {
				Double.parseDouble(value);
			} else {
				Long.parseLong(value);
			}
			return true;
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

}
