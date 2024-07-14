/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept.annotation;

import java.lang.reflect.Field;
import java.util.List;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.osql.interccept.EmptyInterceptor;
import org.teasoft.honey.osql.util.AnnoUtil;

/**
 * 解析实体前的自动设值.
 * @author Kingstar
 * @since 1.11-E
 */
public class CustomAutoSetInterceptor extends EmptyInterceptor {

	private final String partKey = StringConst.PREFIX + "CustomInterceptor";

	private static final long serialVersionUID = 1595293159217L;

	@Override
	public Object beforePasreEntity(Object entity, SuidType suidType) {
		if (isSkip(entity,suidType)) return entity;

		String key = partKey + "_beforePasreEntity" + entity.getClass().getName();
		Boolean flag = HoneyContext.getCustomFlagMap(key);
		if (Boolean.FALSE.equals(flag)) return entity;

		Field fields[] = HoneyUtil.getFields(entity.getClass());
		int len = fields.length;
		boolean isHas = false;

		for (int i = 0; i < len; i++) {
			if (AnnoUtil.isAutoSetString(fields[i])) {
				if (flag == null && !isHas) isHas = true;
				AutoSetStringHandler.process(fields[i], entity, suidType);
			}
		} // end for 遍历完字段

		if (flag == null) // 原来为null,还没设置的,会进行初次设置
			HoneyContext.addCustomFlagMap(key, isHas);

		return entity;
	}

	@Override
	public Object[] beforePasreEntity(Object[] entityArray, SuidType suidType) {
		for (int i = 0; i < entityArray.length; i++) {
			beforePasreEntity(entityArray[i], suidType);
		}
		return entityArray;
	}

	@Override
	public String afterCompleteSql(String sql) {
		return sql;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void beforeReturn(List list) {
		// do nothing
	}

	@Override
	public void beforeReturn() {
		// do nothing
	}

}
