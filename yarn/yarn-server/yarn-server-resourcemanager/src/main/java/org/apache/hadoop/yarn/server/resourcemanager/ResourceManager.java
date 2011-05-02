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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.avro.AvroRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.security.ApplicationTokenSecretManager;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.ApplicationsManager;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.ApplicationsManagerImpl;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.
  ApplicationMasterEvents.ApplicationTrackerEventType;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.ApplicationsStore;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.NodeStore;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.Recoverable;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.Store;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.Store.RMState;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.StoreFactory;
import org.apache.hadoop.yarn.server.resourcemanager.resourcetracker.RMResourceTrackerImpl;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.RMWebApp;
import org.apache.hadoop.yarn.server.security.ContainerTokenSecretManager;
import org.apache.hadoop.yarn.service.CompositeService;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebApps;

/**
 * The ResourceManager is the main class that is a set of components.
 *
 */
public class ResourceManager extends CompositeService implements Recoverable {
  private static final Log LOG = LogFactory.getLog(ResourceManager.class);
  public static final long clusterTimeStamp = System.currentTimeMillis();
  private YarnConfiguration conf;
  
  private ApplicationsManagerImpl applicationsManager;
  
  private ContainerTokenSecretManager containerTokenSecretManager =
      new ContainerTokenSecretManager();

  private ApplicationTokenSecretManager appTokenSecretManager =
      new ApplicationTokenSecretManager();

  
  private ResourceScheduler scheduler;
  private RMResourceTrackerImpl rmResourceTracker;
  private ClientRMService clientRM;
  private ApplicationMasterService masterService;
  private AdminService adminService;
  private AtomicBoolean shutdown = new AtomicBoolean(false);
  private WebApp webApp;
  private RMContext rmContext;
  private final Store store;
  
  public ResourceManager(Store store) {
    super("ResourceManager");
    this.store = store;
  }
  
  public RMContext getRMContext() {
    return this.rmContext;
  }
  
  public interface RMContext {
    public RMDispatcherImpl getDispatcher();
    public NodeStore getNodeStore();
    public ApplicationsStore getApplicationsStore();
  }
  
  public static class RMContextImpl implements RMContext {
    private final RMDispatcherImpl asmEventDispatcher;
    private final Store store;
    
    public RMContextImpl(Store store) {
      this.asmEventDispatcher = new RMDispatcherImpl();
      this.store = store;
    }
    
    @Override
    public RMDispatcherImpl getDispatcher() {
      return this.asmEventDispatcher;
    }

    @Override
    public NodeStore getNodeStore() {
     return store;
    }

    @Override
    public ApplicationsStore getApplicationsStore() {
      return store;
    }
  }
  
  
  @Override
  public synchronized void init(Configuration conf) {
    
    this.rmContext = new RMContextImpl(this.store);
    addService(rmContext.getDispatcher());
    // Initialize the config
    this.conf = new YarnConfiguration(conf);
    // Initialize the scheduler
    this.scheduler = 
      ReflectionUtils.newInstance(
          conf.getClass(YarnConfiguration.RESOURCE_SCHEDULER, 
              FifoScheduler.class, ResourceScheduler.class), 
          this.conf);
  
    this.rmContext.getDispatcher().register(ApplicationTrackerEventType.class, scheduler);
    //TODO change this to be random
    this.appTokenSecretManager.setMasterKey(ApplicationTokenSecretManager
        .createSecretKey("Dummy".getBytes()));

    applicationsManager = createApplicationsManagerImpl();
    addService(applicationsManager);
    
    rmResourceTracker = createRMResourceTracker();
    addService(rmResourceTracker);
  
    try {
      this.scheduler.reinitialize(this.conf, this.containerTokenSecretManager, rmResourceTracker);
    } catch (IOException ioe) {
      throw new RuntimeException("Failed to initialize scheduler", ioe);
    }
    
    clientRM = createClientRMService();
    addService(clientRM);
    
    masterService = createApplicationMasterService();
    addService(masterService) ;
    
    adminService = createAdminService(conf, scheduler);
    addService(adminService);

    super.init(conf);
  }
  
  @Override
  public void start() { 
    try {
      doSecureLogin();
    } catch(IOException ie) {
      throw new AvroRuntimeException("Failed to login", ie);
    }

    webApp = WebApps.$for("yarn", masterService).at(
      conf.get(YarnConfiguration.RM_WEBAPP_BIND_ADDRESS,
      YarnConfiguration.DEFAULT_RM_WEBAPP_BIND_ADDRESS)).
    start(new RMWebApp(this));

    super.start();

    synchronized(shutdown) {
      try {
        while(!shutdown.get()) {
          shutdown.wait();
        }
      } catch(InterruptedException ie) {
        LOG.info("Interrupted while waiting", ie);
      }
    }
  }
  
  protected void doSecureLogin() throws IOException {
    SecurityUtil.login(conf, RMConfig.RM_KEYTAB,
        YarnConfiguration.RM_SERVER_PRINCIPAL_KEY);
  }

  @Override
  public void stop() {
    if (webApp != null) {
      webApp.stop();
    }
  
    synchronized(shutdown) {
      shutdown.set(true);
      shutdown.notifyAll();
    }
    super.stop();
  }
  
  protected RMResourceTrackerImpl createRMResourceTracker() {
    return new RMResourceTrackerImpl(this.containerTokenSecretManager, this.rmContext);
  }
  
  protected ApplicationsManagerImpl createApplicationsManagerImpl() {
    return new ApplicationsManagerImpl(
        this.appTokenSecretManager, this.scheduler, this.rmContext);
  }

  protected ClientRMService createClientRMService() {
    return new ClientRMService(applicationsManager, rmResourceTracker, scheduler);
  }

  protected ApplicationMasterService createApplicationMasterService() {
    return new ApplicationMasterService(
      this.appTokenSecretManager, applicationsManager, scheduler, this.rmContext);
  }
  

  protected AdminService createAdminService(Configuration conf, 
      ResourceScheduler scheduler) {
    return new AdminService(conf, scheduler);
  }

  /**
   * return applications manager.
   * @return
   */
  public ApplicationsManager getApplicationsManager() {
    return applicationsManager;
  }
  
  /**
   * return the scheduler.
   * @return
   */
  public ResourceScheduler getResourceScheduler() {
    return this.scheduler;
  }
  
  /**
   * return the resource tracking component.
   * @return
   */
  public RMResourceTrackerImpl getResourceTracker() {
    return this.rmResourceTracker;
  }
  

  @Override
  public void recover(RMState state) throws Exception {
    applicationsManager.recover(state);
    rmResourceTracker.recover(state);
    scheduler.recover(state);
  }
  
  public static void main(String argv[]) {
    ResourceManager resourceManager = null;
    try {
      Configuration conf = new YarnConfiguration();
      Store store =  StoreFactory.getStore(conf);
      resourceManager = new ResourceManager(store);
      resourceManager.init(conf);
      //resourceManager.recover(store.restore());
      //store.doneWithRecovery();
      resourceManager.start();
    } catch (Throwable e) {
      LOG.error("Error starting RM", e);
    } finally {
      if (resourceManager != null) {
        resourceManager.stop();
      }
    }
  }
}
