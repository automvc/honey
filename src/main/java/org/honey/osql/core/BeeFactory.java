package org.honey.osql.core;

import org.bee.osql.BeeAbstractFactory;

/**
 * @author KingStar
 * @since  1.0
 */
public class BeeFactory extends BeeAbstractFactory {

	public static HoneyFactory getHoneyFactory() {
		return new HoneyFactory();
	}

}
