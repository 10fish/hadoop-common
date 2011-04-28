/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.hadoop.yarn.server.resourcemanager.resourcetracker;

import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.server.api.records.NodeHealthStatus;


public class NodeStatus {
  public static org.apache.hadoop.yarn.server.api.records.NodeStatus createNodeStatus(
      NodeId nodeId, Map<String, List<Container>> containers) {
    RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
    org.apache.hadoop.yarn.server.api.records.NodeStatus nodeStatus = recordFactory.newRecordInstance(org.apache.hadoop.yarn.server.api.records.NodeStatus.class);
    nodeStatus.setNodeId(nodeId);
    nodeStatus.addAllContainers(containers);
    nodeStatus.setNodeHealthStatus(recordFactory
        .newRecordInstance(NodeHealthStatus.class));
    nodeStatus.getNodeHealthStatus().setIsNodeHealthy(true);
    nodeStatus.setLastSeen(System.currentTimeMillis());
    return nodeStatus;
  }
}
