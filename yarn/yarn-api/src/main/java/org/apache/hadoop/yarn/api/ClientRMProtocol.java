package org.apache.hadoop.yarn.api;

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
import org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationRequest;
import org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationResponse;
import org.apache.hadoop.yarn.exceptions.YarnRemoteException;

public interface ClientRMProtocol {
  public GetNewApplicationIdResponse getNewApplicationId(GetNewApplicationIdRequest request) throws YarnRemoteException;
  public GetApplicationMasterResponse getApplicationMaster(GetApplicationMasterRequest request) throws YarnRemoteException;
  public SubmitApplicationResponse submitApplication(SubmitApplicationRequest request) throws YarnRemoteException;
  public FinishApplicationResponse finishApplication(FinishApplicationRequest request) throws YarnRemoteException;
  public GetClusterMetricsResponse getClusterMetrics(GetClusterMetricsRequest request) throws YarnRemoteException;
  public GetAllApplicationsResponse getAllApplications(GetAllApplicationsRequest request) throws YarnRemoteException;
  public GetClusterNodesResponse getClusterNodes(GetClusterNodesRequest request) throws YarnRemoteException;
}
