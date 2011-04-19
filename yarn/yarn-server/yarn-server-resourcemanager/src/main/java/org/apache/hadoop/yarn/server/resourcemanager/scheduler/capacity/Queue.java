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

package org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceStability.Stable;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.Application;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.NodeManager;

/**
 * Queue represents a node in the tree of 
 * hierarchical queues in the {@link CapacityScheduler}.
 */
@Stable
@Private
public interface Queue 
extends org.apache.hadoop.yarn.server.resourcemanager.scheduler.Queue {
  /**
   * Get the parent <code>Queue</code>.
   * @return the parent queue
   */
  public Queue getParent();

  /**
   * Get the queue name.
   * @return the queue name
   */
  public String getQueueName();

  /**
   * Get the full name of the queue, including the heirarchy.
   * @return the full name of the queue
   */
  public String getQueuePath();
  
  /**
   * Get the configured <em>capacity</em> of the queue.
   * @return queue capacity
   */
  public float getCapacity();

  /**
   * Get capacity of the parent of the queue as a function of the 
   * cumulative capacity in the cluster.
   * @return capacity of the parent of the queue as a function of the 
   *         cumulative capacity in the cluster
   */
  public float getAbsoluteCapacity();
  
  /**
   * Get the configured maximum-capacity of the queue. 
   * @return the configured maximum-capacity of the queue
   */
  public float getMaximumCapacity();
  
  /**
   * Get maximum-capacity of the queue as a funciton of the cumulative capacity
   * of the cluster.
   * @return maximum-capacity of the queue as a funciton of the cumulative capacity
   *         of the cluster
   */
  public float getAbsoluteMaximumCapacity();
  
  /**
   * Get the currently utilized capacity of the queue 
   * relative to it's parent queue.
   * @return the currently utilized capacity of the queue 
   *         relative to it's parent queue
   */
  public float getUsedCapacity();
  
  /**
   * Get the currently utilized resources in the cluster 
   * by the queue and children (if any).
   * @return used resources by the queue and it's children 
   */
  public Resource getUsedResources();
  
  /**
   * Get the current <em>utilization</em> of the queue 
   * and it's children (if any).
   * Utilization is defined as the ratio of 
   * <em>used-capacity over configured-capacity</em> of the queue.
   * @return queue utilization
   */
  public float getUtilization();
  
  /**
   * Get child queues
   * @return child queues
   */
  public List<Queue> getChildQueues();
  
  /**
   * Get applications in this queue
   * @return applications in the queue
   */
  public List<Application> getApplications();
  
  /**
   * Submit a new application to the queue.
   * @param application application being submitted
   * @param user user who submitted the application
   * @param queue queue to which the application is submitted
   * @param priority application priority
   */
  public void submitApplication(Application application, String user, 
      String queue, Priority priority) 
  throws AccessControlException;
  
  /**
   * An application submitted to this queue has finished.
   * @param application
   * @param queue application queue 
   */
  public void finishApplication(Application application, String queue)
  throws AccessControlException;
  
  /**
   * Assign containers to applications in the queue or it's children (if any).
   * @param clusterResource the resource of the cluster.
   * @param node node on which resources are available
   * @return
   */
  public Resource assignContainers(Resource clusterResource, NodeManager node);
  
  /**
   * A container assigned to the queue has completed.
   * @param clusterResource the resource of the cluster
   * @param container completed container
   * @param application application to which the container was assigned
   */
  public void completedContainer(Resource clusterResource,
      Container container, Application application);

  /**
   * Get the number of applications in the queue.
   * @return number of applications
   */
  public int getNumApplications();

  
  /**
   * Reinitialize the queue.
   * @param queue new queue to re-initalize from
   * @param clusterResource resources in the cluster
   */
  public void reinitialize(Queue queue, Resource clusterResource) 
  throws IOException;
}
