/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.algorithm;

import org.teasoft.bee.sharding.algorithm.Calculate;
import org.teasoft.honey.util.currency.LongCalculator;

/**
 * @author Kingstar
 * @since  1.11-E
 */
public class DirectCalculate implements Calculate{

	@Override
	public String process(String rule, String oneValue) {
		return LongCalculator.calculate(rule, oneValue);
	}

//	@Override
//	public Long process(Long id) {
//		return null;
//	}
//
//	@Override
//	public String process(String str) {
//		return null;
//	}
	

}
