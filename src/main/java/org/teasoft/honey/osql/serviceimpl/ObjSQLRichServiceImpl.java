package org.teasoft.honey.osql.serviceimpl;

import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.SuidRich;
import org.teasoft.bee.osql.service.ObjSQLRichAbstractServiceImpl;
import org.teasoft.honey.osql.core.BeeFactory;

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
