package org.honey.osql.serviceimpl;

import org.bee.osql.Suid;
import org.bee.osql.service.ObjSQLAbstractServiceImpl;
import org.honey.osql.core.BeeFactory;

/**
 * @author KingStar
 * @since  1.0
 */
public class ObjSQLServiceImpl extends ObjSQLAbstractServiceImpl {

	@Override
	public Suid getSuid() {
		return BeeFactory.getHoneyFactory().getSuid();
	}
}
