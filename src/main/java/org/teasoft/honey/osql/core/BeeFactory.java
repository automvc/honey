package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.BeeAbstractFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class BeeFactory extends BeeAbstractFactory {

	public static HoneyFactory getHoneyFactory() {
		return new HoneyFactory();
	}
	
	public BeeFactory(){
	}

}
