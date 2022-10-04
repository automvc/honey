/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.teasoft.bee.spi.BeanSort;
import org.teasoft.bee.spi.JsonTransform;
import org.teasoft.bee.spi.PreLoad;
import org.teasoft.honey.spi.SpiInstanceRegister;

/**
 * @author Kingstar
 * @since  1.11
 */
public class BeeInitPreLoadService {

	static boolean notStart = true;

	static {
		initLoad();
	}

	static void initLoad() {
		if (notStart) {
			notStart = false;
			
			Logger.info("[Bee] ========= BeeInitPreLoadService initLoad..."); 
			
//			ServiceLoader<PreLoad> loads = ServiceLoader.load(PreLoad.class);
//			Iterator<PreLoad> loadIterator = loads.iterator();
//			while (loadIterator.hasNext()) {
//				try {
//					loadIterator.next();
//				} catch (ServiceConfigurationError e) {
//					Logger.error(e.getMessage(), e);
//				}
//			}
			
//			Class<PreLoad> clazz0=PreLoad.class;
//			loadClass(clazz0);
			
//			Class<JsonTransform> clazz1=JsonTransform.class;
//			loadClassAndReg(clazz1);
			
			loadServiceInstance(PreLoad.class);
			loadServiceInstanceAndReg(JsonTransform.class);
			loadServiceInstanceAndReg(BeanSort.class);
		}
	}
	
	private static <T> void loadServiceInstance(Class<T> clazz) {
		ServiceLoader<T> loads = ServiceLoader.load(clazz);
		Iterator<T> loadIterator = loads.iterator();
		while (loadIterator.hasNext()) {
			try {
				loadIterator.next();
			} catch (ServiceConfigurationError e) {
				Logger.error(e.getMessage(), e);
			}
		}
	}
	
	private static <T extends Serializable> void loadServiceInstanceAndReg(Class<T> clazz) {
		ServiceLoader<T> loads = ServiceLoader.load(clazz);
		Iterator<T> loadIterator = loads.iterator();
		while (loadIterator.hasNext()) {
			try {
				T obj=loadIterator.next();
				SpiInstanceRegister.register(clazz, obj);
			} catch (ServiceConfigurationError e) {
				Logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * 提供给其它应用首次初始化Bee.
	 * for other application init Bee first.
	 */
	public static void init() {
		Logger.info("[Bee] ========= BeeInitPreLoadService init...");
	}

	static boolean isNotStart() {
		return notStart;
	}

	static void setNotStart(boolean notStart) {
		BeeInitPreLoadService.notStart = notStart;
	}

}
