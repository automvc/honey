/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.teasoft.bee.spi.PreLoad;

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
			_initLoad();
		}
	}

	private static void _initLoad() {
//		Logger.info("[Bee] ========= BeeInitPreLoadService initLoad..."); //closed  V1.17
		ServiceLoader<PreLoad> loads = ServiceLoader.load(PreLoad.class);
		Iterator<PreLoad> loadIterator = loads.iterator();
		while (loadIterator.hasNext()) {
			try {
				loadIterator.next();
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
