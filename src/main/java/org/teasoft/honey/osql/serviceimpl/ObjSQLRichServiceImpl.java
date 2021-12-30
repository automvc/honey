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
	
	private Suid suid;
	private SuidRich suidRich;
	
	public void setSuid(Suid suid) {
		this.suid = suid;
	}
	
	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}

	@Override
	public Suid getSuid() {
		if(suid==null) return BeeFactory.getHoneyFactory().getSuid();
		return suid;
	}

	@Override
	public SuidRich getSuidRich() {
		if(suidRich==null) return BeeFactory.getHoneyFactory().getSuidRich();
		return suidRich;
	}
}
