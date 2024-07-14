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
import org.teasoft.honey.util.ObjectUtils;

/**
 * 常用的注解拦截器,都在这了.国际化字典转换则需要设置从DB查询的数据.
 * 主要用于返回前的处理.
 * @author Kingstar
 * @since 1.11
 */
public class CustomInterceptor extends EmptyInterceptor {

	private final String partKey = StringConst.PREFIX + "CustomInterceptor";

	private static final long serialVersionUID = 1595293159217L;

	@Override
	public Object beforePasreEntity(Object entity, SuidType suidType) {
		return entity;
	}

	@Override
	public Object[] beforePasreEntity(Object[] entityArray, SuidType suidType) {
		return entityArray;
	}

	@Override
	public String afterCompleteSql(String sql) {
		return sql;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void beforeReturn(List list) {
		if (ObjectUtils.isEmpty(list)) {
			return;
		}

		Object entity = list.get(0);
		if (isSkip(entity,null)) {
			return;
		}

		Field fields[] = HoneyUtil.getFields(entity.getClass());
		int len = fields.length;
		boolean isHas = false;
		Field field;

		String key = partKey + "_beforeReturn" + entity.getClass().getName();
		Boolean flag = HoneyContext.getCustomFlagMap(key);
		if (Boolean.FALSE.equals(flag)) return; // 之前有检测过,没有相关注解

		for (int i = 0; i < len; i++) { // 遍历一行数据的多个字段 然后在底层处理完所有行
			field = fields[i];

			if (AnnoUtil.isDesensitize(fields[i])) {
				if (!isHas) isHas = true;
				DesensitizeHandler.process(field, list);
			}

			if (AnnoUtil.isDict(field)) {
				if (!isHas) isHas = true;
				DictHandler.process(field, list);
			} else if (AnnoUtil.isDictI18n(field)) {
				DictI18nHandler.process(field, list); // ok 是直接new实现类,注解的handler也可以删除了.
			}
		}

		if (flag == null) // 原来为null,还没设置的,会进行初次设置
			HoneyContext.addCustomFlagMap(key, isHas);
	}

	@Override
	public void beforeReturn() {
		// do nothing
	}

}
