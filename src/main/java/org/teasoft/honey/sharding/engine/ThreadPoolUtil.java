/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * @author Kingstar
 * @since  2.1.7
 */
public class ThreadPoolUtil {
	
//	public static ExecutorService getThreadPool() {
//		int executorSize = HoneyConfig.getHoneyConfig().executorSize;
//		if (executorSize <= 0) //没有设置
//			return Executors.newCachedThreadPool();
//		else
//			return Executors.newFixedThreadPool(executorSize);
//	}
	
	public static ExecutorService getThreadPool(int hopeSize) {
//		int executorSize = HoneyConfig.getHoneyConfig().executorSize; //bug
		int executorSize = HoneyConfig.getHoneyConfig().sharding_executorSize; //fixed bug 2.1.10
		if (hopeSize > 0 && executorSize>0 && hopeSize < executorSize) //实际需要的少于设置的,用少的
			return Executors.newFixedThreadPool(hopeSize);
		else if (executorSize <= 0) //没有设置
			return Executors.newCachedThreadPool();
		else
			return Executors.newFixedThreadPool(executorSize);
	}

}
