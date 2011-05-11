package org.apache.hadoop.yarn.server.resourcemanager.scheduler;

import org.apache.hadoop.metrics2.MetricsSource;
import org.apache.hadoop.metrics2.impl.MetricsSystemImpl;
import org.apache.hadoop.metrics2.MetricsSystem;
import org.apache.hadoop.metrics2.MetricsRecordBuilder;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import static org.apache.hadoop.test.MetricsAsserts.*;
import static org.apache.hadoop.test.MockitoMaker.*;
import org.apache.hadoop.yarn.api.records.ApplicationState;
import org.apache.hadoop.yarn.server.resourcemanager.resource.Resources;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestQueueMetrics {
  static final int GB = 1024; // MB

  MetricsSystem ms;

  @Before public void setup() {
    DefaultMetricsSystem.shutdown(); // not necessary after HADOOP-6919
    ms = new MetricsSystemImpl();
  }

  @Test public void testDefaultSingleQueueMetrics() {
    String queueName = "single";
    String user = "alice";

    QueueMetrics metrics = QueueMetrics.forQueue(ms, queueName, null, false);
    MetricsSource queueSource= queueSource(ms, queueName);
    Application app = mockApp(user);

    metrics.submitApp(user);
    MetricsSource userSource = userSource(ms, queueName, user);
    checkApps(queueSource, 1, 1, 0, 0, 0, 0);

    metrics.setAvailableQueueMemory(100*GB);
    metrics.incrPendingResources(user, 5, Resources.createResource(15*GB));
    // Available resources is set externally, as it depends on dynamic
    // configurable cluster/queue resources
    checkResources(queueSource, 0, 0, 100, 15, 5);

    metrics.incrAppsRunning(user);
    checkApps(queueSource, 1, 0, 1, 0, 0, 0);

    metrics.allocateResources(user, 3, Resources.createResource(2*GB));
    checkResources(queueSource, 6, 3, 100, 9, 2);

    metrics.releaseResources(user, 1, Resources.createResource(2*GB));
    checkResources(queueSource, 4, 2, 100, 9, 2);

    metrics.finishApp(app);
    checkApps(queueSource, 1, 0, 0, 1, 0, 0);
    assertNull(userSource);
  }

  @Test public void testSingleQueueWithUserMetrics() {
    String queueName = "single2";
    String user = "dodo";

    QueueMetrics metrics = QueueMetrics.forQueue(ms, queueName, null, true);
    MetricsSource queueSource = queueSource(ms, queueName);
    Application app = mockApp(user);

    metrics.submitApp(user);
    MetricsSource userSource = userSource(ms, queueName, user);

    checkApps(queueSource, 1, 1, 0, 0, 0, 0);
    checkApps(userSource, 1, 1, 0, 0, 0, 0);

    metrics.setAvailableQueueMemory(100*GB);
    metrics.setAvailableUserMemory(user, 10*GB);
    metrics.incrPendingResources(user, 5, Resources.createResource(15*GB));
    // Available resources is set externally, as it depends on dynamic
    // configurable cluster/queue resources
    checkResources(queueSource, 0, 0, 100, 15, 5);
    checkResources(userSource, 0, 0, 10, 15, 5);

    metrics.incrAppsRunning(user);
    checkApps(queueSource, 1, 0, 1, 0, 0, 0);
    checkApps(userSource, 1, 0, 1, 0, 0, 0);

    metrics.allocateResources(user, 3, Resources.createResource(2*GB));
    checkResources(queueSource, 6, 3, 100, 9, 2);
    checkResources(userSource, 6, 3, 10, 9, 2);

    metrics.releaseResources(user, 1, Resources.createResource(2*GB));
    checkResources(queueSource, 4, 2, 100, 9, 2);
    checkResources(userSource, 4, 2, 10, 9, 2);

    metrics.finishApp(app);
    checkApps(queueSource, 1, 0, 0, 1, 0, 0);
    checkApps(userSource, 1, 0, 0, 1, 0, 0);
  }

  @Test public void testTwoLevelWithUserMetrics() {
    String parentQueueName = "root";
    String leafQueueName = "root.leaf";
    String user = "alice";

    QueueMetrics parentMetrics =
        QueueMetrics.forQueue(ms, parentQueueName, null, true);
    Queue parentQueue = make(stub(Queue.class).returning(parentMetrics).
        from.getMetrics());
    QueueMetrics metrics =
        QueueMetrics.forQueue(ms, leafQueueName, parentQueue, true);
    MetricsSource parentQueueSource = queueSource(ms, parentQueueName);
    MetricsSource queueSource = queueSource(ms, leafQueueName);
    Application app = mockApp(user);

    metrics.submitApp(user);
    MetricsSource userSource = userSource(ms, leafQueueName, user);
    MetricsSource parentUserSource = userSource(ms, parentQueueName, user);

    checkApps(queueSource, 1, 1, 0, 0, 0, 0);
    checkApps(parentQueueSource, 1, 1, 0, 0, 0, 0);
    checkApps(userSource, 1, 1, 0, 0, 0, 0);
    checkApps(parentUserSource, 1, 1, 0, 0, 0, 0);

    parentMetrics.setAvailableQueueMemory(100*GB);
    metrics.setAvailableQueueMemory(100*GB);
    parentMetrics.setAvailableUserMemory(user, 10*GB);
    metrics.setAvailableUserMemory(user, 10*GB);
    metrics.incrPendingResources(user, 5, Resources.createResource(15*GB));
    checkResources(queueSource, 0, 0, 100, 15, 5);
    checkResources(parentQueueSource, 0, 0, 100, 15, 5);
    checkResources(userSource, 0, 0, 10, 15, 5);
    checkResources(parentUserSource, 0, 0, 10, 15, 5);

    metrics.incrAppsRunning(user);
    checkApps(queueSource, 1, 0, 1, 0, 0, 0);
    checkApps(userSource, 1, 0, 1, 0, 0, 0);

    metrics.allocateResources(user, 3, Resources.createResource(2*GB));
    // Available resources is set externally, as it depends on dynamic
    // configurable cluster/queue resources
    checkResources(queueSource, 6, 3, 100, 9, 2);
    checkResources(parentQueueSource, 6, 3, 100, 9, 2);
    checkResources(userSource, 6, 3, 10, 9, 2);
    checkResources(parentUserSource, 6, 3, 10, 9, 2);

    metrics.releaseResources(user, 1, Resources.createResource(2*GB));
    checkResources(queueSource, 4, 2, 100, 9, 2);
    checkResources(parentQueueSource, 4, 2, 100, 9, 2);
    checkResources(userSource, 4, 2, 10, 9, 2);
    checkResources(parentUserSource, 4, 2, 10, 9, 2);

    metrics.finishApp(app);
    checkApps(queueSource, 1, 0, 0, 1, 0, 0);
    checkApps(parentQueueSource, 1, 0, 0, 1, 0, 0);
    checkApps(userSource, 1, 0, 0, 1, 0, 0);
    checkApps(parentUserSource, 1, 0, 0, 1, 0, 0);
  }

  public static void checkApps(MetricsSource source, int submitted, int pending,
      int running, int completed, int failed, int killed) {
    MetricsRecordBuilder rb = getMetrics(source);
    assertCounter("AppsSubmitted", submitted, rb);
    assertGauge("AppsPending", pending, rb);
    assertGauge("AppsRunning", running, rb);
    assertCounter("AppsCompleted", completed, rb);
    assertCounter("AppsFailed", failed, rb);
    assertCounter("AppsKilled", killed, rb);
  }

  public static void checkResources(MetricsSource source, int allocGB,
      int allocCtnrs, int availGB, int pendingGB, int pendingCtnrs) {
    MetricsRecordBuilder rb = getMetrics(source);
    assertGauge("AllocatedGB", allocGB, rb);
    assertGauge("AllocatedContainers", allocCtnrs, rb);
    assertGauge("AvailableGB", availGB, rb);
    assertGauge("PendingGB", pendingGB, rb);
    assertGauge("PendingContainers", pendingCtnrs, rb);
  }

  private static Application mockApp(String user) {
    Application app = mock(Application.class);
    when(app.getState()).thenReturn(ApplicationState.RUNNING);
    when(app.getUser()).thenReturn(user);
    return app;
  }

  public static MetricsSource queueSource(MetricsSystem ms, String queue) {
    MetricsSource s = ms.getSource(QueueMetrics.sourceName(queue).toString());
    return s;
  }

  public static MetricsSource userSource(MetricsSystem ms, String queue,
                                         String user) {
    MetricsSource s = ms.getSource(QueueMetrics.sourceName(queue).
        append(",user=").append(user).toString());
    return s;
  }
}
