/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.Sharding;
import org.teasoft.bee.osql.annotation.customizable.DsTabHandler;
import org.teasoft.bee.sharding.DsTabStruct;
import org.teasoft.bee.sharding.ShardingSimpleStruct;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.util.StringUtils;

/**
 * 与MultiTenancyHandlerController逻辑很近
 * 用于处理Sharding注解
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingAnnoHandlerController {

	private ShardingAnnoHandlerController(){
		
	}
	
	@SuppressWarnings("rawtypes")
	public static DsTabStruct process(Field field, Object entity, SuidType sqlSuidType,ShardingSimpleStruct sharding0) {
		DsTabStruct dsTabStruct = null;
		try {
			Sharding anno = field.getAnnotation(Sharding.class); //只这行不一样.
			String appointDS = anno.appointDS();
			String appointTab = anno.appointTab();

			dsTabStruct = new DsTabStruct();
			boolean isAppointDs = false;
			boolean isAppointTab = false;
			if (StringUtils.isNotBlank(appointDS)) {
				dsTabStruct.setDsName(appointDS);
				isAppointDs = true;
			}

			if (StringUtils.isNotBlank(appointTab)) {
				dsTabStruct.setTabName(appointTab);
				isAppointTab = true;
			}
			
//			appointDS,appointTab
			//只指明了一个,另一个还要计算吗???　bug??   2022-08-29 不算.
//			只指定一个就返回?  Sharding注解,只指定tabName,则Ds可以通过反查; 但只指定ds,tab则确定不了.
			//MultiTenancy 只指定tabName,则Ds使用默认;只指定ds,则tabName使用自动转换(可用在分库不分表的情况)
//			if (isAppoint) return dsTabStruct;
			if(isAppointTab) return dsTabStruct;  //指定了tab就返回了.
//			只指定了ds,则运算时,不再计算
//			if(!isAppointDs) sharding.setDsName(anno.dsName()); //指定了,就不设置,下一层就不会再计算

			field.setAccessible(true);
			Object shardingValue = field.get(entity); //可能是null,如何处理???
			
			//TODO 分库分表时,分片字段的值是null是,要查所有库表,怎么知道所有的库和表?
			//交给具体处理器处理. 2022-05-02
//			if (tenancyValue == null) {  
//				Logger.error("多租户字段值是null.");
//				return null;
//			}

			if (sharding0 == null) sharding0 = new ShardingSimpleStruct();
			 //记录sharding返回给condtion解析用
			sharding0.setDsAlgorithm(anno.dsAlgorithm());
			sharding0.setDsRule(anno.dsRule());
			if(!isAppointDs) sharding0.setDsName(anno.dsName()); //指定了,就不设置,下一层就不会再计算
			sharding0.setTabAlgorithm(anno.tabAlgorithm());
			sharding0.setTabRule(anno.tabRule());
			if(StringUtils.isNotBlank(anno.tabName())) sharding0.setTabName(anno.tabName()); //2.0
//			sharding.setShardingValue(tenancyValue);
			sharding0.setDsShardingValue(shardingValue);
			sharding0.setTabShardingValue(shardingValue);

			Class c = anno.handler();

			if (c.equals(DsTabHandler.class)) {//只是默认的接口
				//使用系统定义的
				dsTabStruct = new DsTabDefaultHandler().process(sharding0);
//			} else if (c.isAssignableFrom(DsTabHandler.class)) { //是AnnotationHandler的实现类 
		 	
//			}  else if(Modifier.isAbstract(c.getModifiers())){//如何判断是抽象类??
			} else if (! c.isInterface()  && !Modifier.isAbstract(c.getModifiers())) {//不是接口和不是抽象类
				DsTabHandler obj = (DsTabHandler) c.newInstance();
				dsTabStruct = obj.process(sharding0);

			}
			
			if (isAppointDs) {
				if (dsTabStruct == null) dsTabStruct = new DsTabStruct();
				dsTabStruct.setDsName(appointDS); // 设置指定的DS
			}

		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

		return dsTabStruct;
	}

}
