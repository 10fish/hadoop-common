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

package org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationMaster;
import org.apache.hadoop.yarn.api.records.ApplicationState;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.QueueUserACLInfo;
import org.apache.hadoop.yarn.api.records.ResourceRequest;
import org.apache.hadoop.yarn.event.EventHandler;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ASMEvent;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ApplicationMasterEvents.ApplicationEventType;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ApplicationMasterEvents.ApplicationTrackerEventType;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.MemStore;
import org.apache.hadoop.yarn.server.resourcemanager.resourcetracker.NodeInfo;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.NodeManager;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.NodeResponse;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.security.ContainerTokenSecretManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSchedulerNegotiator extends TestCase {
  private static RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
  private SchedulerNegotiator schedulerNegotiator;
  private DummyScheduler scheduler;
  private final int testNum = 99999;
  
  private final RMContext context = new ResourceManager.RMContextImpl(new MemStore());
  ApplicationMasterInfo masterInfo;
  private EventHandler handler;
  
  private class DummyScheduler implements ResourceScheduler {
    @Override
    public List<Container> allocate(ApplicationId applicationId,
        List<ResourceRequest> ask, List<Container> release) throws IOException {
      ArrayList<Container> containers = new ArrayList<Container>();
      Container container = recordFactory.newRecordInstance(Container.class);
      container.setId(recordFactory.newRecordInstance(ContainerId.class));
      container.getId().setAppId(applicationId);
      container.getId().setId(testNum);
      containers.add(container);
      return containers;
    }
    @Override
    public void reinitialize(Configuration conf,
        ContainerTokenSecretManager secretManager) {
    }
    @Override
    public void addNode(NodeManager nodeManager) {
    }
    @Override
    public NodeResponse nodeUpdate(NodeInfo nodeInfo,
        Map<String, List<Container>> containers) {
      return null;
    }
    @Override
    public void removeNode(NodeInfo node) {
    }

    @Override
    public void handle(ASMEvent<ApplicationTrackerEventType> event) {
    }
   
    @Override
    public void removeApplication(ApplicationId applicationId)
        throws IOException {
    }
    @Override
    public QueueInfo getQueueInfo(String queueName,
        boolean includeApplications, boolean includeChildQueues,
        boolean recursive) throws IOException {
      return null;
    }
    @Override
    public List<QueueUserACLInfo> getQueueUserAclInfo() {
      return null;
    }
    @Override
    public void addApplication(ApplicationId applicationId,
        ApplicationMaster master, String user, String queue, Priority priority)
        throws IOException {
      // TODO Auto-generated method stub
      
    }
  }
  
  @Before
  public void setUp() {
    scheduler = new DummyScheduler();
    schedulerNegotiator = new SchedulerNegotiator(context, scheduler);
    Configuration conf = new Configuration();
    schedulerNegotiator.init(conf);
    schedulerNegotiator.start();
    handler = context.getDispatcher().getEventHandler();
    context.getDispatcher().init(conf);
    context.getDispatcher().start();
  }
  
  @After
  public void tearDown() {
    schedulerNegotiator.stop();
  }
  
  public void waitForState(ApplicationState state, ApplicationMasterInfo info) {
    int count = 0;
    while (info.getState() != state && count < 100) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
       e.printStackTrace();
      }
      count++;
    }
    Assert.assertEquals(state, info.getState());
  }
  
  private class DummyEventHandler implements EventHandler<ASMEvent<ApplicationTrackerEventType>> {
    @Override
    public void handle(ASMEvent<ApplicationTrackerEventType> event) {
    }
  }

  @Test
  public void testSchedulerNegotiator() throws Exception {
    ApplicationSubmissionContext submissionContext = recordFactory.newRecordInstance(ApplicationSubmissionContext.class);
    submissionContext.setApplicationId(recordFactory.newRecordInstance(ApplicationId.class));
    submissionContext.getApplicationId().setClusterTimestamp(System.currentTimeMillis());
    submissionContext.getApplicationId().setId(1);
    
    masterInfo =
      new ApplicationMasterInfo(this.context,
          "dummy", submissionContext, "dummyClientToken");
    context.getDispatcher().register(ApplicationEventType.class, masterInfo);
    context.getDispatcher().register(ApplicationTrackerEventType.class, masterInfo);
    handler.handle(new ASMEvent<ApplicationEventType>(ApplicationEventType.
    ALLOCATE, masterInfo));
    waitForState(ApplicationState.ALLOCATED, masterInfo);
    Container container = masterInfo.getMasterContainer();
    assertTrue(container.getId().getId() == testNum);
  }
}
