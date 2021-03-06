/*
 * Copyright (c) 2011 eZuce, Inc. All rights reserved.
 * Contributed to SIPfoundry under a Contributor Agreement
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

#ifndef RegDB_H
#define	RegDB_H

//#include <sys/types.h>
//#include <sys/stat.h>
//#include <unistd.h>
#include <vector>
#include "sipdb/RegBinding.h"
#include "sipdb/MongoDB.h"
#include "net/Url.h"

#ifndef GRUU_PREFIX
#define GRUU_PREFIX "~~gr~"
#endif

#ifndef SIP_GRUU_URI_PARAM
#define SIP_GRUU_URI_PARAM "gr"
#endif

class RegDB : public MongoDB::BaseDB
{
public:
	static const std::string NS;
    typedef std::vector<RegBinding> Bindings;

 RegDB(const MongoDB::ConnectionInfo& info) :
    BaseDB(info, NS), _local(NULL), _expireGracePeriod(0), _expirationTimeIndexTTL(0)
	{
	}
	;

 RegDB(const MongoDB::ConnectionInfo& info, RegDB* local) :
     BaseDB(info, NS), _local(local), _expireGracePeriod(0), _expirationTimeIndexTTL(0)
	{
	}
	;

 RegDB(const MongoDB::ConnectionInfo& info, RegDB* local, const std::string& ns) :
    BaseDB(info, ns), _local(local), _expireGracePeriod(0), _expirationTimeIndexTTL(0)
	{
	}
	;

 ~RegDB()
	{
          if (_local) {
            delete _local;
            _local = NULL;
          }
	}
	;

    static RegDB* CreateInstance(bool ensureIndexes = false);

    void updateBinding(const RegBinding::Ptr& pBinding);

    void updateBinding(RegBinding& binding);

    void expireOldBindings(
        const std::string& identity,
        const std::string& callId,
        unsigned int cseq,
        unsigned long timeNow);

    void expireAllBindings(
        const std::string& identity,
        const std::string& callId,
        unsigned int cseq,
        unsigned long timeNow);

    void removeAllExpired();

    bool isOutOfSequence(
        const std::string& identity,
        const std::string& callId,
        unsigned int cseq) const;

    bool getUnexpiredContactsUser(
        const std::string& identity,
        unsigned long timeNow,
        Bindings& bindings,
        bool preferPrimary = false) const;

    bool getUnexpiredContactsUserContaining(
        const std::string& matchIdentity,
        unsigned long timeNow,
        Bindings& bindings,
        bool preferPrimary = false) const;
    
    bool getUnexpiredContactsUserWithAddress(
      const std::string& identity,
      const std::string& address,
      unsigned long timeNow,
      Bindings& bindings,
      bool preferPrimary = false) const;

    bool getUnexpiredContactsUserInstrument(
        const std::string& identity,
        const std::string& instrument,
        unsigned long timeNow,
        Bindings& bindings,
        bool preferPrimary = false) const;

    bool getUnexpiredContactsInstrument(
        const std::string& instrument,
        unsigned long timeNow,
        Bindings& bindings,
        bool preferPrimary = false) const;
    
    bool isRegisteredBinding(
      const Url& binding,
      bool preferPrimary = false);
    
    bool getUnexpiredRegisteredBinding(
      const Url& registeredBinding,
      Bindings& bindings,
      bool preferPrimary = false);

    void cleanAndPersist(int currentExpireTime);

    void clearAllBindings();

    void setLocalAddress(const std::string& localAddress) { _localAddress = localAddress; };

    const std::string& getLocalAddress() const { return _localAddress; };

	const std::string& getNS() const
	{
		return _ns;
	}
	;
  
  //
  // Set the additional time a registration record can linger after it expires.
  // This is expressed in seconds
  //
  void setExpireGracePeriod(unsigned long expireGracePeriod /* (seconds) */);
  unsigned long getExpireGracePeriod() const;

protected:

private:
    void ensureIndexes(mongo::DBClientBase* client = NULL);
    std::string _localAddress;
    RegDB* _local;
    unsigned long _expireGracePeriod;
    int _expirationTimeIndexTTL;
};

//
// Inlines
//

inline void RegDB::setExpireGracePeriod(unsigned long expireGracePeriod /* (seconds) */)
{
  _expireGracePeriod = expireGracePeriod;
}

inline unsigned long RegDB::getExpireGracePeriod() const
{
  return _expireGracePeriod;
}

#endif	/* RegDB_H */

