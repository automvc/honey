/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept.annotation;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.annotation.Dict;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since 1.11-E
 */
public class DictHandler {

	private static final String placeholderStr = "@#placeholderStr(default do not process)";

	private DictHandler() {

	}

	/**
	 * 
	 * @param field 带有注解的Field
	 * @param list 需要处理注解的List
	 */
	@SuppressWarnings("rawtypes")
	public static void process(Field field, List list) {
		try {
			Dict anno = field.getAnnotation(Dict.class);
			String mapStr = anno.map(); // eg:0=No,1=Yes

			Map<String, String> dictMap = new HashMap<>();

			if (StringUtils.isNotBlank(mapStr)) {
				String kvPair[] = mapStr.split(",");
				String kv[];
				for (int i = 0; i < kvPair.length; i++) {
					kv = kvPair[i].split("=");
					dictMap.put(kv[0], kv[1]);
				}
			}

			boolean isReplaceNull = false;
			String nullToValue = anno.nullToValue();
			if (!placeholderStr.equals(nullToValue)) isReplaceNull = true;

//			.println("----------mapStr:" + mapStr);
//			.println("----------nullToValue:  " + nullToValue);

			for (int i = 0; i < list.size(); i++) {
				Object obj = list.get(i);
				Field f = HoneyUtil.getField(obj.getClass(), field.getName());
				HoneyUtil.setAccessibleTrue(f);
				Object value = f.get(obj);
				if (value == null) {
					if (isReplaceNull) HoneyUtil.setFieldValue(f, obj, nullToValue);
				} else {
					String v = dictMap.get((String) value); // 没有映射则不替换
					if (v != null) HoneyUtil.setFieldValue(f, obj, v);
				}
			}

		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
		}
	}

}
