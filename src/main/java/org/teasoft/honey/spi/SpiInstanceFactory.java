/*
 * Copyright 2016-2022 the original author.All rights reserved.
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

//	@SuppressWarnings("deprecation")
	public static JsonTransform getJsonTransform() {

		JsonTransform jsonTransform = SpiInstanceRegister.getInstance(JsonTransform.class);
		try {
//			if (jsonTransform == null) jsonTransform = (JsonTransform) Class
//					.forName("org.teasoft.beex.spi.JsonTransformDefault").newInstance();

//			if (jsonTransform == null) jsonTransform = (JsonTransform) Class
//					.forName("org.teasoft.beex.spi.FastJsonTransform").newInstance();

			try {
				// 1.jackson
				if (jsonTransform == null)
					jsonTransform = (JsonTransform) Class.forName("org.teasoft.beex.spi.JsonTransformDefault")
							.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				Logger.debug(e.getMessage(), e);
			}
			// 2.fastjson
			if (jsonTransform == null)
				jsonTransform = (JsonTransform) Class.forName("org.teasoft.beex.spi.FastJsonTransform")
						.getDeclaredConstructor().newInstance();

		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

		return jsonTransform;
	}

}
