package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.BeeAbstractFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class BeeFactory extends BeeAbstractFactory {

	private static HoneyFactory honeyFactory = null;

	public void setHoneyFactory(HoneyFactory honeyFactory) {
		this.honeyFactory = honeyFactory;
	}

	public static HoneyFactory getHoneyFactory() {
		if (honeyFactory == null) {
			honeyFactory = new HoneyFactory();
		}
		return honeyFactory;
	}

	public BeeFactory() {}

}
