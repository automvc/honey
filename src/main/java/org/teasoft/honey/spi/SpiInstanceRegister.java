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

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class SpiInstanceRegister implements Registry {
	
	private static Map<Class<?>,Serializable> spiInstanceMap=new HashMap<>();
	
	@SuppressWarnings("unchecked")
	public static <T> T getInstance(Class<T> clazz) {
		return (T)HoneyUtil.copyObject(spiInstanceMap.get(clazz));
	}

	public static void register(Class<?> clazz,Serializable instance) {
		spiInstanceMap.put(clazz,instance);
	}
	
}
