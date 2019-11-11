/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.concurrent.ConcurrentMap;

import org.teasoft.bee.osql.NameTranslate;

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

	public static String toTableName(String entityName) {
		//entityName maybe include package name
		//special one, config in :bee.osql.name.mapping.entity2table
		String tableName=entity2tableMap.get(entityName);
		if(tableName!=null && "".equals(tableName.trim())) return tableName;
		else {//若找不到,检测是否包含包名,若有,则再用类名看下是否能找到
			int index = entityName.lastIndexOf(".");
			if(index>0){
				entityName=entityName.substring(index + 1);  //此时entityName只包含类名
				tableName=entity2tableMap.get(entityName);
				if(tableName!=null && "".equals(tableName.trim())) return tableName;
			}
		}
		
		return nameTranslat.toTableName(entityName); //到此的entityName只包含类名
	}

	public static String toColumnName(String fieldName) {
		return nameTranslat.toColumnName(fieldName);
	}

	public static String toEntityName(String tableName) {
		if(table2entityMap==null){
			table2entityMap=HoneyContext.getTable2entityMap();
		}
		//special one, config in :bee.osql.name.mapping.entity2table
		String entityName=table2entityMap.get(tableName);
		if(entityName!=null && "".equals(entityName.trim())) return entityName;
		
		return nameTranslat.toEntityName(tableName);
	}

	public static String toFieldName(String columnName) {
		return nameTranslat.toFieldName(columnName);
	}
}
