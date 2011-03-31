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


import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.NodeManager;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestResourceManager extends TestCase {
  private static final Log LOG = LogFactory.getLog(TestResourceManager.class);
  
  private ResourceManager resourceManager = null;
  
  @Before
  public void setUp() throws Exception {
    resourceManager = new ResourceManager();
    resourceManager.init(new Configuration());
  }

  @After
  public void tearDown() throws Exception {
  }

  private org.apache.hadoop.yarn.server.resourcemanager.NodeManager 
  registerNode(String hostName, String rackName, int memory) 
  throws IOException {
    return new org.apache.hadoop.yarn.server.resourcemanager.NodeManager(
        hostName, rackName, memory, resourceManager.getResourceTracker());
  }
  
  @Test
  public void testResourceManagerInitialization() throws IOException {
    LOG.info("--- START: testResourceManagerInitialization ---");
        
    final int memory = 16 * 1024;
    
    // Register node1
    String host1 = "host1";
    org.apache.hadoop.yarn.server.resourcemanager.NodeManager nm1 = 
      registerNode(host1, NetworkTopology.DEFAULT_RACK, memory);
    nm1.heartbeat();
    
    // Register node2
    String host2 = "host2";
    org.apache.hadoop.yarn.server.resourcemanager.NodeManager nm2 = 
      registerNode(host2, NetworkTopology.DEFAULT_RACK, memory);
    nm2.heartbeat();

    LOG.info("--- END: testResourceManagerInitialization ---");
  }

  @Test
  public void testApplicationSubmission() throws IOException {
    LOG.info("--- START: testApplicationSubmission ---");
        
    final int memory = 16 * 1024;
    
    // Register node1
    String host1 = "host1";
    org.apache.hadoop.yarn.server.resourcemanager.NodeManager nm1 = 
      registerNode(host1, NetworkTopology.DEFAULT_RACK, memory);
    nm1.heartbeat();
    
    // Register node 2
    String host2 = "host1";
    org.apache.hadoop.yarn.server.resourcemanager.NodeManager nm2 = 
      registerNode(host2, NetworkTopology.DEFAULT_RACK, memory);
    nm2.heartbeat();
    
    // Submit an application
    Application application = new Application("user1", resourceManager);
    application.submit();
    
    LOG.info("--- END: testApplicationSubmission ---");
  }

  @Test
  public void testResourceAllocation() throws IOException {
    LOG.info("--- START: testResourceAllocation ---");
        
    final int memory = 4 * 1024;
    
    // Register node1
    String host1 = "host1";
    org.apache.hadoop.yarn.server.resourcemanager.NodeManager nm1 = 
      registerNode(host1, NetworkTopology.DEFAULT_RACK, memory);
    nm1.heartbeat();
    
    // Register node2
    String host2 = "host2";
    org.apache.hadoop.yarn.server.resourcemanager.NodeManager nm2 = 
      registerNode(host2, NetworkTopology.DEFAULT_RACK, memory/2);
    nm2.heartbeat();

    // Submit an application
    Application application = new Application("user1", resourceManager);
    application.submit();
    
    application.addNodeManager(host1, nm1);
    application.addNodeManager(host2, nm2);
    
    // Application resource requirements
    final int memory1 = 1024;
    Resource capability1 = 
      org.apache.hadoop.yarn.server.resourcemanager.resource.Resource.createResource(
          memory1); 
    Priority priority1 = 
      org.apache.hadoop.yarn.server.resourcemanager.resource.Priority.create(1);
    application.addResourceRequestSpec(priority1, capability1);
    
    Task t1 = new Task(application, priority1, new String[] {host1, host2});
    application.addTask(t1);
        
    final int memory2 = 2048;
    Resource capability2 = 
      org.apache.hadoop.yarn.server.resourcemanager.resource.Resource.createResource(
          memory2); 
    Priority priority0 = 
      org.apache.hadoop.yarn.server.resourcemanager.resource.Priority.create(0); // higher
    application.addResourceRequestSpec(priority0, capability2);
    
    // Send resource requests to the scheduler
    application.schedule();
    
    // Send a heartbeat to kick the tires on the Scheduler
    nm1.heartbeat();
    
    // Get allocations from the scheduler
    application.schedule();
    
    nm1.heartbeat();
    checkResourceUsage(nm1, nm2);
    
    LOG.info("Adding new tasks...");
    
    Task t2 = new Task(application, priority1, new String[] {host1, host2});
    application.addTask(t2);

    Task t3 = new Task(application, priority0, new String[] {NodeManager.ANY});
    application.addTask(t3);

    // Send resource requests to the scheduler
    application.schedule();
    checkResourceUsage(nm1, nm2);
    
    // Send a heartbeat to kick the tires on the Scheduler
    LOG.info("Sending hb from host2");
    nm2.heartbeat();
    
    LOG.info("Sending hb from host1");
    nm1.heartbeat();
    
    // Get allocations from the scheduler
    LOG.info("Trying to allocate...");
    application.schedule();

    nm1.heartbeat();
    nm2.heartbeat();
    checkResourceUsage(nm1, nm2);
    
    // Complete tasks
    LOG.info("Finishing up tasks...");
    application.finishTask(t1);
    application.finishTask(t2);
    application.finishTask(t3);
    
    // Send heartbeat
    nm1.heartbeat();
    nm2.heartbeat();
    checkResourceUsage(nm1, nm2);
    
    LOG.info("--- END: testResourceAllocation ---");
  }

  private void checkResourceUsage(
      org.apache.hadoop.yarn.server.resourcemanager.NodeManager... nodes ) {
    for (org.apache.hadoop.yarn.server.resourcemanager.NodeManager nodeManager : nodes) {
      nodeManager.checkResourceUsage();
    }
  }

}
