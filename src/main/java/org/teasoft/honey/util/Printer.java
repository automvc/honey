/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.List;

import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  2.0
 */
public class Printer {

	public static void print(List<String[]> list) {
		if (ObjectUtils.isEmpty(list)) return;
		String a[] = null;
		String row = null;
		for (int i = 0; list != null && i < list.size(); i++) {
			a = list.get(i);
			row = "";
			for (int j = 0; j < a.length; j++) {
				row += a[j] + "   ";
			}
			Logger.info(row);
		}
	}
	
	public static void printList(List<?> list) {
		if (list == null) {
			Logger.info("null");
			return ;
		}
		for (int i = 0; i < list.size(); i++) {
			Logger.info(list.get(i).toString());
		}
	}
}
