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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.io.DataInputByteBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.SecurityInfo;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ContainerManager;
import org.apache.hadoop.yarn.api.protocolrecords.CleanupContainerRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StopContainerRequest;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerToken;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.event.EventHandler;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.ipc.YarnRPC;
import org.apache.hadoop.yarn.security.ApplicationTokenIdentifier;
import org.apache.hadoop.yarn.security.ApplicationTokenSecretManager;
import org.apache.hadoop.yarn.security.ContainerManagerSecurityInfo;
import org.apache.hadoop.yarn.security.ContainerTokenIdentifier;
import org.apache.hadoop.yarn.security.client.ClientToAMSecretManager;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ASMEvent;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ApplicationMasterEvents.AMLauncherEventType;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.events.ApplicationMasterEvents.ApplicationEventType;

public class AMLauncher implements Runnable {

  private static final Log LOG = LogFactory.getLog(AMLauncher.class);

  private ContainerManager containerMgrProxy;

  private final AppContext master;
  private final Configuration conf;
  private final RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
  private ApplicationTokenSecretManager applicationTokenSecretManager;
  private ClientToAMSecretManager clientToAMSecretManager;
  private AMLauncherEventType event;
  
  @SuppressWarnings("rawtypes")
  private EventHandler handler;
  
  @SuppressWarnings("unchecked")
  public AMLauncher(RMContext asmContext, AppContext master,
      AMLauncherEventType event,ApplicationTokenSecretManager applicationTokenSecretManager,
      ClientToAMSecretManager clientToAMSecretManager, Configuration conf) {
    this.master = master;
    this.conf = new Configuration(conf); // Just not to touch the sec-info class
    this.applicationTokenSecretManager = applicationTokenSecretManager;
    this.clientToAMSecretManager = clientToAMSecretManager;
    this.conf.setClass(
        CommonConfigurationKeysPublic.HADOOP_SECURITY_INFO_CLASS_NAME,
        ContainerManagerSecurityInfo.class, SecurityInfo.class);
    this.event = event;
    this.handler = asmContext.getDispatcher().getEventHandler();
  }
  
  private void connect() throws IOException {
    ContainerId masterContainerID = master.getMasterContainer().getId();
    
    containerMgrProxy =
        getContainerMgrProxy(masterContainerID.getAppId());
  }
  
  private void launch() throws IOException {
    connect();
    ContainerId masterContainerID = master.getMasterContainer().getId();
    ApplicationSubmissionContext applicationContext =
      master.getSubmissionContext();
    LOG.info("Setting up container " + master.getMasterContainer() 
        + " for AM " + master.getMaster());  
    ContainerLaunchContext launchContext =
        getLaunchSpec(applicationContext, masterContainerID);
    StartContainerRequest request = recordFactory.newRecordInstance(StartContainerRequest.class);
    request.setContainerLaunchContext(launchContext);
    containerMgrProxy.startContainer(request);
    LOG.info("Done launching container " + master.getMasterContainer() 
        + " for AM " + master.getMaster());
  }
  
  private void cleanup() throws IOException {
    connect();
    ContainerId containerId = master.getMasterContainer().getId();
    StopContainerRequest stopRequest = recordFactory.newRecordInstance(StopContainerRequest.class);
    stopRequest.setContainerId(containerId);
    containerMgrProxy.stopContainer(stopRequest);
    
    CleanupContainerRequest cleanupRequest = recordFactory.newRecordInstance(CleanupContainerRequest.class);
    cleanupRequest.setContainerId(containerId);
    containerMgrProxy.cleanupContainer(cleanupRequest);
  }

  private ContainerManager getContainerMgrProxy(
      final ApplicationId applicationID) throws IOException {

    Container container = master.getMasterContainer();

    final String containerManagerBindAddress = container.getContainerManagerAddress();

    final YarnRPC rpc = YarnRPC.create(conf); // TODO: Don't create again and again.

    UserGroupInformation currentUser =
        UserGroupInformation.createRemoteUser("TODO"); // TODO
    if (UserGroupInformation.isSecurityEnabled()) {
      ContainerToken containerToken = container.getContainerToken();
      Token<ContainerTokenIdentifier> token =
          new Token<ContainerTokenIdentifier>(
              containerToken.getIdentifier().array(),
              containerToken.getPassword().array(), new Text(
                  containerToken.getKind()), new Text(
                  containerToken.getService()));
      currentUser.addToken(token);
    }
    return currentUser.doAs(new PrivilegedAction<ContainerManager>() {
      @Override
      public ContainerManager run() {
        return (ContainerManager) rpc.getProxy(ContainerManager.class,
            NetUtils.createSocketAddr(containerManagerBindAddress), conf);
      }
    });
  }

  private ContainerLaunchContext getLaunchSpec(
      ApplicationSubmissionContext applicationMasterContext,
      ContainerId containerID) throws IOException {

    // Construct the actual Container
    ContainerLaunchContext container = recordFactory.newRecordInstance(ContainerLaunchContext.class);
    container.addAllCommands(applicationMasterContext.getCommandList());
    StringBuilder mergedCommand = new StringBuilder();
    String failCount = Integer.toString(master.getFailedCount());
    List<String> commandList = new ArrayList<String>();
    for (String str : container.getCommandList()) {
      String result = str.replaceFirst(YarnConfiguration.AM_FAIL_COUNT_STRING, failCount);
      mergedCommand.append(result).append(" ");
      commandList.add(result);
    }
    container.clearCommands();
    container.addAllCommands(commandList);
    /** add the failed count to the app master command line */
   
    LOG.info("Command to launch container " + 
        containerID + " : " + mergedCommand);
    container.addAllEnv(applicationMasterContext.getAllEnvironment());

    container.addAllEnv(setupTokensInEnv(applicationMasterContext));

    // Construct the actual Container
    container.setContainerId(containerID);
    container.setUser(applicationMasterContext.getUser());
    container.setResource(applicationMasterContext.getMasterCapability());
    container.addAllLocalResources(applicationMasterContext.getAllResourcesTodo());
    container.setContainerTokens(applicationMasterContext.getFsTokensTodo());
    return container;
  }

  private Map<String, String> setupTokensInEnv(
      ApplicationSubmissionContext asc)
      throws IOException {
    Map<String, String> env =
      new HashMap<String, String>();
    if (UserGroupInformation.isSecurityEnabled()) {
      // TODO: Security enabled/disabled info should come from RM.

      Credentials credentials = new Credentials();

      DataInputByteBuffer dibb = new DataInputByteBuffer();
      if (asc.getFsTokensTodo() != null) {
        // TODO: Don't do this kind of checks everywhere.
        dibb.reset(asc.getFsTokensTodo());
        credentials.readTokenStorageStream(dibb);
      }

      ApplicationTokenIdentifier id =
          new ApplicationTokenIdentifier(master.getMasterContainer().getId().getAppId());
      Token<ApplicationTokenIdentifier> token =
          new Token<ApplicationTokenIdentifier>(id,
              this.applicationTokenSecretManager);
      String schedulerAddressStr =
          this.conf.get(YarnConfiguration.SCHEDULER_ADDRESS,
              YarnConfiguration.DEFAULT_SCHEDULER_BIND_ADDRESS);
      InetSocketAddress unresolvedAddr =
          NetUtils.createSocketAddr(schedulerAddressStr);
      String resolvedAddr =
          unresolvedAddr.getAddress().getHostAddress() + ":"
              + unresolvedAddr.getPort();
      token.setService(new Text(resolvedAddr));
      String appMasterTokenEncoded = token.encodeToUrlString();
      LOG.debug("Putting appMaster token in env : " + appMasterTokenEncoded);
      env.put(YarnConfiguration.APPLICATION_MASTER_TOKEN_ENV_NAME,
          appMasterTokenEncoded);

      // Add the RM token
      credentials.addToken(new Text(resolvedAddr), token);
      DataOutputBuffer dob = new DataOutputBuffer();
      credentials.writeTokenStorageToStream(dob);
      asc.setFsTokensTodo(ByteBuffer.wrap(dob.getData(), 0, dob.getLength()));

      ApplicationTokenIdentifier identifier =
          new ApplicationTokenIdentifier(
              this.master.getMaster().getApplicationId());
      SecretKey clientSecretKey =
          this.clientToAMSecretManager.getMasterKey(identifier);
      String encoded =
          Base64.encodeBase64URLSafeString(clientSecretKey.getEncoded());
      LOG.debug("The encoded client secret-key to be put in env : " + encoded);
      env.put(YarnConfiguration.APPLICATION_CLIENT_SECRET_ENV_NAME, encoded);
    }
    return env;
  }
  
  @SuppressWarnings("unchecked")
  public void run() {
    switch (event) {
    case LAUNCH:
      ApplicationEventType eventType = ApplicationEventType.LAUNCHED;
      try {
        LOG.info("Launching master" + master.getMaster());
        launch();
      } catch(Exception ie) {
        LOG.info("Error launching ", ie);
        eventType = ApplicationEventType.LAUNCH_FAILED;
      }
      handler.handle(new ASMEvent<ApplicationEventType>(eventType,  master));
      break;
    case CLEANUP:
      try {
        LOG.info("Cleaning master " + master.getMaster());
        cleanup();
      } catch(IOException ie) {
        LOG.info("Error cleaning master ", ie);
      }
      handler.handle(new ASMEvent<ApplicationEventType>(ApplicationEventType.FINISH, master));
      break;
    default:
      break;
    }
  }

  public AppContext getApplicationContext() {
   return master;
  }
}
