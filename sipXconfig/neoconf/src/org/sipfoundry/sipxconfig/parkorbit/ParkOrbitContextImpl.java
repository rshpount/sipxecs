/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
package org.sipfoundry.sipxconfig.parkorbit;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sipfoundry.sipxconfig.alias.AliasManager;
import org.sipfoundry.sipxconfig.cfgmgt.ConfigManager;
import org.sipfoundry.sipxconfig.common.BeanId;
import org.sipfoundry.sipxconfig.common.ExtensionInUseException;
import org.sipfoundry.sipxconfig.common.NameInUseException;
import org.sipfoundry.sipxconfig.common.SipxCollectionUtils;
import org.sipfoundry.sipxconfig.common.SipxHibernateDaoSupport;
import org.sipfoundry.sipxconfig.common.event.EntitySaveListener;
import org.sipfoundry.sipxconfig.commserver.SipxReplicationContext;
import org.sipfoundry.sipxconfig.setting.Group;
import org.sipfoundry.sipxconfig.setting.SettingDao;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class ParkOrbitContextImpl extends SipxHibernateDaoSupport implements ParkOrbitContext,
        BeanFactoryAware {
    private static final String VALUE = "value";
    private static final String QUERY_PARK_ORBIT_IDS_WITH_ALIAS = "parkOrbitIdsWithAlias";
    private static final String PARK_ORBIT_GROUP_ID = "park_orbit";

    private SipxReplicationContext m_replicationContext;
    private AliasManager m_aliasManager;
    private BeanFactory m_beanFactory;
    private SettingDao m_settingDao;
    private ConfigManager m_configManager;

    public void storeParkOrbit(ParkOrbit parkOrbit) {
        // Check for duplicate names and extensions before saving the park orbit
        String name = parkOrbit.getName();
        String extension = parkOrbit.getExtension();
        final String parkOrbitTypeName = "call park";
        if (!m_aliasManager.canObjectUseAlias(parkOrbit, name)) {
            throw new NameInUseException(parkOrbitTypeName, name);
        }
        if (!m_aliasManager.canObjectUseAlias(parkOrbit, extension)) {
            throw new ExtensionInUseException(parkOrbitTypeName, extension);
        }

        getHibernateTemplate().saveOrUpdate(parkOrbit);
    }

    public void removeParkOrbits(Collection ids) {
        if (ids.isEmpty()) {
            return;
        }
        // TODO: this inadvertantly circumvent daoevenlisteners
        removeAll(ParkOrbit.class, ids);
        m_configManager.replicationRequired(FEATURE);
    }

    public ParkOrbit loadParkOrbit(Integer id) {
        return (ParkOrbit) getHibernateTemplate().load(ParkOrbit.class, id);
    }

    public Collection getParkOrbits() {
        return getHibernateTemplate().loadAll(ParkOrbit.class);
    }

    public String getDefaultMusicOnHold() {
        return getBackgroundMusic().getMusic();
    }

    public void setDefaultMusicOnHold(String music) {
        BackgroundMusic backgroundMusic = getBackgroundMusic();
        backgroundMusic.setMusic(music);
        getHibernateTemplate().saveOrUpdate(backgroundMusic);
    }

    private BackgroundMusic getBackgroundMusic() {
        List musicList = getHibernateTemplate().loadAll(BackgroundMusic.class);
        if (!musicList.isEmpty()) {
            return (BackgroundMusic) musicList.get(0);
        }
        return new BackgroundMusic();
    }

    @Required
    public void setAliasManager(AliasManager aliasManager) {
        m_aliasManager = aliasManager;
    }

    public boolean isAliasInUse(String alias) {
        // Look for the ID of a park orbit with the specified alias as its name or extension.
        // If there is one, then the alias is in use.
        List objs = getHibernateTemplate().findByNamedQueryAndNamedParam(
                QUERY_PARK_ORBIT_IDS_WITH_ALIAS, VALUE, alias);
        return SipxCollectionUtils.safeSize(objs) > 0;
    }

    public Collection getBeanIdsOfObjectsWithAlias(String alias) {
        Collection ids = getHibernateTemplate().findByNamedQueryAndNamedParam(
                QUERY_PARK_ORBIT_IDS_WITH_ALIAS, VALUE, alias);
        Collection bids = BeanId.createBeanIdCollection(ids, ParkOrbit.class);
        return bids;
    }

    /**
     * Remove all park orbits - mostly used for testing
     */
    public void clear() {
        HibernateTemplate template = getHibernateTemplate();
        Collection orbits = template.loadAll(ParkOrbit.class);
        template.deleteAll(orbits);
        m_configManager.replicationRequired(FEATURE);
    }

    public ParkOrbit newParkOrbit() {
        ParkOrbit orbit = (ParkOrbit) m_beanFactory.getBean(ParkOrbit.class.getName(),
                ParkOrbit.class);

        // All auto attendants share same group: default
        Set groups = orbit.getGroups();
        if (groups == null || groups.isEmpty()) {
            orbit.addGroup(getDefaultParkOrbitGroup());
        }

        return orbit;
    }

    public Group getDefaultParkOrbitGroup() {
        return m_settingDao.getGroupCreateIfNotFound(PARK_ORBIT_GROUP_ID, "default");
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        m_beanFactory = beanFactory;
    }

    public void setSettingDao(SettingDao settingDao) {
        m_settingDao = settingDao;
    }

    public EntitySaveListener<Group> createGroupSaveListener() {
        return new OnGroupSave();
    }

    private class OnGroupSave extends EntitySaveListener<Group> {
        public OnGroupSave() {
            super(Group.class);
        }

        protected void onEntitySave(Group group) {
            if (PARK_ORBIT_GROUP_ID.equals(group.getResource()) && !group.isNew()) {
                m_configManager.replicationRequired(FEATURE);
            }
        }
    }
}
