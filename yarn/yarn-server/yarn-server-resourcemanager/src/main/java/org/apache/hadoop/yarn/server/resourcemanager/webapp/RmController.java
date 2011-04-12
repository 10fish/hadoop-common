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

import com.google.inject.Inject;

import java.util.Date;

import org.apache.hadoop.util.VersionInfo;
import org.apache.hadoop.yarn.api.records.Application;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationState;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.resourcemanager.applicationsmanager.ApplicationsManager;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.webapp.Controller;
import org.apache.hadoop.yarn.webapp.ResponseInfo;

import static org.apache.hadoop.yarn.server.resourcemanager.webapp.RMWebApp.*;
import static org.apache.hadoop.yarn.util.StringHelper.*;

// Do NOT rename/refactor this to RMView as it will wreak havoc
// on Mac OS HFS as its case-insensitive!
public class RmController extends Controller {
  @Inject RmController(RequestContext ctx) { super(ctx); }

  @Override public void index() {
    setTitle("Applications");
  }

  public void info() {
    setTitle("About the Cluster");
    long ts = ResourceManager.clusterTimeStamp;
    ResourceManager rm = injector().getInstance(ResourceManager.class);
    info("Cluster overview").
      _("Cluster ID:", ts).
      _("ResourceManager state:", rm.getServiceState()).
      _("ResourceManager started on:", new Date(ts)).
      _("ResourceManager version:", "FIXAPI: 1.0-SNAPSHOT").
      _("Hadoop version:", VersionInfo.getBuildVersion());
    render(InfoPage.class);
  }

  public void app() {
    String aid = $(APP_ID);
    if (aid.isEmpty()) {
      setStatus(response().SC_BAD_REQUEST);
      setTitle("Bad request: requires application ID");
      return;
    }
    ApplicationId appID = Apps.toAppID(aid);
    ApplicationsManager asm = injector().getInstance(ApplicationsManager.class);
    Application app = asm.getApplication(appID);
    if (app == null) {
      // TODO: handle redirect to jobhistory server
      setStatus(response().SC_NOT_FOUND);
      setTitle("Application not found: "+ aid);
      return;
    }
    setTitle(join("Application ", aid));
    CharSequence master = app.getMasterHost();
    String ui = master == null ? "UNASSIGNED"
                               : join(master, ':', app.getMasterPort());

    ResponseInfo info = info("Application Overview").
      _("User:", app.getUser()).
      _("Name:", app.getName()).
      _("State:", app.getState()).
      _("Started:", "FIXAPI!").
      _("Elapsed:", "FIXAPI!").
      _("Master UI:", master == null ? "#" : join("http://", ui), ui);
    if (app.getState() == ApplicationState.COMPLETED || 
        app.getState() == ApplicationState.FAILED || 
        app.getState() == ApplicationState.KILLED) {
      info._("History:", "FIXAPI!");
    }
    render(InfoPage.class);
  }

  public void nodes() {
    render(NodesPage.class);
  }

  public void scheduler() {
    ResourceManager rm = injector().getInstance(ResourceManager.class);
    ResourceScheduler rs = rm.getResourceScheduler();
    if (rs == null || rs instanceof CapacityScheduler) {
      setTitle("Capacity Scheduler");
      render(CapacitySchedulerPage.class);
      return;
    }
    setTitle("Default Scheduler");
    render(DefaultSchedulerPage.class);
  }

  public void queue() {
    setTitle(join("Queue ", get(QUEUE_NAME, "unknown")));
  }

  public void submit() {
    setTitle("Application Submission Not Allowed");
  }

  public void json() {
    renderJSON(AppsList.class);
  }
}
