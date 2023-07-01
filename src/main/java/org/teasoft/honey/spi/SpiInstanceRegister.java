/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.osql.Registry;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class SpiInstanceRegister implements Registry {
	
	private static Map<Class<?>,Serializable> spiInstanceMap=new HashMap<>();
	
	@SuppressWarnings("unchecked")
	public static <T> T getInstance(Class<T> clazz) {
		T t = null;
		try {
			Serializable s=spiInstanceMap.get(clazz);
			if(s==null) return t;
			t = (T) HoneyUtil.copyObject(s);
		} catch (Exception e) {
			Logger.debug(e.getMessage());
		}
		return t;
	}

	public static void register(Class<?> clazz,Serializable instance) {
		spiInstanceMap.put(clazz,instance);
	}
	
}
