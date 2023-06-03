/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.annotation.DsTabHandler;
import org.teasoft.bee.sharding.DsTabStruct;
import org.teasoft.bee.sharding.ShardingSimpleStruct;
import org.teasoft.bee.sharding.algorithm.Calculate;
import org.teasoft.honey.sharding.algorithm.CalculateFactory;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * 多租户和分片的默认处理器.
 * 用于处理MultiTenancy注解和Sharding注解
 * MultiTenancy注解,当分片值为null时不处理,调用者可使用默认数据源和表.
 * Sharding注解,当分片值为null时可能产生全域操作.
 * @author Kingstar
 * @since  1.11-E
 */
public class DefaultAnnoDsTabHandler implements DsTabHandler {

	private static final String dsRuleConst = "${dsRule}";
	private static final String tabRuleConst = "${tabRule}";

	public DsTabStruct process(ShardingSimpleStruct sharding) {
		
		String shardingValue = ObjectUtils.string(sharding.getDsShardingValue());
		DsTabStruct dsTabStruct = null;
		if (shardingValue == null) { //分片值为null,直接返回,  最终可以使用默认值.  默认DS??  顺序,哪个先??
			                         //08-29  MultiTenancy可以用默认值;但Sharding会走全域查所有
			                          //String类型的分片值,是否要将值改为0????
			
			return dsTabStruct;
	    }

		dsTabStruct = new DsTabStruct();
		int dsAlgorithm = sharding.getDsAlgorithm();
		String dsRule = sharding.getDsRule();
		String dsName = sharding.getDsName();
		int tabAlgorithm = sharding.getTabAlgorithm();
		String tabRule = sharding.getTabRule();
		String tabName = sharding.getTabName();
//		String shardingValue = (String) sharding.shardingValue;
		
		
//		shardingValue分片值为null时,如何处理?    报异常或使用默认DS?   通过配置
//		多租户,分片字段的值是null是,使用默认DS???
//	       多租户也是多数据源的一种?? 不一定，有可能是单表的.
//		单表时，返回null,则按之前的规则转换tableName
//		多库的多租户时，原来也是要设置默认DS的.
		
//		多租户是不允许租户的字段为null的???  但也可以这样理解，为null的，则使用共用表。 这样对只是大客户或特定客户另外给出表保存数据的场景也有利。
//		 若是使用字符串做为租户的判断条件，可能要使用Map

		boolean adjustSharding=false;

		//---------ds--------------start
		if (StringUtils.isNotBlank(dsRule) && StringUtils.isNotBlank(dsName)) {
			//			String suffix = LongCalculator.calculate(dsRule, (String) tenancyValue);
			//通过算法类别dsAlgorithm,获取具体的算法.
			Calculate calculate1=CalculateFactory.getCalculate(dsAlgorithm);
			if(calculate1==null) {
				throw new BeeException("Can not find the Calculate with dsAlgorithm: "+dsAlgorithm);
			}
			
			if(dsAlgorithm==0 && ! isNumber(shardingValue)) {
//				shardingValue=shardingValue.hashCode()+"";
				shardingValue=ShardingUtil.hashInt(shardingValue)+"";
				adjustSharding=true;
	     	}
			
			String suffix = calculate1.process(dsRule, shardingValue);
			if (dsName.contains(dsRuleConst)) { // test
				dsName=dsName.replace(dsRuleConst, suffix);
			} else {
				dsName = dsName + suffix;
			}
			dsTabStruct.setDsName(dsName);
		} else if (StringUtils.isNotBlank(dsName)) {
			dsTabStruct.setDsName(dsName);
		} 
//		else if (StringUtils.isNotBlank(dsRule)) {
//			// 只设置dsRule,不设置dsName    是否允许????  
//			//不允许不设置dsName,只设置dsRule
//
//		}
		//---------ds--------------end

		//---------Tab--------------start
		String tabSuffix = "";
		boolean hasTabRule = false;
		if (StringUtils.isNotBlank(tabRule)) {
//			tabSuffix = LongCalculator.calculate(tabRule, shardingValue);
			
			Calculate calculate2=CalculateFactory.getCalculate(tabAlgorithm);
			if(calculate2==null) {
				throw new BeeException("Can not find the Calculate with dsAlgorithm: "+tabAlgorithm);
			}
			
			if(tabAlgorithm==0 && ! isNumber(shardingValue) && !adjustSharding) {
				shardingValue=ShardingUtil.hashInt(shardingValue)+"";
	     	}
			
			tabSuffix =calculate2.process(tabRule, shardingValue);
			
			hasTabRule = true;
		}
//		tabName,tabSuffix不会同时设置
		if (hasTabRule && StringUtils.isNotBlank(tabName)) {
			if (tabName.contains(tabRuleConst)) { //配置时,自己加"-"
				tabName=tabName.replace(tabRuleConst, tabSuffix);
			} else {
				
				String sepTab = ShardingRegistry.getSepTab(tabName); //2.1.5.20 
				if (StringUtils.isNotEmpty(sepTab)) tabSuffix = sepTab + tabSuffix; // 分隔符在DsTabHandler实现类加
				
				tabName = tabName + tabSuffix;
//				tabName = tabName +"_"+ tabSuffix; //加分隔
			}
			dsTabStruct.setTabName(tabName);
		} else if (StringUtils.isNotBlank(tabName)) {
			dsTabStruct.setTabName(tabName);
		} else if (hasTabRule) {
			// 只设置tabRule,不设置tabName    是否允许????   
			//允许, 表示tabName使用实体名转换得来
//			dsTabStruct.setTabSuffix(tabSuffix);
		}
		dsTabStruct.setTabSuffix(tabSuffix); //都设置
		//---------Tab--------------end

		return dsTabStruct;
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
