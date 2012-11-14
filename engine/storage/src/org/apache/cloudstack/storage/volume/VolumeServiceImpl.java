/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.volume;

import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.manager.PrimaryDataStoreManager;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;

import org.springframework.stereotype.Service;

import com.cloud.storage.Volume;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;

//1. change volume state
//2. orchestrator of volume, control most of the information of volume, storage pool id, voluem state, scope etc.

@Service
public class VolumeServiceImpl implements VolumeService {
	@Inject
	VolumeDao volDao;
	@Inject
	PrimaryDataStoreManager dataStoreMgr;
	@Override
	public VolumeInfo createVolume(VolumeInfo volume, long dataStoreId, VolumeDiskType diskType) {
		PrimaryDataStore dataStore = dataStoreMgr.getPrimaryDataStore(dataStoreId);
		if (dataStore == null) {
			throw new CloudRuntimeException("Can't find dataStoreId: " + dataStoreId);
		}
		
		if (dataStore.exists(volume)) {
			return volume;
		}
		
		VolumeObject vo = (VolumeObject)volume;
		vo.stateTransit(VolumeEvent.CreateRequested);
		
		try {
			VolumeInfo vi = dataStore.createVolume(vo, diskType);
			vo.stateTransit(VolumeEvent.OperationSucceeded);
			return vi;
		} catch (Exception e) {
			vo.stateTransit(VolumeEvent.OperationFailed);
			throw new CloudRuntimeException(e.toString());
		}
	}

	@DB
	@Override
	public boolean deleteVolume(long volumeId) {
		return true;
	}

	@Override
	public boolean cloneVolume(long volumeId, long baseVolId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean createVolumeFromSnapshot(long volumeId, long snapshotId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String grantAccess(long volumeId, long endpointId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean rokeAccess(long volumeId, long endpointId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public VolumeEntity allocateVolumeInDb(long size, VolumeType type, String volName, Long templateId) {
		VolumeVO vo = volDao.allocVolume(size, type, volName, templateId);
		return new VolumeEntityImpl(new VolumeObject(null, vo));
	}

	@Override
	public VolumeEntity getVolumeEntity(long volumeId) {
		VolumeVO vo = volDao.findById(volumeId);
		if (vo == null) {
			return null;
		}
		
		if (vo.getPoolId() == null) {
			return new VolumeEntityImpl(new VolumeObject(null, vo));
		} else {
			PrimaryDataStore dataStore = dataStoreMgr.getPrimaryDataStore(vo.getPoolId());
			return new VolumeEntityImpl(dataStore.getVolume(volumeId));
		}
	}
}
