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
import org.teasoft.bee.osql.annotation.customizable.ColumnHandler;
import org.teasoft.bee.osql.exception.BeeErrorNameException;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * 命名转换.Name Translate.
 * @author Kingstar
 * @since  1.5
 */
public class NameTranslateHandle {

	private static NameTranslate nameTranslate = BeeFactory.getHoneyFactory().getInitNameTranslate();
	private static ConcurrentMap<String, String> entity2tableMap;
	private static ConcurrentMap<String, String> table2entityMap = null;
	
	private static ColumnHandler columnHandler; //V1.11
	
	private static String schemaName; //V1.11
	
	static {
		entity2tableMap = HoneyContext.getEntity2tableMap();
//		table2entityMap=HoneyContext.getTable2entityMap();
		
		String sName=HoneyConfig.getHoneyConfig().getSchemaName();
		if(StringUtils.isNotBlank(sName)) NameTranslateHandle.schemaName=sName;
	}

	private NameTranslateHandle() {}
	
	/**
	 * 指定命名转换实现类
	 * @param nameTranslate
	 */
	public static void setNameTranslate(NameTranslate nameTranslate) { // for set customer naming.
		HoneyContext.clearFieldNameCache();
		NameTranslateHandle.nameTranslate = nameTranslate;
	}

	public static NameTranslate getNameTranslate() {
		NameTranslate nameTranslate1=HoneyContext.getCurrentNameTranslate();
		if(nameTranslate1!=null) {
//			HoneyContext.clearFieldNameCache(); //V1.17
			return nameTranslate1; //当前对象设置有,则优先使用.
		}
		return NameTranslateHandle.nameTranslate;
	}
	
	public static ColumnHandler getColumnHandler() {
		return columnHandler;
	}

	/**
	 * 指定列名命名转换处理器.Specifies the column naming conversion handler
	 * @param columnHandler 列名命名转换处理器.column naming conversion handler
	 */
	public static void setColumnHandler(ColumnHandler columnHandler) {
		NameTranslateHandle.columnHandler = columnHandler;
	}
	
	/**
	 * get schema name
	 * In some database has other name, eg:Cassandra is Keyspace
	 * @return schema name
	 */
	public static String getSchemaName() {
		return schemaName;
	}

	/**
	 * set schema name
	 * In some database has other name, eg:Cassandra is Keyspace
	 * @param schemaName  schema name
	 */
	public static void setSchemaName(String schemaName) {
		
		checkSchemaName(schemaName);
		NameTranslateHandle.schemaName = schemaName;
	}
	
	private static void checkSchemaName(String schemaName) {
		if(NameCheckUtil.isIllegal(schemaName)) {
			throw new BeeErrorNameException("The schemaName: '" + schemaName + "' is illegal!");
		}
	}

	public static String getSchemaNameLocal() {
		return HoneyContext.getSysCommStrLocal(StringConst.SchemaName);
	}

	public static void setSchemaNameLocal(String schemaNameLocal) {
		checkSchemaName(schemaNameLocal);
		HoneyContext.setSysCommStrLocal(StringConst.SchemaName, schemaNameLocal);
	}
	
	
	public static String toTableName(String entityName) {
		
		//sync from V2.1
		if ("java.lang.String".equals(entityName) || "java.lang.Class".equals(entityName)
				 || "java.lang.Object".equals(entityName)) { // 2.1 fixed bug
					throw new BeeIllegalParameterException(entityName + " is a wrong entity name.");
				}
		
		String tableName = _toTableName(entityName);
		if (tableName.indexOf('.') == -1) {
			if (StringUtils.isNotBlank(getSchemaNameLocal()))
				tableName = getSchemaNameLocal() + "." + tableName; //V1.11
			else if (StringUtils.isNotBlank(getSchemaName())) 
				tableName = getSchemaName() + "." + tableName; //V1.11
		}
		return tableName;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String _toTableName(String entityName) {
		try {
			//V1.11 via ThreadLocal
			String appointTab = HoneyContext.getAppointTab();
			if (StringUtils.isNotBlank(appointTab)) return appointTab;
			String tabSuffix = HoneyContext.getTabSuffix();
			if (StringUtils.isNotBlank(tabSuffix)) {
				int index = entityName.lastIndexOf('.');
				if (index > 0) {
					entityName = entityName.substring(index + 1);
					return getNameTranslate().toTableName(entityName) + tabSuffix;
				}
			}

			if (OneTimeParameter.isTrue(StringConst.DoNotCheckAnnotation)) {
				//nothing
			} else {
				//Table注解不再需要命名转换,Entity注解解析动态命名参数后还需要命名转换
				Class obj = Class.forName(entityName);
//				if (obj.isAnnotationPresent(Table.class)) {
//					Table tab = (Table) obj.getAnnotation(Table.class);
//					String tabName = processAutoPara(tab.value());
				if (AnnoUtil.isTable(obj)) {	
					String tabName = processAutoPara(AnnoUtil.getValue(obj));
					if (NameCheckUtil.isIllegal(tabName)) {
						throw new BeeIllegalParameterException(
								"Annotation Table set wrong value:" + tabName);
					}
					return tabName;
				} else if (obj.isAnnotationPresent(Entity.class)) {
					Entity entity = (Entity) obj.getAnnotation(Entity.class);
					entityName = processAutoPara(entity.value());
					if (NameCheckUtil.isIllegal(entityName)) {
						throw new BeeIllegalParameterException(
								"Annotation Entity set wrong value:" + entityName);
					}
				}
			}
		} catch (ClassNotFoundException e) {
			if (entityName != null && !entityName.contains("."))
				Logger.info("In NameTranslateHandle,ClassNotFoundException : " + e.getMessage());
			else
				Logger.warn("In NameTranslateHandle,ClassNotFoundException : " + e.getMessage());
		}

		//entityName maybe include package name
		//special one, config in :bee.osql.name.mapping.entity2table
		if (entityName == null) entityName = "";
		String tableName = entity2tableMap.get(entityName);
		if (tableName != null && !"".equals(tableName.trim())) {
			return tableName;//fix bug 2020-08-22
		} else {//若找不到,检测是否包含包名,若有,则去除包名后再用类名看下是否能找到
			int index = entityName.lastIndexOf('.');
			if (index > 0) {
				entityName = entityName.substring(index + 1); //此时entityName只包含类名
				tableName = entity2tableMap.get(entityName);
				if (tableName != null && !"".equals(tableName.trim())) return tableName;//fix bug 2020-08-22
			}
		}
		
		return getNameTranslate().toTableName(entityName); //到此的entityName只包含类名
	}

	public static String toColumnName(String fieldName) {
		return getNameTranslate().toColumnName(fieldName);
	}
	
	public static String toColumnName(String fieldName, Class entityClass) {
		boolean openDefineColumn=HoneyConfig.getHoneyConfig().openDefineColumn;
		if (openDefineColumn && entityClass != null) {
			if (getColumnHandler() != null) {
				String defineColumn = getColumnHandler().toColumnName(fieldName, entityClass);
				if(defineColumn!=null) return defineColumn;
			}
		}
		   
		return getNameTranslate().toColumnName(fieldName);
	}
	
	public synchronized static String toEntityName(String tableName) {//生成javabean时会用到. SqlLib不会用到.因会传入T entity
		if (table2entityMap == null) {
			table2entityMap = HoneyContext.getTable2entityMap();
		}
		if (tableName == null) tableName = "";
		//special one, config in :bee.osql.name.mapping.entity2table
		String entityName = table2entityMap.get(tableName);
		if (entityName != null && !"".equals(entityName.trim())) return entityName; //fix bug 2020-08-22

		return getNameTranslate().toEntityName(tableName);
	}

	public static String toFieldName(String columnName) {
		return getNameTranslate().toFieldName(columnName);
	}
	
	public static String toFieldName(String columnName, Class entityClass) {
		boolean openDefineColumn=HoneyConfig.getHoneyConfig().openDefineColumn;
		if (openDefineColumn && entityClass != null) {
			if (getColumnHandler() != null) {
				String fieldName = getColumnHandler().toFieldName(columnName, entityClass);
				if(fieldName!=null) return fieldName;
			}
		}
		return getNameTranslate().toFieldName(columnName);
	}
	
	private static String processAutoPara(String autoPara) {
		int start = autoPara.indexOf("${");
		int end = autoPara.indexOf('}');
		if (start > 0 && end > 0 && start + 2 < end) {
			String key = autoPara.substring(start + 2, end);
			Map<String, String> map = new HashMap<>();
			String value = (String) OneTimeParameter.getAttribute(key);
			if (value == null) {
				Logger.warn("Auto table: parameter  ${" + key + "} in " + autoPara
						+ " still has not value, will be ignore it!");
//				return autoPara;
				value = ""; //V1.9 没设置时,直接去掉变量表达式
			}
			map.put(key, value);
			return TokenUtil.processWithMap(autoPara, "${", "}", map);
		} else {
			return autoPara;
		}
	}
}
