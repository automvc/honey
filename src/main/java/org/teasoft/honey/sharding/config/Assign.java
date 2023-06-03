/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class Assign {

	public static List<String> order(int min, int max) {
		List<String> list = new ArrayList<>();
		for (int j = min; j <= max; j++) {
			list.add(j + "");
		}
		return list;
	}

	public static List<String> polling(int min, int max, int size) {
		List<String> list = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			for (int j = min + i; j <= max; j = j + size) {
				list.add(j + "");
			}
		}

		return list;
	}

}
