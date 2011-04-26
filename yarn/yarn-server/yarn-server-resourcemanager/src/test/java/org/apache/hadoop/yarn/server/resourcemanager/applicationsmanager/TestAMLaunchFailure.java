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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationMaster;
import org.apache.hadoop.yarn.api.records.ApplicationState;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.QueueUserACLInfo;
import org.apache.hadoop.yarn.api.records.ResourceRequest;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.event.EventHandler;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.security.ApplicationTokenSecretManager;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager.ASMContext;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ASMEvent;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ApplicationMasterEvents.AMLauncherEventType;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ApplicationMasterEvents.ApplicationEventType;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ApplicationMasterEvents.ApplicationTrackerEventType;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.YarnScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/* a test case that tests the launch failure of a AM */
public class TestAMLaunchFailure extends TestCase {
  private static final Log LOG = LogFactory.getLog(TestAMLaunchFailure.class);
  private static final RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
  ApplicationsManagerImpl asmImpl;
  YarnScheduler scheduler = new DummyYarnScheduler();
  ApplicationTokenSecretManager applicationTokenSecretManager = 
    new ApplicationTokenSecretManager();

  private ASMContext context;

  private static class DummyYarnScheduler implements YarnScheduler {
    private Container container = recordFactory.newRecordInstance(Container.class);

    @Override
    public List<Container> allocate(ApplicationId applicationId,
        List<ResourceRequest> ask, List<Container> release) throws IOException {
      return Arrays.asList(container);
    }

    @Override
    public void addApplication(ApplicationId applicationId, String user,
        String queue, Priority priority) throws IOException {
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
  }

  private class DummyApplicationTracker implements EventHandler<ASMEvent<ApplicationTrackerEventType>> {
    public DummyApplicationTracker() {
      context.getDispatcher().register(ApplicationTrackerEventType.class, this);
    }
    @Override
    public void handle(ASMEvent<ApplicationTrackerEventType> event) {
    }
  }

  public class ExtApplicationsManagerImpl extends ApplicationsManagerImpl {

    private  class DummyApplicationMasterLauncher implements EventHandler<ASMEvent<AMLauncherEventType>> {
      private AtomicInteger notify = new AtomicInteger();
      private AppContext app;

      public DummyApplicationMasterLauncher(ASMContext context) {
        context.getDispatcher().register(AMLauncherEventType.class, this);
        new TestThread().start();
      }
      @Override
      public void handle(ASMEvent<AMLauncherEventType> appEvent) {
        switch(appEvent.getType()) {
        case LAUNCH:
          LOG.info("LAUNCH called ");
          app = appEvent.getAppContext();
          synchronized (notify) {
            notify.addAndGet(1);
            notify.notify();
          }
          break;
        }
      }

      private class TestThread extends Thread {
        public void run() {
          synchronized(notify) {
            try {
              while (notify.get() == 0) {
                notify.wait();
              }
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            context.getDispatcher().getEventHandler().handle(
                new ASMEvent<ApplicationEventType>(ApplicationEventType.LAUNCHED,
                    app));
          }
        }
      }
    }

    public ExtApplicationsManagerImpl(
        ApplicationTokenSecretManager applicationTokenSecretManager,
        YarnScheduler scheduler) {
      super(applicationTokenSecretManager, scheduler, context);
    }

    @Override
    protected EventHandler<ASMEvent<AMLauncherEventType>> createNewApplicationMasterLauncher(
        ApplicationTokenSecretManager tokenSecretManager) {
      return new DummyApplicationMasterLauncher(context);
    }
  }


  @Before
  public void setUp() {
    context = new ResourceManager.ASMContextImpl();
    asmImpl = new ExtApplicationsManagerImpl(applicationTokenSecretManager, scheduler);
    Configuration conf = new Configuration();
    new DummyApplicationTracker();
    conf.setLong(YarnConfiguration.AM_EXPIRY_INTERVAL, 3000L);
    conf.setInt(YarnConfiguration.AM_MAX_RETRIES, 1);
    asmImpl.init(conf);
    asmImpl.start();  
  }

  @After
  public void tearDown() {
    asmImpl.stop();
  }

  private ApplicationSubmissionContext createDummyAppContext(ApplicationId appID) {
    ApplicationSubmissionContext context = recordFactory.newRecordInstance(ApplicationSubmissionContext.class);
    context.setApplicationId(appID);
    return context;
  }

  @Test
  public void testAMLaunchFailure() throws Exception {
    ApplicationId appID = asmImpl.getNewApplicationID();
    ApplicationSubmissionContext context = createDummyAppContext(appID);
    asmImpl.submitApplication(context);
    ApplicationMaster master = asmImpl.getApplicationMaster(appID);

    while (master.getState() != ApplicationState.FAILED) {
      Thread.sleep(200);
      master = asmImpl.getApplicationMaster(appID);
    }
    assertTrue(master.getState() == ApplicationState.FAILED);
  }
}
