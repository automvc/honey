package org.teasoft.honey.osql.serviceimpl;

import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.service.ObjSQLAbstractServiceImpl;
import org.teasoft.honey.osql.core.BeeFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjSQLServiceImpl extends ObjSQLAbstractServiceImpl {

	private Suid suid;

	public void setSuid(Suid suid) {
		this.suid = suid;
	}

	@Override
	public Suid getSuid() {
		if (suid == null) return BeeFactory.getHoneyFactory().getSuid();
		return suid;
	}
}
