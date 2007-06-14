/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * 
 */
package org.sipfoundry.sipxconfig.admin.dialplan.sbc;

import java.util.Collection;
import java.util.List;

import org.sipfoundry.sipxconfig.common.DaoUtils;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class SbcManagerImpl extends HibernateDaoSupport implements SbcManager {

    public DefaultSbc loadDefaultSbc() {
        List sbcs = getHibernateTemplate().loadAll(DefaultSbc.class);
        DefaultSbc sbc = (DefaultSbc) DataAccessUtils.singleResult(sbcs);
        if (sbc == null) {
            sbc = new DefaultSbc();
            sbc.setRoutes(new SbcRoutes());
            saveSbc(sbc);
        }
        return sbc;
    }

    public List<AuxSbc> loadAuxSbcs() {
        return getHibernateTemplate().loadAll(AuxSbc.class);
    }

    public void saveSbc(Sbc sbc) {
        getHibernateTemplate().saveOrUpdate(sbc);
    }

    public AuxSbc loadSbc(Integer sbcId) {
        return (AuxSbc) getHibernateTemplate().load(AuxSbc.class, sbcId);
    }

    public void removeSbcs(Collection<Integer> selectedRows) {
        HibernateTemplate hibernate = getHibernateTemplate();
        Collection sbcs = DaoUtils.loadBeanByIds(hibernate, AuxSbc.class, selectedRows);
        hibernate.deleteAll(sbcs);
    }

    public void clear() {
        HibernateTemplate hibernate = getHibernateTemplate();
        Collection sbcs = hibernate.loadAll(Sbc.class);
        hibernate.deleteAll(sbcs);
    }
}
