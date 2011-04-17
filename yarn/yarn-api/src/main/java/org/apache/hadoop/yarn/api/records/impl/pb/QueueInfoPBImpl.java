package org.apache.hadoop.yarn.api.records.impl.pb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.yarn.api.records.Application;
import org.apache.hadoop.yarn.api.records.ProtoBase;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.proto.YarnProtos.ApplicationProto;
import org.apache.hadoop.yarn.proto.YarnProtos.QueueInfoProto;
import org.apache.hadoop.yarn.proto.YarnProtos.QueueInfoProtoOrBuilder;

public class QueueInfoPBImpl extends ProtoBase<QueueInfoProto> implements
    QueueInfo {

  QueueInfoProto proto = QueueInfoProto.getDefaultInstance();
  QueueInfoProto.Builder builder = null;
  boolean viaProto = false;

  List<Application> applicationsList;
  List<QueueInfo> childQueuesList;
  
  public QueueInfoPBImpl() {
    builder = QueueInfoProto.newBuilder();
  }
  
  public QueueInfoPBImpl(QueueInfoProto proto) {
    this.proto = proto;
    viaProto = true;
  }

  @Override
  public List<Application> getApplications() {
    initLocalApplicationsList();
    return this.applicationsList;
  }

  @Override
  public float getCapacity() {
    QueueInfoProtoOrBuilder p = viaProto ? proto : builder;
    return (p.hasCapacity()) ? p.getCapacity() : -1;
  }

  @Override
  public List<QueueInfo> getChildQueues() {
    initLocalChildQueuesList();
    return this.childQueuesList;
  }

  @Override
  public float getCurrentCapacity() {
    QueueInfoProtoOrBuilder p = viaProto ? proto : builder;
    return (p.hasCurrentCapacity()) ? p.getCurrentCapacity() : 0;
  }

  @Override
  public float getMaximumCapacity() {
    QueueInfoProtoOrBuilder p = viaProto ? proto : builder;
    return (p.hasMaximumCapacity()) ? p.getMaximumCapacity() : -1;
  }

  @Override
  public String getQueueName() {
    QueueInfoProtoOrBuilder p = viaProto ? proto : builder;
    return (p.hasQueueName()) ? p.getQueueName() : null;
  }

  @Override
  public void setApplications(List<Application> applications) {
    if (applications == null) {
      builder.clearApplications();
    }
    this.applicationsList = applications;
  }

  @Override
  public void setCapacity(float capacity) {
    maybeInitBuilder();
    builder.setCapacity(capacity);
  }

  @Override
  public void setChildQueues(List<QueueInfo> childQueues) {
    if (childQueues == null) {
      builder.clearChildQueues();
    }
    this.childQueuesList = childQueues;
  }

  @Override
  public void setCurrentCapacity(float currentCapacity) {
    maybeInitBuilder();
    builder.setCurrentCapacity(currentCapacity);
  }

  @Override
  public void setMaximumCapacity(float maximumCapacity) {
    maybeInitBuilder();
    builder.setMaximumCapacity(maximumCapacity);
  }

  @Override
  public void setQueueName(String queueName) {
    maybeInitBuilder();
    if (queueName == null) {
      builder.clearQueueName();
    }
    builder.setQueueName(queueName);
  }

  @Override
  public QueueInfoProto getProto() {
    mergeLocalToProto();
    proto = viaProto ? proto : builder.build();
    viaProto = true;
    return proto;
  }

  private void initLocalApplicationsList() {
    if (this.applicationsList != null) {
      return;
    }
    QueueInfoProtoOrBuilder p = viaProto ? proto : builder;
    List<ApplicationProto> list = p.getApplicationsList();
    applicationsList = new ArrayList<Application>();

    for (ApplicationProto a : list) {
      applicationsList.add(convertFromProtoFormat(a));
    }
  }

  private void addApplicationsToProto() {
    maybeInitBuilder();
    builder.clearApplications();
    if (applicationsList == null)
      return;
    Iterable<ApplicationProto> iterable = new Iterable<ApplicationProto>() {
      @Override
      public Iterator<ApplicationProto> iterator() {
        return new Iterator<ApplicationProto>() {
  
          Iterator<Application> iter = applicationsList.iterator();
  
          @Override
          public boolean hasNext() {
            return iter.hasNext();
          }
  
          @Override
          public ApplicationProto next() {
            return convertToProtoFormat(iter.next());
          }
  
          @Override
          public void remove() {
            throw new UnsupportedOperationException();
  
          }
        };
  
      }
    };
    builder.addAllApplications(iterable);
  }

  private void initLocalChildQueuesList() {
    if (this.childQueuesList != null) {
      return;
    }
    QueueInfoProtoOrBuilder p = viaProto ? proto : builder;
    List<QueueInfoProto> list = p.getChildQueuesList();
    childQueuesList = new ArrayList<QueueInfo>();

    for (QueueInfoProto a : list) {
      childQueuesList.add(convertFromProtoFormat(a));
    }
  }

  private void addChildQueuesInfoToProto() {
    maybeInitBuilder();
    builder.clearChildQueues();
    if (childQueuesList == null)
      return;
    Iterable<QueueInfoProto> iterable = new Iterable<QueueInfoProto>() {
      @Override
      public Iterator<QueueInfoProto> iterator() {
        return new Iterator<QueueInfoProto>() {
  
          Iterator<QueueInfo> iter = childQueuesList.iterator();
  
          @Override
          public boolean hasNext() {
            return iter.hasNext();
          }
  
          @Override
          public QueueInfoProto next() {
            return convertToProtoFormat(iter.next());
          }
  
          @Override
          public void remove() {
            throw new UnsupportedOperationException();
  
          }
        };
  
      }
    };
    builder.addAllChildQueues(iterable);
  }

  private void mergeLocalToBuilder() {
    if (this.childQueuesList != null) {
      addChildQueuesInfoToProto();
    }
    if (this.applicationsList != null) {
      addApplicationsToProto();
    }
  }

  private void mergeLocalToProto() {
    if (viaProto) 
      maybeInitBuilder();
    mergeLocalToBuilder();
    proto = builder.build();
    viaProto = true;
  }

  private void maybeInitBuilder() {
    if (viaProto || builder == null) {
      builder = QueueInfoProto.newBuilder(proto);
    }
    viaProto = false;
  }


  private ApplicationPBImpl convertFromProtoFormat(ApplicationProto a) {
    return new ApplicationPBImpl(a);
  }

  private ApplicationProto convertToProtoFormat(Application t) {
    return ((ApplicationPBImpl)t).getProto();
  }

  private QueueInfoPBImpl convertFromProtoFormat(QueueInfoProto a) {
    return new QueueInfoPBImpl(a);
  }
  
  private QueueInfoProto convertToProtoFormat(QueueInfo q) {
    return ((QueueInfoPBImpl)q).getProto();
  }

}
