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

import org.apache.hadoop.net.Node;
import org.apache.hadoop.yarn.server.api.records.NodeHealthStatus;
import org.apache.hadoop.yarn.server.api.records.NodeId;

/**
 * Node managers information on available resources 
 * and other static information.
 *
 */
public interface NodeInfo {
  /**
   * the node id of of this node.
   * @return the node id of this node.
   */
  public NodeId getNodeID();
  /**
   * the ContainerManager address for this node.
   * @return the ContainerManager address for this node.
   */
  public String getNodeAddress();
  /**
   * the http-Address for this node.
   * @return the http-url address for this node
   */
  public String getHttpAddress();
  /**
   * the health-status for this node
   * @return the health-status for this node.
   */
  public NodeHealthStatus getNodeHealthStatus();
  /**
   * the total available resource.
   * @return the total available resource.
   */
  public org.apache.hadoop.yarn.api.records.Resource getTotalCapability();
  /**
   * The rack name for this node manager.
   * @return the rack name.
   */
  public String getRackName();
  /**
   * the {@link Node} information for this node.
   * @return {@link Node} information for this node.
   */
  public Node getNode();
  /**
   * the available resource for this node.
   * @return the available resource this node.
   */
  public org.apache.hadoop.yarn.api.records.Resource getAvailableResource();
  /**
   * used resource on this node.
   * @return the used resource on this node.
   */
  public org.apache.hadoop.yarn.api.records.Resource getUsedResource();
  /**
   * The current number of containers for this node
   * @return the number of containers
   */
  public int getNumContainers();
 }