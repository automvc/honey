/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

/**
 * @author Kingstar
 * @since  1.17
 */
public class UUID {
	
	private UUID() {}

	public static String getId() {
		return java.util.UUID.randomUUID().toString().replace("-", "");
	}
	
	public static String getId(boolean includeSeparator) {
		if (includeSeparator)
			return java.util.UUID.randomUUID().toString();
		else
			return getId();
	}

}
