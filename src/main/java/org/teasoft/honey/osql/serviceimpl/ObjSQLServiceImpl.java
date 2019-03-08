package org.teasoft.honey.osql.serviceimpl;

import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.service.ObjSQLAbstractServiceImpl;
import org.teasoft.honey.osql.core.BeeFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjSQLServiceImpl extends ObjSQLAbstractServiceImpl {

	@Override
	public Suid getSuid() {
		return BeeFactory.getHoneyFactory().getSuid();
	}
}
