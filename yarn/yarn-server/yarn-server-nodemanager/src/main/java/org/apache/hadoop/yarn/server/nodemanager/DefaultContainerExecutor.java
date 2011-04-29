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

package org.apache.hadoop.yarn.server.nodemanager;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.UnsupportedFileSystemException;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Shell.ExitCodeException;
import org.apache.hadoop.util.Shell.ShellCommandExecutor;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.Container;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.launcher.ContainerLaunch;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.localizer.ContainerLocalizer;
import org.apache.hadoop.yarn.util.ConverterUtils;

public class DefaultContainerExecutor extends ContainerExecutor {

  private static final Log LOG = LogFactory
      .getLog(DefaultContainerExecutor.class);

  private final FileContext lfs;

  public DefaultContainerExecutor() {
    try {
      this.lfs = FileContext.getLocalFSFileContext();
    } catch (UnsupportedFileSystemException e) {
      throw new RuntimeException(e);
    }
  }

  DefaultContainerExecutor(FileContext lfs) {
    this.lfs = lfs;
  }

  @Override
  public void startLocalizer(Path nmLocal, InetSocketAddress nmAddr,
      String user, String appId, String locId, Path logDir,
      List<Path> localDirs) throws IOException, InterruptedException {

    ContainerLocalizer localizer =
        new ContainerLocalizer(user, appId, locId, logDir, localDirs);

    createUserLocalDirs(localDirs, user);
    createUserCacheDirs(localDirs, user);
    createAppDirs(localDirs, user, appId);
    createAppLogDir(logDir, appId);

    Path appStorageDir = getApplicationDir(localDirs, user, appId);

    String tokenFn = String.format(ContainerLocalizer.TOKEN_FILE_FMT, locId);
    Path appTokens = new Path(nmLocal, tokenFn);
    Path tokenDst = new Path(appStorageDir, tokenFn);
    lfs.util().copy(appTokens, tokenDst);
    lfs.setWorkingDirectory(appStorageDir);

    // TODO: DO it over RPC for maintaining similarity?
    localizer.runLocalization(nmAddr);
  }

  @Override
  public int launchContainer(Container container, Path nmLocal,
      String user, String appId, List<Path> appDirs, String stdout,
      String stderr) throws IOException {
    // create container dirs
    for (Path p : appDirs) {
      lfs.mkdir(new Path(p,
                ConverterUtils.toString(container.getContainerID())),
                null, true);
    }
    // copy launch script to work dir
    Path appWorkDir = new Path(appDirs.get(0), container.toString());
    Path launchScript = new Path(nmLocal, ContainerLaunch.CONTAINER_SCRIPT);
    Path launchDst = new Path(appWorkDir, ContainerLaunch.CONTAINER_SCRIPT);
    lfs.util().copy(launchScript, launchDst);
    // copy container tokens to work dir
    String tokenFn = String.format(
        ContainerLocalizer.TOKEN_FILE_FMT,
        ConverterUtils.toString(container.getContainerID()));
    Path appTokens = new Path(nmLocal, tokenFn);
    Path tokenDst = new Path(appWorkDir, tokenFn);
    lfs.util().copy(appTokens, tokenDst);
    // create log dir under app
    // fork script
    ShellCommandExecutor shExec = null;
    try {
      lfs.setPermission(launchDst,
          ContainerExecutor.TASK_LAUNCH_SCRIPT_PERMISSION);
      String[] command = 
          new String[] { "bash", "-c", launchDst.toUri().getPath().toString() };
      shExec = new ShellCommandExecutor(command,
          new File(appWorkDir.toUri().getPath()));
      launchCommandObjs.put(container.getLaunchContext().getContainerId(), shExec);
      shExec.execute();
    } catch (Exception e) {
      if (null == shExec) {
        return -1;
      }
      int exitCode = shExec.getExitCode();
      LOG.warn("Exit code from task is : " + exitCode);
      logOutput(shExec.getOutput());
      return exitCode;
    } finally {
      launchCommandObjs.remove(container.getLaunchContext().getContainerId());
    }
    return 0;
  }

  @Override
  public boolean signalContainer(String user, String pid, Signal signal)
      throws IOException {
    final String sigpid = ContainerExecutor.isSetsidAvailable
        ? "-" + pid
        : pid;
    try {
      sendSignal(sigpid, Signal.NULL);
    } catch (ExitCodeException e) {
      return false;
    }
    try {
      sendSignal(sigpid, signal);
    } catch (IOException e) {
      try {
        sendSignal(sigpid, Signal.NULL);
      } catch (IOException ignore) {
        return false;
      }
      throw e;
    }
    return true;
  }

  /**
   * Send a specified signal to the specified pid
   *
   * @param pid the pid of the process [group] to signal.
   * @param signal signal to send
   * (for logging).
   */
  protected void sendSignal(String pid, Signal signal) throws IOException {
    ShellCommandExecutor shexec = null;
      String[] arg = { "kill", "-" + signal.getValue(), pid };
      shexec = new ShellCommandExecutor(arg);
    shexec.execute();
  }

  @Override
  public void deleteAsUser(String user, Path subDir, Path... baseDirs)
      throws IOException, InterruptedException {
    if (baseDirs == null || baseDirs.length == 0) {
      LOG.info("Deleting absolute path : " + subDir);
      lfs.delete(subDir, true);
      return;
    }
    for (Path baseDir : baseDirs) {
      Path del = new Path(baseDir, subDir);
      LOG.info("Deleting path : " + del);
      lfs.delete(del, true);
    }
  }

  /** Permissions for user dir.
   * $loaal.dir/usercache/$user */
  private static final short USER_PERM = (short)0750;
  /** Permissions for user appcache dir.
   * $loaal.dir/usercache/$user/appcache */
  private static final short APPCACHE_PERM = (short)0710;
  /** Permissions for user filecache dir.
   * $loaal.dir/usercache/$user/filecache */
  private static final short FILECACHE_PERM = (short)0710;
  /** Permissions for user app dir.
   * $loaal.dir/usercache/$user/filecache */
  private static final short APPDIR_PERM = (short)0710;
  /** Permissions for user log dir.
   * $logdir/$user/$appId */
  private static final short LOGDIR_PERM = (short)0710;

  private Path getApplicationDir(List<Path> localDirs, String user,
      String appId) {
    return getApplicationDir(localDirs.get(0), user, appId);
  }

  private Path getApplicationDir(Path base, String user, String appId) {
    return new Path(getAppcacheDir(base, user), appId);
  }

  private Path getUserCacheDir(Path base, String user) {
    return new Path(new Path(base, ContainerLocalizer.USERCACHE), user);
  }

  private Path getAppcacheDir(Path base, String user) {
    return new Path(getUserCacheDir(base, user),
        ContainerLocalizer.APPCACHE);
  }

  private Path getFileCacheDir(Path base, String user) {
    return new Path(getUserCacheDir(base, user),
        ContainerLocalizer.FILECACHE);
  }

  /**
   * Initialize the local directories for a particular user.
   * <ul>
   * <li>$local.dir/usercache/$user</li>
   * </ul>
   */
  private void createUserLocalDirs(List<Path> localDirs, String user)
      throws IOException {
    boolean userDirStatus = false;
    FsPermission userperms = new FsPermission(USER_PERM);
    for (Path localDir : localDirs) {
      // create $local.dir/usercache/$user
      try {
        lfs.mkdir(getUserCacheDir(localDir, user), userperms, true);
      } catch (IOException e) {
        LOG.warn("Unable to create the user directory : " + localDir, e);
        continue;
      }
      userDirStatus = true;
    }
    if (!userDirStatus) {
      throw new IOException("Not able to initialize user directories "
          + "in any of the configured local directories for user " + user);
    }
  }


  /**
   * Initialize the local cache directories for a particular user.
   * <ul>
   * <li>$local.dir/usercache/$user</li>
   * <li>$local.dir/usercache/$user/appcache</li>
   * <li>$local.dir/usercache/$user/filecache</li>
   * </ul>
   */
  private void createUserCacheDirs(List<Path> localDirs, String user)
      throws IOException {
    LOG.info("Initializing user " + user);

    boolean appcacheDirStatus = false;
    boolean distributedCacheDirStatus = false;
    FsPermission appCachePerms = new FsPermission(APPCACHE_PERM);
    FsPermission fileperms = new FsPermission(FILECACHE_PERM);

    for (Path localDir : localDirs) {
      // create $local.dir/usercache/$user/appcache
      final Path appDir = getAppcacheDir(localDir, user);
      try {
        lfs.mkdir(appDir, appCachePerms, true);
        appcacheDirStatus = true;
      } catch (IOException e) {
        LOG.warn("Unable to create app cache directory : " + appDir, e);
      }
      // create $local.dir/usercache/$user/filecache
      final Path distDir = getFileCacheDir(localDir, user);
      try {
        lfs.mkdir(distDir, fileperms, true);
        distributedCacheDirStatus = true;
      } catch (IOException e) {
        LOG.warn("Unable to create file cache directory : " + distDir, e);
      }
    }
    if (!appcacheDirStatus) {
      throw new IOException("Not able to initialize app-cache directories "
          + "in any of the configured local directories for user " + user);
    }
    if (!distributedCacheDirStatus) {
      throw new IOException(
          "Not able to initialize distributed-cache directories "
              + "in any of the configured local directories for user "
              + user);
    }
  }

  /**
   * Initialize the local directories for a particular user.
   * <ul>
   * <li>$local.dir/usercache/$user/appcache/$appid</li>
   * </ul>
   * @param localDirs 
   */
  private void createAppDirs(List<Path> localDirs, String user, String appId)
      throws IOException {
    boolean initAppDirStatus = false;
    FsPermission appperms = new FsPermission(APPDIR_PERM);
    for (Path localDir : localDirs) {
      Path fullAppDir = getApplicationDir(localDir, user, appId);
      if (lfs.util().exists(fullAppDir)) {
        // this will happen on a partial execution of localizeJob. Sometimes
        // copying job.xml to the local disk succeeds but copying job.jar might
        // throw out an exception. We should clean up and then try again.
        lfs.delete(fullAppDir, true);
      }
      // create $local.dir/usercache/$user/appcache/$appId
      try {
        lfs.mkdir(fullAppDir, appperms, true);
        initAppDirStatus = true;
      } catch (IOException e) {
        LOG.warn("Unable to create app directory " + fullAppDir.toString(), e);
      }
    }
    if (!initAppDirStatus) {
      throw new IOException("Not able to initialize app directories "
          + "in any of the configured local directories for app "
          + appId.toString());
    }
    // pick random work dir for compatibility
    // create $local.dir/usercache/$user/appcache/$appId/work
    Path workDir =
        new Path(getApplicationDir(localDirs, user, appId),
            ContainerLocalizer.WORKDIR);
    lfs.mkdir(workDir, null, true);
  }

  /**
   * Create application log directory.
   */
  private void createAppLogDir(Path logDir, String appId)
      throws IOException {
    Path appUserLogDir = new Path(logDir, appId);
    try {
      lfs.mkdir(appUserLogDir, new FsPermission(LOGDIR_PERM), true);
    } catch (IOException e) {
      throw new IOException(
          "Could not create app user log directory: " + appUserLogDir, e);
    }
  }

}
