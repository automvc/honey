/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.annotation.Entity;
import org.teasoft.bee.osql.annotation.Table;

/**
 * @author Kingstar
 * @since  1.5
 */
public class NameTranslateHandle {
	private static NameTranslate nameTranslat = BeeFactory.getHoneyFactory().getNameTranslate();
	private static ConcurrentMap<String,String> entity2tableMap;
	private static ConcurrentMap<String,String> table2entityMap=null;
	static{
		entity2tableMap=HoneyContext.getEntity2tableMap();
//		table2entityMap=HoneyContext.getTable2entityMap();
	}
	
	/**
	 * 指定命名转换实现类
	 * @param nameTranslat
	 */
	public static void setNameTranslat(NameTranslate nameTranslat) { //TODO remove??
		NameTranslateHandle.nameTranslat = nameTranslat;
	}

	@SuppressWarnings({"rawtypes","unchecked"}) 
	public static String toTableName(String entityName) {
		try {
			String flag = (String) OneTimeParameter.getAttribute("_SYS_Bee_DoNotCheckAnnotation");
			if ("tRue".equals(flag)) {
				//nothing
			} else {
				//Table注解不再需要命名转换,Entity注解解析动态命名参数后还需要命名转换
				Class obj = Class.forName(entityName);
				if (obj.isAnnotationPresent(Table.class)) {
					Table tab = (Table) obj.getAnnotation(Table.class);
					return processAutoPara(tab.value());
				} else if (obj.isAnnotationPresent(Entity.class)) {
					Entity tntity = (Entity) obj.getAnnotation(Entity.class);
					entityName = processAutoPara(tntity.value());
				}
			}
		} catch (Exception e) {
//			e.printStackTrace();
			Logger.error(e.getMessage());
		}
		
		//entityName maybe include package name
		//special one, config in :bee.osql.name.mapping.entity2table
		String tableName=entity2tableMap.get(entityName);
		if(tableName!=null && !"".equals(tableName.trim())) return tableName;//fix bug 2020-08-22
		else {//若找不到,检测是否包含包名,若有,则去除包名后再用类名看下是否能找到
			int index = entityName.lastIndexOf(".");
			if(index>0){
				entityName=entityName.substring(index + 1);  //此时entityName只包含类名
				tableName=entity2tableMap.get(entityName);
				if(tableName!=null && !"".equals(tableName.trim())) return tableName;//fix bug 2020-08-22
			}
		}
		
		return nameTranslat.toTableName(entityName); //到此的entityName只包含类名
	}

	public static String toColumnName(String fieldName) {
		return nameTranslat.toColumnName(fieldName);
	}

	public static String toEntityName(String tableName) {//生成javabean时会用到. SqlLib不会用到.因会传入T entity
		if(table2entityMap==null){
			table2entityMap=HoneyContext.getTable2entityMap();
		}
		//special one, config in :bee.osql.name.mapping.entity2table
		String entityName=table2entityMap.get(tableName);
		if(entityName!=null && !"".equals(entityName.trim())) return entityName; //fix bug 2020-08-22
		
		return nameTranslat.toEntityName(tableName);
	}

	public static String toFieldName(String columnName) {
		return nameTranslat.toFieldName(columnName);
	}
	
	private static String processAutoPara(String autoPara) {
		int start = autoPara.indexOf("${");
		int end = autoPara.indexOf("}");
		if (start > 0 && end > 0 && start + 2 < end) {
			String key = autoPara.substring(start + 2, end);
			Map<String,String> map=new HashMap<>();
			String value=(String)OneTimeParameter.getAttribute(key);
			if(value==null){
				Logger.error("Auto table error: parameter  ${"+key+"} in "+autoPara+" still has not value!");
				return autoPara;
			}
			map.put(key, value);
			return TokenUtil.processWithMap(autoPara, "${", "}", map);
		} else {
			return autoPara;
		}
	}
}
