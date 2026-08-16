/*
 * Copyright 2022-2026 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.lang.reflect.Field;
import java.util.List;

import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyContext1;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.LocalType;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.util.ObjectUtils;

/**
 * @author Kingstar
 * @since  3.0.0
 */
public class RecoverAddedOrderBySelectColumnDecorator {
	
	@SuppressWarnings("unchecked")
	public static <T> void recover(List<T> rsList) {

		List<String> addedOrderByFields = (List<String>) HoneyContext1.get(LocalType.SHARDING,
				StringConst.AddedOrderByField);

		if (ObjectUtils.isEmpty(addedOrderByFields) || rsList == null || rsList.isEmpty()) return;
		try {
			T entity = (T) rsList.get(0).getClass().newInstance();
			Field[] fields = new Field[addedOrderByFields.size()];
			for (int i = 0; i < fields.length; i++) {
				fields[i] = HoneyUtil.getField(rsList.get(0).getClass(), addedOrderByFields.get(i));
				HoneyUtil.setAccessibleTrue(fields[i]);
				Object v = HoneyUtil.getValue(fields[i], entity);
				for (T t : rsList) {
					HoneyUtil.setFieldValue(fields[i], t, v);
				}
			}
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException e) {
			throw ExceptionHelper.convert(e);
		}

	}
}
