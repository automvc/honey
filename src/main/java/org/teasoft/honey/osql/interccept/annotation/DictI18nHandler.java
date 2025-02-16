/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept.annotation;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Kingstar
 * @since  1.11-E
 */
public class DictI18nHandler {

	private static DictI18nDefaultHandler handler = new DictI18nDefaultHandler();

	private DictI18nHandler() {

	}

	@SuppressWarnings("rawtypes")
	public static void process(Field field, List list) {
		handler.process(field, list);
	}

}
