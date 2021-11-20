package com.jessin.practice.dubbo.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * @Author: jessin
 * @Date: 19-11-26 下午10:00
 */
@Slf4j
public class CuratorZookeeperClient {

    static final Charset charset = Charset.forName("UTF-8");
    private final CuratorFramework client;
    private Map<String, TreeCache> treeCacheMap = new ConcurrentHashMap<>();

    /**
     * TODO 设置zk临时节点的有效时间sessionTimeout
     * @param zkAddress
     */
    public CuratorZookeeperClient(String zkAddress) {
        try {
            int timeout = 3000;
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString(zkAddress)
                    .retryPolicy(new RetryNTimes(1, 1000))
                    .connectionTimeoutMs(timeout).sessionTimeoutMs(5000);
            client = builder.build();
            client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState state) {
                    if (state == ConnectionState.LOST) {
                        log.info("zk断开");
                     //   CuratorZookeeperClient.this.stateChanged(StateListener.DISCONNECTED);
                    } else if (state == ConnectionState.CONNECTED) {
                        log.info("zk连接成功");

                   //     CuratorZookeeperClient.this.stateChanged(StateListener.CONNECTED);
                    } else if (state == ConnectionState.RECONNECTED) {
                        log.info("zk重新连接");
                        // TODO 重新连接时，这里需要重新注册关注的事件
                     //   CuratorZookeeperClient.this.stateChanged(StateListener.RECONNECTED);
                    }
                }
            });
            client.start();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void createPersistent(String path) {
        try {
            client.create().forPath(path);
        } catch (KeeperException.NodeExistsException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void create(String path, boolean ephemeral) {
        if (!ephemeral) {
            if (checkExists(path)) {
                return;
            }
        }
        int i = path.lastIndexOf('/');
        if (i > 0) {
            // 递归调用
            create(path.substring(0, i), false);
        }
        if (ephemeral) {
            createEphemeral(path);
        } else {
            createPersistent(path);
        }
    }

    public void createEphemeral(String path) {
        try {
            log.info("向zk注册临时节点：{}", path);
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (KeeperException.NodeExistsException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void createPersistent(String path, String data) {
        byte[] dataBytes = data.getBytes(charset);
        try {
            client.create().forPath(path, dataBytes);
        } catch (KeeperException.NodeExistsException e) {
            try {
                client.setData().forPath(path, dataBytes);
            } catch (Exception e1) {
                throw new IllegalStateException(e.getMessage(), e1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void createEphemeral(String path, String data) {
        byte[] dataBytes = data.getBytes(charset);
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path, dataBytes);
        } catch (KeeperException.NodeExistsException e) {
            try {
                client.setData().forPath(path, dataBytes);
            } catch (Exception e1) {
                throw new IllegalStateException(e.getMessage(), e1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void delete(String path) {
        try {
            client.delete().forPath(path);
        } catch (KeeperException.NoNodeException e) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public List<String> getChildren(String path) {
        try {
            return client.getChildren().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public boolean checkExists(String path) {
        try {
            if (client.checkExists().forPath(path) != null) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public boolean isConnected() {
        return client.getZookeeperClient().isConnected();
    }

    public String doGetContent(String path) {
        try {
            byte[] dataBytes = client.getData().forPath(path);
            return (dataBytes == null || dataBytes.length == 0) ? null : new String(dataBytes, charset);
        } catch (KeeperException.NoNodeException e) {
            // ignore NoNode Exception.
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    public void doClose() {
        client.close();
    }

    public CuratorZookeeperClient.CuratorWatcherImpl createTargetChildListener(String path, ChildListener listener) {
        return new CuratorZookeeperClient.CuratorWatcherImpl(client, listener);
    }

    public List<String> addTargetChildListener(String path, ChildListener listener) {
        return addTargetChildListener(path, createTargetChildListener(path, listener));
    }

    public List<String> addTargetChildListener(String path, CuratorWatcherImpl listener) {
        try {
            return client.getChildren().usingWatcher(listener).forPath(path);
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected CuratorZookeeperClient.CuratorWatcherImpl createTargetDataListener(String path, DataListener listener) {
        return new CuratorWatcherImpl(client, listener);
    }

    protected void addTargetDataListener(String path, CuratorZookeeperClient.CuratorWatcherImpl treeCacheListener) {
        this.addTargetDataListener(path, treeCacheListener, null);
    }

    protected void addTargetDataListener(String path, CuratorZookeeperClient.CuratorWatcherImpl treeCacheListener, Executor executor) {
        try {
            TreeCache treeCache = TreeCache.newBuilder(client, path).setCacheData(false).build();
            treeCacheMap.putIfAbsent(path, treeCache);
            treeCache.start();
            if (executor == null) {
                treeCache.getListenable().addListener(treeCacheListener);
            } else {
                treeCache.getListenable().addListener(treeCacheListener, executor);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Add treeCache listener for path:" + path, e);
        }
    }

    protected void removeTargetDataListener(String path, CuratorZookeeperClient.CuratorWatcherImpl treeCacheListener) {
        TreeCache treeCache = treeCacheMap.get(path);
        if (treeCache != null) {
            treeCache.getListenable().removeListener(treeCacheListener);
        }
        treeCacheListener.dataListener = null;
    }

    public void removeTargetChildListener(String path, CuratorWatcherImpl listener) {
        listener.unwatch();
    }

    static class CuratorWatcherImpl implements CuratorWatcher, TreeCacheListener {

        private CuratorFramework client;
        private volatile ChildListener childListener;
        private volatile DataListener dataListener;


        public CuratorWatcherImpl(CuratorFramework client, ChildListener listener) {
            this.client = client;
            this.childListener = listener;
        }

        public CuratorWatcherImpl(CuratorFramework client, DataListener dataListener) {
            this.dataListener = dataListener;
        }

        protected CuratorWatcherImpl() {
        }

        public void unwatch() {
            this.childListener = null;
        }

        /**
         * CuratorWatcher
         * @param event
         * @throws Exception
         */
        @Override
        public void process(WatchedEvent event) throws Exception {
            if (childListener != null) {
                String path = event.getPath() == null ? "" : event.getPath();
                childListener.childChanged(path,
                        // if path is null, curator using watcher will throw NullPointerException.
                        // if client connect or disconnect to server, zookeeper will queue
                        // watched event(Watcher.Event.EventType.None, .., path = null).
                        // TODO 再次添加watcher，该方法返回子路径？
                        path != null && path.length() > 0
                                ? client.getChildren().usingWatcher(this).forPath(path)
                                : Collections.<String>emptyList());
            }
        }

        /**
         * TreeCacheListener
         * @param client
         * @param event
         * @throws Exception
         */
        @Override
        public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
            if (dataListener != null) {
                TreeCacheEvent.Type type = event.getType();
                String content = null;
                String path = null;
                switch (type) {
                case NODE_ADDED:
                    path = event.getData().getPath();
                    content = new String(event.getData().getData(), charset);
                    break;
                case NODE_UPDATED:
                    path = event.getData().getPath();
                    content = new String(event.getData().getData(), charset);
                    break;
                case NODE_REMOVED:
                    path = event.getData().getPath();
                    break;
                case INITIALIZED:
                    break;
                case CONNECTION_LOST:
                    break;
                case CONNECTION_RECONNECTED:
                    break;
                case CONNECTION_SUSPENDED:
                    break;

                }
                dataListener.dataChanged(path, content, type);
            }
        }
    }

    /**
     * just for unit test
     *
     * @return
     */
    CuratorFramework getClient() {
        return client;
    }
}