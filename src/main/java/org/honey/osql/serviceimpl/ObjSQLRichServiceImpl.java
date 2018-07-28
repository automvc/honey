package org.honey.osql.serviceimpl;

import org.bee.osql.Suid;
import org.bee.osql.SuidRich;
import org.bee.osql.service.ObjSQLRichAbstractServiceImpl;
import org.honey.osql.core.BeeFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjSQLRichServiceImpl extends ObjSQLRichAbstractServiceImpl {

	@Override
	public Suid getSuid() {
		return BeeFactory.getHoneyFactory().getSuid();
	}

	@Override
	public SuidRich getSuidRich() {
		return BeeFactory.getHoneyFactory().getSuidRich();
	}
}
