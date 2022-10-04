/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.algorithm;

import org.teasoft.bee.sharding.algorithm.Calculate;
import org.teasoft.bee.sharding.algorithm.CalculateRegistry;

/**
 * @author Kingstar
 * @since  1.11-E
 */
public class CalculateFactory {

	private CalculateFactory() {

	}

	public static Calculate getCalculate(int type) {
		if (type == 0)
			return new DirectCalculate();
		else {
			//从算法注册器中获取
			return CalculateRegistry.getCalculate(type);
		}
	}

}
