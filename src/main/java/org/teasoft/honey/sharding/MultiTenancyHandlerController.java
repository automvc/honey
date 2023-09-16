/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.DsTabHandler;
import org.teasoft.bee.osql.annotation.MultiTenancy;
import org.teasoft.bee.sharding.DsTabStruct;
import org.teasoft.bee.sharding.ShardingSimpleStruct;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.util.StringUtils;

/**
 * 通过注解收集ShardingSimpleStruct的数据,然后交给处理类处理.
 * @author Kingstar
 * @since  1.11-E
 */
public class MultiTenancyHandlerController {

	private MultiTenancyHandlerController(){
		
	}
	
	@SuppressWarnings("rawtypes")
	public static DsTabStruct process(Field field, Object entity, SuidType sqlSuidType) {
		DsTabStruct dsTabStruct = null;
		try {
			MultiTenancy anno = field.getAnnotation(MultiTenancy.class);
			String appointDS = anno.appointDS();
			String appointTab = anno.appointTab();

			dsTabStruct = new DsTabStruct();
			boolean isAppoint = false;
			if (StringUtils.isNotBlank(appointDS)) {
				dsTabStruct.setDsName(appointDS);
				isAppoint = true;
			}

			if (StringUtils.isNotBlank(appointTab)) {
				dsTabStruct.setTabName(appointTab);
				isAppoint = true;
			}
			
			//只指明了一个,另一个还要计算吗???　bug??
			if (isAppoint) return dsTabStruct;

//			field.setAccessible(true);
			HoneyUtil.setAccessibleTrue(field);
			Object tenancyValue = field.get(entity); //可能是null,如何处理???
			
			// 分库分表时,分片字段的值是null是,要查所有库表,怎么知道所有的库和表?
			//交给具体处理器处理. 2022-05-02
//			if (tenancyValue == null) {  
//				Logger.error("多租户字段值是null.");
//				return null;
//			}

			ShardingSimpleStruct sharding = new ShardingSimpleStruct();
			sharding.setDsAlgorithm(anno.dsAlgorithm());
			sharding.setDsRule(anno.dsRule());
			sharding.setDsName(anno.dsName());
			sharding.setTabAlgorithm(anno.tabAlgorithm());
			sharding.setTabRule(anno.tabRule());
			sharding.setTabName(anno.tabName());
//			sharding.setShardingValue(tenancyValue);
			sharding.setDsShardingValue(tenancyValue);
			sharding.setTabShardingValue(tenancyValue);

			Class c = anno.handler();

			if (c.equals(DsTabHandler.class)) {//只是默认的接口
				//使用系统定义的
				dsTabStruct = new DefaultAnnoDsTabHandler().process(sharding);
//			} else if (c.isAssignableFrom(DsTabHandler.class)) { //是AnnotationHandler的实现类 
		 	
//			}  else if(Modifier.isAbstract(c.getModifiers())){//如何判断是抽象类??
			} else if (! c.isInterface()  && !Modifier.isAbstract(c.getModifiers())) {//不是接口和不是抽象类
				DsTabHandler obj = (DsTabHandler) c.newInstance();
				dsTabStruct = obj.process(sharding);

			}

		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

		return dsTabStruct;
	}

}
