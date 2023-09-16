/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.osql.annotation.ColumnHandler;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.util.ObjectUtils;

/**
 * @author Kingstar
 * @since  1.11
 */
public class DefaultColumnHandler implements ColumnHandler {
	
	private final String field2Column=StringConst.PREFIX+"Field2Column";

	@Override
	public String toColumnName(String fieldName, Class entityClass) {
		if (entityClass != null) {
			String entityFullName=entityClass.getName();
			Boolean flag = HoneyContext.getCustomFlagMap(field2Column + entityFullName);
			if (flag == null) {//还没检测的
				initDefineColumn(entityClass); 
				flag = HoneyContext.getCustomFlagMap(field2Column + entityFullName);
			}

			if (ObjectUtils.isTrue(flag)) {
				String defineColumn = HoneyContext.getCustomMapValue(field2Column + entityFullName,
						fieldName);
				if (defineColumn != null) return defineColumn;
			} //返回标志是false,或没找到,则表示没有另外定义列名,使用原来的转换规则
		}

		return null;
	}

	@Override
	public String toFieldName(String columnName, Class entityClass) {
		if (entityClass != null) {
			String entityFullName=entityClass.getName();
			Boolean flag = HoneyContext.getCustomFlagMap(field2Column + entityFullName);
			if (ObjectUtils.isTrue(flag)) {
				String fieldName = HoneyContext
						.getCustomMapValue(StringConst.Column2Field + entityFullName, columnName);
				if (fieldName != null) return fieldName;
			} //确认查询之前是否都已经检测过一遍   返回标志是false,或没找到,则表示没有另外定义列名,使用原来的转换规则
		}
		return null;
	}

	private void initDefineColumn(Class entityClass) {
		try {
//			Object entity = Class.forName(entityFullName).newInstance();
			//看下直接传entity是否方便????
//			Field fields[] = entity.getClass().getDeclaredFields();
			
			Field fields[] = entityClass.getDeclaredFields();
			String entityFullName=entityClass.getName();
			String defineColumn = "";
			String fiName = "";
			int len = fields.length;
			Map<String, String> kv = new HashMap<>();
			Map<String, String> column2Field = new HashMap<>();
			boolean has = false;
			for (int i = 0; i < len; i++) {
				if (HoneyUtil.isSkipField(fields[i])) continue;
//				fields[i].setAccessible(true);
				HoneyUtil.setAccessibleTrue(fields[i]);
				if (AnnoUtil.isColumn(fields[i])) {
//					Column column = fields[i].getAnnotation(Column.class);
//					defineColumn = column.value();
					defineColumn=AnnoUtil.getValue(fields[i]);
					if (NameCheckUtil.isIllegal(defineColumn)) {
						throw new BeeIllegalParameterException(
								"Annotation Column set wrong value:" + defineColumn);
					}

					fiName = fields[i].getName();
					kv.put(fiName, defineColumn);
					column2Field.put(defineColumn, fiName); //单表查询拼结果会用到
//					if (findName.equals(fiName)) findDefineColumn = defineColumn;
					has = true;
				}
			} //end for

			if (has) {
				HoneyContext.addCustomMap(field2Column + entityFullName, kv);
				HoneyContext.addCustomMap(StringConst.Column2Field + entityFullName, column2Field); //SqlLib, select会用到. 
				HoneyContext.addCustomFlagMap(field2Column + entityFullName, Boolean.TRUE);
			} else {
				HoneyContext.addCustomFlagMap(field2Column + entityFullName, Boolean.FALSE);
			}
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
			//ignore
		}
	}

}
