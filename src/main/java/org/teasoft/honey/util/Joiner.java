/*
 * Copyright 2019-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

/**
 * @author Kingstar
 * @since  2.4.0
 */
public class Joiner {
	public static String join(String delimiter, String... elements) {

		if (delimiter == null || elements == null) return null;
		if (elements.length == 0) return null;
		if (elements.length == 1) return elements[0];

		StringBuilder idsStr = new StringBuilder(elements[0]);
		for (int i = 1; i < elements.length; i++) { // from i=1
			idsStr.append(delimiter);
			idsStr.append(elements[i]);
		}
		return idsStr.toString();
	}

	public static String join(String delimiter, Number... elements) {

		if (delimiter == null || elements == null) return null;
		if (elements.length == 0) return null;
		if (elements.length == 1) {
			if (elements[0] == null)
				return null;
			else
				return elements[0] + "";
		}

		StringBuilder idsStr = new StringBuilder(elements[0] + "");
		for (int i = 1; i < elements.length; i++) { // from i=1
			idsStr.append(delimiter);
			idsStr.append(elements[i]);
		}
		return idsStr.toString();
	}

}
