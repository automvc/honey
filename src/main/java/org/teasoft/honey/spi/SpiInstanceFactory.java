/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.spi;

import org.teasoft.bee.spi.JsonTransform;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author AiTeaSoft
 * @since 2.0
 */
public class SpiInstanceFactory {

	private static boolean DONE = false;

	// change in V2.1
	public static JsonTransform getJsonTransform() {

		JsonTransform jsonTransform = SpiInstanceRegister.getInstance(JsonTransform.class);
		try {
			try {
				// 1.jackson
				if (jsonTransform == null) {
					Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
					if (!DONE) {
						DONE = true;
						Logger.info("Use the json jar is com.fasterxml.jackson!");
					}
					jsonTransform = (JsonTransform) Class.forName("org.teasoft.beex.spi.JsonTransformDefault")
							.getDeclaredConstructor().newInstance();
				}
			} catch (Exception e) {
				Logger.debug(e.getMessage(), e);
			}

			try {
				// 2.fastjson V2.1
				if (jsonTransform == null) {
					Class.forName("com.alibaba.fastjson.JSON");
					if (!DONE) {
						DONE = true;
						Logger.info("Use the json jar is com.alibaba.fastjson!");
					}
					jsonTransform = (JsonTransform) Class.forName("org.teasoft.beex.spi.FastJsonTransform")
							.getDeclaredConstructor().newInstance();
				}
			} catch (Exception e) {
				Logger.debug(e.getMessage(), e);
			}

			if (jsonTransform == null) {
				Logger.warn("Can not find any json jar !");
			}

		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
		}

		return jsonTransform;
	}

}
