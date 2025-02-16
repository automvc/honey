/*
 * Copyright 2016-2024 the original author.All rights reserved.
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

	private CalculateFactory() {}

	public static Calculate getCalculate(int type) {
		if (type == 0)
			return new DirectCalculate();
//		else if (type == 1)
//			return new DateCalculate();  //2.4.0    放注册器好些,这样用户可以覆盖.   在Config首次注册
		else {
			// 从算法注册器中获取
			return CalculateRegistry.getCalculate(type);
		}
	}

}
