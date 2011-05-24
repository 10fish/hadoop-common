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

package org.apache.hadoop.mapred;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.apache.avro.ipc.Server;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.v2.api.MRClientProtocol;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.FailTaskAttemptRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.FailTaskAttemptResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetCountersRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetCountersResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetDiagnosticsRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetDiagnosticsResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetJobReportRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetJobReportResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetTaskAttemptCompletionEventsRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetTaskAttemptCompletionEventsResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetTaskAttemptReportRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetTaskAttemptReportResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetTaskReportRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetTaskReportResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetTaskReportsRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.GetTaskReportsResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.KillJobRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.KillJobResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.KillTaskAttemptRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.KillTaskAttemptResponse;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.KillTaskRequest;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.KillTaskResponse;
import org.apache.hadoop.mapreduce.v2.api.records.Counters;
import org.apache.hadoop.mapreduce.v2.api.records.JobId;
import org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId;
import org.apache.hadoop.mapreduce.v2.api.records.TaskId;
import org.apache.hadoop.mapreduce.v2.api.records.TaskType;
import org.apache.hadoop.mapreduce.v2.jobhistory.JHConfig;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.YarnException;
import org.apache.hadoop.yarn.api.ClientRMProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationRequest;
import org.apache.hadoop.yarn.api.protocolrecords.FinishApplicationResponse;
import org.apache.hadoop.yarn.api.protocolrecords.GetAllApplicationsRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetAllApplicationsResponse;
import org.apache.hadoop.yarn.api.protocolrecords.GetApplicationMasterRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetApplicationMasterResponse;
import org.apache.hadoop.yarn.api.protocolrecords.GetClusterMetricsRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetClusterMetricsResponse;
import org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesResponse;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationIdRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationIdResponse;
import org.apache.hadoop.yarn.api.protocolrecords.GetQueueInfoRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetQueueInfoResponse;
import org.apache.hadoop.yarn.api.protocolrecords.GetQueueUserAclsInfoRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetQueueUserAclsInfoResponse;
import org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationRequest;
import org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationMaster;
import org.apache.hadoop.yarn.api.records.ApplicationState;
import org.apache.hadoop.yarn.api.records.ApplicationStatus;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnRemoteException;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.factory.providers.YarnRemoteExceptionFactoryProvider;
import org.apache.hadoop.yarn.ipc.YarnRPC;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.ApplicationsManager;
import org.apache.hadoop.yarn.service.AbstractService;
import org.junit.Test;

public class TestClientRedirect {

  static {
    DefaultMetricsSystem.setMiniClusterMode(true);
  }

  private static final Log LOG = LogFactory.getLog(TestClientRedirect.class);
  private static final String RMADDRESS = "0.0.0.0:8054";
  private static final RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
  
  private static final String AMHOSTADDRESS = "0.0.0.0:10020";
  private static final String HSHOSTADDRESS = "0.0.0.0:10021";
  private static final int HSPORT = 10020;
  private volatile boolean amContact = false; 
  private volatile boolean hsContact = false;
  private volatile boolean amRunning = false;

  @Test
  public void testRedirect() throws Exception {
    
    Configuration conf = new YarnConfiguration();
    conf.set(YarnConfiguration.APPSMANAGER_ADDRESS, RMADDRESS);
    conf.set(JHConfig.HS_BIND_ADDRESS, HSHOSTADDRESS);
    RMService rmService = new RMService("test");
    rmService.init(conf);
    rmService.start();
  
    AMService amService = new AMService();
    amService.init(conf);
    amService.start(conf);
    amRunning = true;

    HistoryService historyService = new HistoryService();
    historyService.init(conf);
    historyService.start(conf);
  
    LOG.info("services started");
    YARNRunner yarnRunner = new YARNRunner(conf);
    Throwable t = null;
    org.apache.hadoop.mapreduce.JobID jobID =
      new org.apache.hadoop.mapred.JobID("201103121733", 1);
    yarnRunner.getJobCounters(jobID);
    Assert.assertTrue(amContact);
    
    //bring down the AM service
    amService.stop();
    amRunning = false;
    
    yarnRunner.getJobCounters(jobID);
    Assert.assertTrue(hsContact);
    
    rmService.stop();
    historyService.stop();
  }

  class RMService extends AbstractService implements ClientRMProtocol {
    private ApplicationsManager applicationsManager;
    private String clientServiceBindAddress;
    InetSocketAddress clientBindAddress;
    private Server server;

    public RMService(String name) {
      super(name);
    }

    @Override
    public void init(Configuration conf) {
      clientServiceBindAddress = RMADDRESS;
      /*
      clientServiceBindAddress = conf.get(
          YarnConfiguration.APPSMANAGER_ADDRESS,
          YarnConfiguration.DEFAULT_APPSMANAGER_BIND_ADDRESS);
          */
      clientBindAddress = NetUtils.createSocketAddr(clientServiceBindAddress);
      super.init(conf);
    }

    @Override
    public void start() {
      // All the clients to appsManager are supposed to be authenticated via
      // Kerberos if security is enabled, so no secretManager.
      YarnRPC rpc = YarnRPC.create(getConfig());
      Configuration clientServerConf = new Configuration(getConfig());
      this.server = rpc.getServer(ClientRMProtocol.class, this,
          clientBindAddress, clientServerConf, null);
      this.server.start();
      super.start();
    }

    @Override
    public GetNewApplicationIdResponse getNewApplicationId(GetNewApplicationIdRequest request) throws YarnRemoteException {
      return null;
    }
    
    @Override
    public GetApplicationMasterResponse getApplicationMaster(GetApplicationMasterRequest request) throws YarnRemoteException {
      ApplicationId applicationId = request.getApplicationId();
      ApplicationMaster master = recordFactory.newRecordInstance(ApplicationMaster.class);
      master.setApplicationId(applicationId);
      master.setStatus(recordFactory.newRecordInstance(ApplicationStatus.class));
      master.getStatus().setApplicationId(applicationId);
      if (amRunning) {
        master.setState(ApplicationState.RUNNING);
      } else {
        master.setState(ApplicationState.COMPLETED);
      }
      String[] split = AMHOSTADDRESS.split(":");
      master.setHost(split[0]);
      master.setRpcPort(Integer.parseInt(split[1]));
      GetApplicationMasterResponse response = recordFactory.newRecordInstance(GetApplicationMasterResponse.class);
      response.setApplicationMaster(master);
      return response;
    }

    @Override
    public SubmitApplicationResponse submitApplication(SubmitApplicationRequest request) throws YarnRemoteException {
      throw YarnRemoteExceptionFactoryProvider.getYarnRemoteExceptionFactory(null).createYarnRemoteException("Test");
    }
    
    @Override
    public FinishApplicationResponse finishApplication(FinishApplicationRequest request) throws YarnRemoteException {
      return null;
    }
    
    @Override
    public GetClusterMetricsResponse getClusterMetrics(GetClusterMetricsRequest request) throws YarnRemoteException {
      return null;
    }

    @Override
    public GetAllApplicationsResponse getAllApplications(
        GetAllApplicationsRequest request) throws YarnRemoteException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public GetClusterNodesResponse getClusterNodes(
        GetClusterNodesRequest request) throws YarnRemoteException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public GetQueueInfoResponse getQueueInfo(GetQueueInfoRequest request)
        throws YarnRemoteException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public GetQueueUserAclsInfoResponse getQueueUserAcls(
        GetQueueUserAclsInfoRequest request) throws YarnRemoteException {
      // TODO Auto-generated method stub
      return null;
    }
  }

  class HistoryService extends AMService {
    public HistoryService() {
      super(HSHOSTADDRESS);
    }

    @Override
    public GetCountersResponse getCounters(GetCountersRequest request) throws YarnRemoteException {
      JobId jobId = request.getJobId();
      hsContact = true;
      Counters counters = recordFactory.newRecordInstance(Counters.class);
//      counters.groups = new HashMap<CharSequence, CounterGroup>();
      GetCountersResponse response = recordFactory.newRecordInstance(GetCountersResponse.class);
      response.setCounters(counters);
      return response;
   }
  }

  class AMService extends AbstractService 
      implements MRClientProtocol {
    private InetSocketAddress bindAddress;
    private Server server;
    private final String hostAddress;
    public AMService() {
      this(AMHOSTADDRESS);
    }
    
    public AMService(String hostAddress) {
      super("TestClientService");
      this.hostAddress = hostAddress;
    }

    public void start(Configuration conf) {
      YarnRPC rpc = YarnRPC.create(conf);
      //TODO : use fixed port ??
      InetSocketAddress address = NetUtils.createSocketAddr(hostAddress);
      InetAddress hostNameResolved = null;
      try {
        address.getAddress();
        hostNameResolved = InetAddress.getLocalHost();
      } catch (UnknownHostException e) {
        throw new YarnException(e);
      }

      server =
          rpc.getServer(MRClientProtocol.class, this, address,
              conf, null);
      server.start();
      this.bindAddress =
        NetUtils.createSocketAddr(hostNameResolved.getHostAddress()
            + ":" + server.getPort());
       super.start();
    }

    public void stop() {
      server.close();
      super.stop();
    }

    @Override
    public GetCountersResponse getCounters(GetCountersRequest request) throws YarnRemoteException {
      JobId jobID = request.getJobId();
    
      amContact = true;
      Counters counters = recordFactory.newRecordInstance(Counters.class);
//      counters.groups = new HashMap<CharSequence, CounterGroup>();
        GetCountersResponse response = recordFactory.newRecordInstance(GetCountersResponse.class);
        response.setCounters(counters);
        return response;
      }

    @Override
    public GetJobReportResponse getJobReport(GetJobReportRequest request) throws YarnRemoteException {
      JobId jobId = request.getJobId();
      return null;
    }

    @Override
    public GetTaskReportResponse getTaskReport(GetTaskReportRequest request) throws YarnRemoteException {
      TaskId taskID = request.getTaskId();
      return null;
    }


    @Override
    public GetTaskAttemptReportResponse getTaskAttemptReport(GetTaskAttemptReportRequest request) throws YarnRemoteException {
      TaskAttemptId taskAttemptID = request.getTaskAttemptId();
      return null;
    }

    @Override
    public GetTaskAttemptCompletionEventsResponse getTaskAttemptCompletionEvents(GetTaskAttemptCompletionEventsRequest request) throws YarnRemoteException {
      JobId jobId = request.getJobId();
      int fromEventId = request.getFromEventId();
      int maxEvents = request.getMaxEvents();
      return null;
    }

    @Override
    public GetTaskReportsResponse getTaskReports(GetTaskReportsRequest request) throws YarnRemoteException {
      JobId jobID = request.getJobId();
      TaskType taskType = request.getTaskType();
      return null;
    }

    @Override
    public GetDiagnosticsResponse getDiagnostics(GetDiagnosticsRequest request) throws YarnRemoteException {
      TaskAttemptId taskAttemptID = request.getTaskAttemptId();
      return null;
    }

    @Override
    public KillJobResponse killJob(KillJobRequest request) throws YarnRemoteException {
      JobId jobID = request.getJobId();
      return null;
    }

    @Override
    public KillTaskResponse killTask(KillTaskRequest request) throws YarnRemoteException {
      TaskId taskID = request.getTaskId();
      return null;
    }

    @Override
    public KillTaskAttemptResponse killTaskAttempt(KillTaskAttemptRequest request) throws YarnRemoteException {
      TaskAttemptId taskAttemptID = request.getTaskAttemptId();
      return null;
    }

    @Override
    public FailTaskAttemptResponse failTaskAttempt(FailTaskAttemptRequest request) throws YarnRemoteException {
      TaskAttemptId taskAttemptID = request.getTaskAttemptId();
      return null;
    }
  }
}
