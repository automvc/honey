/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.spi;

import org.teasoft.bee.spi.BeanSort;
import org.teasoft.bee.spi.JsonTransform;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class SpiInstanceFactory {

	public static JsonTransform getJsonTransform() {

		JsonTransform jsonTransform = SpiInstanceRegister.getInstance(JsonTransform.class);
		try {
			if (jsonTransform == null) jsonTransform = (JsonTransform) Class
					.forName("org.teasoft.beex.spi.JsonTransformDefault").newInstance();
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

		return jsonTransform;
	}

	public static BeanSort getBeanSort() {
		BeanSort beanSort = SpiInstanceRegister.getInstance(BeanSort.class);
		try {
			if (beanSort == null) beanSort = (BeanSort) Class
					.forName("org.teasoft.beex.sort.CommSort").newInstance();
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

		return beanSort;
	}

}
