/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(aiteasoft@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.math.BigInteger;
import java.security.MessageDigest;

import org.teasoft.honey.logging.Logger;

/**
 * @author Kingstar
 * @since  1.8.99
 */
public class MD5 {

	private MD5() {}

	public static String getMd5(String text) {
		byte[] secretBytes = null;
		String re = "";
		try {
			secretBytes = MessageDigest.getInstance("md5").digest(text.getBytes("utf8"));
		} catch (Exception e) {
			Logger.warn("Have Exception when generate MD5. " + e.getMessage());
			return null;
		}
		re = new BigInteger(1, secretBytes).toString(16);
		for (int i = 0; i < 32 - re.length(); i++) {
			re = "0" + re;
		}
		return re;
	}
}
