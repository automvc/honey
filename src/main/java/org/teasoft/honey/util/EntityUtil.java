/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.lang.reflect.Field;

import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.NameTranslateHandle;

/**
 * 实体相关工具类
 * @author Kingstar
 * @since  1.11
 */
public final class EntityUtil {
	
	private EntityUtil() {}

	/**
	 * 获取实体的字段名称,不作命名转换
	 * 会忽略部分注解字段
	 * @param entity 实体对象
	 * @return 实体的字段名称
	 */
	public static String getFieldNames(Object entity) {
		return getFieldNames(entity, false);
	}

	/**
	 * 获取实体的字段名称
	 * 会忽略部分注解字段
	 * @param entity 实体对象
	 * @param isTransform 是否命名转换标识
	 * @return 实体的字段名称
	 */
	public static String getFieldNames(Object entity, boolean isTransform) {
		if (entity == null) return "";
		Field fields[] = entity.getClass().getDeclaredFields();

		if (fields == null) return "";

		StringBuffer s = new StringBuffer();
		int len = fields.length;
		boolean isFirst = true;

		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipField(fields[i])) continue;
			if (HoneyUtil.isSkipFieldJustFetch(fields[i])) continue;

			if (isFirst) {
				isFirst = false;
			} else {
				s.append(",");
			}
			if (isTransform)
				s.append(NameTranslateHandle.toColumnName(fields[i].getName()));
			else
				s.append(fields[i].getName());

		}
		return s.toString();

	}

}
