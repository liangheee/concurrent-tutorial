package com.liangheee.thread.diy.databaseconnpool;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.LongAdder;

@Slf4j(topic = "c.TestDatabaseConnectionPool")
public class TestDatabaseConnectionPool {
    public static void main(String[] args) {
        DatabasePool databasePool = new DatabasePool(5, 2, 2000);
        for(int i = 0;i < 100;i++){
            new Thread(() -> {
                Connection connection = databasePool.borrow(60000);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("borrow {}",connection);
                try {
                    databasePool.free(connection);
                    log.debug("free {}",connection);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            },"t" + i).start();
        }
    }
}

@Slf4j(topic = "c.DatabasePool")
class DatabasePool {
    private int maximum;
    private int coreSize;
    private long maxIdle;
    private int currentSize;

    private List<Connection> connections;
    private AtomicIntegerArray states;
    private LongAdder wait;

    public DatabasePool(int maximum, int coreSize,long maxIdle) {
        this.maximum = maximum;
        this.coreSize = coreSize;
        this.maxIdle = maxIdle;
        this.currentSize = coreSize;
        wait = new LongAdder();
    }

    public void preStartConnection() {
        // balking模式
        if(connections != null && !connections.isEmpty()){
            return;
        }
        synchronized (this){
            if(connections == null || connections.isEmpty()){
                connections = new CopyOnWriteArrayList<>();
                states = new AtomicIntegerArray(coreSize);
                for(int i = 0;i < coreSize;i++){
                    connections.add(new WeakConnection("连接" + (i + 1)));
                    states.set(i,0);
                }
            }
        }
    }

    public Connection borrow(long timeout){
        if(connections == null || connections.isEmpty()){
            preStartConnection();
        }

        // 连接扩容
        incrementConnections();

        long begin = System.currentTimeMillis();
        while(true){
             long waitTime = timeout - (System.currentTimeMillis() - begin);

             if(waitTime <= 0){
                 throw new RuntimeException("获取线程池超时");
             }

            for(int i = 0;i < currentSize;i++){
                if (states.compareAndSet(i,0,1)) {
                    wait.decrement();
                    return connections.get(i);
                }
            }

            synchronized (this){
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 扩容
     */
    private void incrementConnections(){
        // balking模式
        if(currentSize >= maximum){
            return;
        }
        synchronized (this){
            wait.increment();
            if(wait.sum() >= maximum && currentSize < maximum){
                log.debug("扩容数据库连接池中...");
                AtomicIntegerArray st = new AtomicIntegerArray(maximum);
                for(int i = 0;i < states.length();i++){
                    st.set(i,states.get(i));
                }
                states = st;
                for(int i = currentSize;i < maximum;i++){
                    connections.add(new WeakConnection("连接" + (i + 1)));
                    states.set(i,0);
                }
                currentSize = maximum;
                log.debug("扩容数据库连接池结束");
                this.notifyAll();
            }
        }
    }

    /**
     * 判断当前线程是否空闲超过了空闲等待时间，如果超过则缩容
     * 缩容后要考虑states和连接数组的对应关系
     */
    private void decrementConnections(){
        synchronized (this){
            // TODO 缩容操作
        }
    }

    public void free(Connection connection) throws SQLException {
        if(connection != null && !connection.isClosed()){
            for(int i = 0;i < connections.size();i++){
                if (connections.get(i) == connection) {
                    states.set(i,0);
                    synchronized (this){
                        this.notifyAll();
                    }
                }
            }
        }
    }
}

@Slf4j(topic = "c.WeakConnection")
class WeakConnection implements Connection {

    private String name;

    public WeakConnection(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "WeakConnection{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public Statement createStatement() throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return null;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return "";
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {

    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return false;
    }

    @Override
    public void commit() throws SQLException {

    }

    @Override
    public void rollback() throws SQLException {

    }

    @Override
    public void close() throws SQLException {

    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {

    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {

    }

    @Override
    public String getCatalog() throws SQLException {
        return "";
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {

    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return 0;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return Collections.emptyMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {

    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return false;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {

    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return "";
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {

    }

    @Override
    public String getSchema() throws SQLException {
        return "";
    }

    @Override
    public void abort(Executor executor) throws SQLException {

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
