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

package org.apache.hadoop.yarn.server.resourcemanager.webapp;

import java.util.Date;

import com.google.inject.Inject;

import org.apache.hadoop.yarn.server.resourcemanager.resourcetracker.NodeInfo;
import org.apache.hadoop.yarn.server.resourcemanager.resourcetracker.ResourceContext;
import org.apache.hadoop.yarn.webapp.SubView;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.*;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

import static org.apache.hadoop.yarn.webapp.view.JQueryUI.*;

class NodesPage extends RmView {

  static class NodesBlock extends HtmlBlock {
    final ResourceContext resource;

    @Inject
    NodesBlock(ResourceContext rc, ViewContext ctx) {
      super(ctx);
      resource = rc;
    }

    @Override
    protected void render(Block html) {
      TBODY<TABLE<Hamlet>> tbody = html.table("#nodes").
          thead().
          tr().
          th(".rack", "Rack").
          th(".nodeid", "Node ID").
          th(".host", "Host").
          th(".healthStatus", "Health-status").
          th(".lastHealthUpdate", "Last health-update").
          th(".healthReport", "Health-report").
          th(".containers", "Containers").
          th(".mem", "Mem Used (MB)").
          th(".mem", "Mem Avail (MB)")._()._().
          tbody();
      for (NodeInfo ni : resource.getAllNodeInfo()) {
        tbody.tr().
            td(ni.getRackName()).
            td(String.valueOf(ni.getNodeID().getId())).
            td().a("http://" + ni.getHttpAddress(), ni.getHttpAddress())._().
            td(ni.getNodeHealthStatus().getIsNodeHealthy() ? "Healthy"
                : "Unhealthy").
            td(new Date(ni.getNodeHealthStatus()
                .getLastHealthReportTime()).toString()).
            td(String.valueOf(ni.getNodeHealthStatus().getHealthReport())).
            td(String.valueOf(ni.getNumContainers())).
            td(String.valueOf(ni.getUsedResource().getMemory())).
            td(String.valueOf(ni.getAvailableResource().getMemory()))._();
      }
      tbody._()._();
    }
  }

  @Override protected void preHead(Page.HTML<_> html) {
    commonPreHead(html);
    setTitle("Nodes of the cluster");
    set(DATATABLES_ID, "nodes");
    set(initID(DATATABLES, "nodes"), nodesTableInit());
    setTableStyles(html, "nodes");
  }

  @Override protected Class<? extends SubView> content() {
    return NodesBlock.class;
  }

  private String nodesTableInit() {
    return tableInit().
        // rack, nodeid, host, healthStatus, containers, memused, memavail
        append(", aoColumns:[null, null, null, null, {bSearchable:false}, ").
        append("{bSearchable:false}, {bSearchable:false}]}").toString();
  }
}
