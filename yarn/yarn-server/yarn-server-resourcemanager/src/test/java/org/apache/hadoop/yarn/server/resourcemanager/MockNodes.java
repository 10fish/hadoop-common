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

package org.apache.hadoop.yarn.server.resourcemanager;

import com.google.common.collect.Lists;

import java.util.List;

import org.apache.hadoop.net.Node;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.server.api.records.NodeHealthStatus;
import org.apache.hadoop.yarn.server.resourcemanager.resourcetracker.NodeInfo;

/**
 * Test helper to generate mock nodes
 */
public class MockNodes {
  private static int NODE_ID = 0;
  private static RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);

  public static List<NodeInfo> newNodes(int racks, int nodesPerRack,
                                        Resource perNode) {
    List<NodeInfo> list = Lists.newArrayList();
    for (int i = 0; i < racks; ++i) {
      for (int j = 0; j < nodesPerRack; ++j) {
        list.add(newNodeInfo(i, perNode));
      }
    }
    return list;
  }

  public static NodeId newNodeID(int id) {
    NodeId nid = recordFactory.newRecordInstance(NodeId.class);
    nid.setId(id);
    return nid;
  }

  public static Resource newResource(int mem) {
    Resource rs = recordFactory.newRecordInstance(Resource.class);
    rs.setMemory(mem);
    return rs;
  }

  public static Resource newUsedResource(Resource total) {
    Resource rs = recordFactory.newRecordInstance(Resource.class);
    rs.setMemory((int)(Math.random() * total.getMemory()));
    return rs;
  }

  public static Resource newAvailResource(Resource total, Resource used) {
    Resource rs = recordFactory.newRecordInstance(Resource.class);
    rs.setMemory(total.getMemory() - used.getMemory());
    return rs;
  }

  public static NodeInfo newNodeInfo(int rack, final Resource perNode) {
    final String rackName = "rack"+ rack;
    final int nid = NODE_ID++;
    final NodeId nodeID = newNodeID(nid);
    final String hostName = "host"+ nid;
    final String httpAddress = "localhost:0";
    final NodeHealthStatus nodeHealthStatus =
        recordFactory.newRecordInstance(NodeHealthStatus.class);
    final Resource used = newUsedResource(perNode);
    final Resource avail = newAvailResource(perNode, used);
    final int containers = (int)(Math.random() * 8);
    return new NodeInfo() {
      @Override
      public NodeId getNodeID() {
        return nodeID;
      }

      @Override
      public String getNodeAddress() {
        return hostName;
      }

      @Override
      public String getHttpAddress() {
        return httpAddress;
      }

      @Override
      public Resource getTotalCapability() {
        return perNode;
      }

      @Override
      public String getRackName() {
        return rackName;
      }

      @Override
      public Node getNode() {
        throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public Resource getAvailableResource() {
        return avail;
      }

      @Override
      public Resource getUsedResource() {
        return used;
      }

      @Override
      public int getNumContainers() {
        return containers;
      }

      @Override
      public NodeHealthStatus getNodeHealthStatus() {
        return nodeHealthStatus;
      }
    };
  }
}
