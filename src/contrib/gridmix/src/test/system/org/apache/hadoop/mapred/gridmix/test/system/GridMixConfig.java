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
package org.apache.hadoop.mapred.gridmix.test.system;

import org.apache.hadoop.mapred.gridmix.Gridmix;
import org.apache.hadoop.mapred.gridmix.JobCreator;
import org.apache.hadoop.mapred.gridmix.SleepJob;

public class GridMixConfig {

  /**
   *  Gridmix original job id.
   */
  public static final String GRIDMIX_ORIGINAL_JOB_ID = Gridmix.ORIGINAL_JOB_ID;

  /**
   *  Gridmix output directory.
   */
  public static final String GRIDMIX_OUTPUT_DIR = Gridmix.GRIDMIX_OUT_DIR; 

  /**
   * Gridmix job type (LOADJOB/SLEEPJOB).
   */
  public static final String GRIDMIX_JOB_TYPE = JobCreator.GRIDMIX_JOB_TYPE;

  /**
   *  Gridmix submission use queue.
   */
  /* In Gridmix package the visibility of below mentioned 
  properties are protected and it have not visible outside 
  the package. However,it should required for system tests, 
  so it's re-defining in system tests config file.*/
  public static final String GRIDMIX_JOB_SUBMISSION_QUEUE_IN_TRACE = 
      "gridmix.job-submission.use-queue-in-trace";
  
  /**
   *  Gridmix user resolver(RoundRobinUserResolver/
   *  SubmitterUserResolver/EchoUserResolver).
   */
  public static final String GRIDMIX_USER_RESOLVER = Gridmix.GRIDMIX_USR_RSV;

  /**
   *  Gridmix queue depth.
   */
  public static final String GRIDMIX_QUEUE_DEPTH = Gridmix.GRIDMIX_QUE_DEP;

  /* In Gridmix package the visibility of below mentioned 
  property is protected and it should not available for 
  outside the package. However,it should required for 
  system tests, so it's re-defining in system tests config file.*/
  /**
   * Gridmix generate bytes per file.
   */
  public static final String GRIDMIX_BYTES_PER_FILE = 
      "gridmix.gen.bytes.per.file";
  
  /**
   *  Gridmix job submission policy(STRESS/REPLAY/SERIAL).
   */

  public static final String GRIDMIX_SUBMISSION_POLICY =
      "gridmix.job-submission.policy";

  /**
   *  Gridmix minimum file size.
   */
  public static final String GRIDMIX_MINIMUM_FILE_SIZE =
      "gridmix.min.file.size";

  /**
   * Gridmix key fraction.
   */
  public static final String GRIDMIX_KEY_FRC = 
      "gridmix.key.fraction";

  /**
   * Gridmix compression enable
   */
  public static final String GRIDMIX_COMPRESSION_ENABLE =
      "gridmix.compression-emulation.enable";
  /**
   * Gridmix distcache enable
   */
  public static final String GRIDMIX_DISTCACHE_ENABLE = 
      "gridmix.distributed-cache-emulation.enable";

  /**
   *  Gridmix logger mode.
   */
  public static final String GRIDMIX_LOG_MODE =
      "log4j.logger.org.apache.hadoop.mapred.gridmix";

  /**
   * Gridmix sleep job map task only.
   */
  public static final String GRIDMIX_SLEEPJOB_MAPTASK_ONLY = 
      SleepJob.SLEEPJOB_MAPTASK_ONLY;

  /**
   * Gridmix sleep map maximum time.
   */
  public static final String GRIDMIX_SLEEP_MAP_MAX_TIME = 
      SleepJob.GRIDMIX_SLEEP_MAX_MAP_TIME;

  /**
   * Gridmix sleep reduce maximum time.
   */
  public static final String GRIDMIX_SLEEP_REDUCE_MAX_TIME = 
      SleepJob.GRIDMIX_SLEEP_MAX_REDUCE_TIME;
}
