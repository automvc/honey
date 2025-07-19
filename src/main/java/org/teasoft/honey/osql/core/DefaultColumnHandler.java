/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.annotation.ColumnHandler;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.util.ObjectUtils;

/**
 * @author Kingstar
 * @since  1.11
 */
public class DefaultColumnHandler implements ColumnHandler {

	private final String field2Column = StringConst.PREFIX + "Field2Column";

	@Override
	public String toColumnName(String fieldName, Class entityClass) {
		String s1 = toColumnName0(fieldName, entityClass);

		if (s1 == null && isOpenEntityCanExtend()) {
			Class<?> superClass = null;
			Class<?> currentClass = entityClass;
			do {
				superClass = currentClass.getSuperclass(); // 拿的是父类，而不是字段的值
				if (HoneyUtil.isSuperEntity(superClass)) {
					s1 = toColumnName0(fieldName, superClass);
					currentClass = superClass;
				} else {
					break;
				}
			} while (s1 == null);
		}

		return s1;
	}

	private String toColumnName0(String fieldName, Class entityClass) {
		if (HoneyUtil.isJavaPackage(entityClass)) {
			Logger.debug("The parameter entityClass is from Java library");
			return null;
		}
		if (entityClass != null) {
			String entityFullName = entityClass.getName();
			Boolean flag = HoneyContext.getCustomFlagMap(field2Column + entityFullName);
			if (flag == null) {// 还没检测的
				initDefineColumn(entityClass);
				flag = HoneyContext.getCustomFlagMap(field2Column + entityFullName);
			}

			if (ObjectUtils.isTrue(flag)) {
				String defineColumn = HoneyContext.getCustomMapValue(field2Column + entityFullName, fieldName);
				if (defineColumn != null) return defineColumn;
			} // 返回标志是false,或没找到,则表示没有另外定义列名,使用原来的转换规则
		}

		return null;
	}

	@Override
	public String toFieldName(String columnName, Class entityClass) {
		String s1 = toFieldName0(columnName, entityClass);

		if (s1 == null && isOpenEntityCanExtend()) {
			Class<?> superClass = null;
			Class<?> currentClass = entityClass;
			do {
				superClass = currentClass.getSuperclass(); // 拿的是父类，而不是字段的值
				if (HoneyUtil.isSuperEntity(superClass)) {
					s1 = toFieldName0(columnName, superClass);
					currentClass = superClass;
				} else {
					break;
				}
			} while (s1 == null);
		}

		return s1;
	}

	private String toFieldName0(String columnName, Class entityClass) {
		if (HoneyUtil.isJavaPackage(entityClass)) {
			Logger.debug("The parameter entityClass is from Java library");
			return null;
		}
		if (entityClass != null) {
			String entityFullName = entityClass.getName();
			Boolean flag = HoneyContext.getCustomFlagMap(field2Column + entityFullName);
			if (ObjectUtils.isTrue(flag)) {
				String fieldName = HoneyContext.getCustomMapValue(StringConst.Column2Field + entityFullName, columnName);
				if (fieldName != null) return fieldName;
			} // 确认查询之前是否都已经检测过一遍 返回标志是false,或没找到,则表示没有另外定义列名,使用原来的转换规则
		}
		return null;
	}

	void initDefineColumn(Class entityClass) {
		HoneyContext.initParseDefineColumn(entityClass);
	}

	private static boolean isOpenEntityCanExtend() {
		return HoneyConfig.getHoneyConfig().openEntityCanExtend;
	}

}
