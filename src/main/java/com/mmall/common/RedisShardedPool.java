package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.*;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

import java.util.ArrayList;
import java.util.List;

/**
 * @author redLi
 * @package com.mmall.util
 * @time 2018/03/15 23:16
 * @description:
 */
public class RedisShardedPool {
    /**
     * jedis连接池
     */
    private static ShardedJedisPool pool;
    /**
     * redis1Ip
     */
    private static String redis1Ip = PropertiesUtil.getProperty("redis1.ip");
    /**
     * redis1Port
     */
    private static Integer redis1Port = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));
    /**
     * redis2Ip
     */
    private static String redis2Ip = PropertiesUtil.getProperty("redis2.ip");
    /**
     * redis2Port
     */
    private static Integer redis2Port = Integer.parseInt(PropertiesUtil.getProperty("redis2.port"));
    /**
     * 最大连接数
     */
    private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total", "20"));
    /**
     * 在jedisPool中最大的idle(空闲的)的jedis实例个数
     */
    private static Integer maxIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle", "10"));
    /**
     * 在jedispool中最小的idle(空闲的)的jedis实例个数
     */
    private static Integer minIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle", "2"));
    /**
     * 在borrow一个jedis实例时，是否进行验证操作，若赋值true，则可用
     */
    private static Boolean testOnBorrow = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow", "true"));
    /**
     * 在borrow一个jedis实例时，是否进行验证操作，若赋值true，则放回jedispoll的jedis实例可用
     */
    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return", "false"));

    private static void initPool() {
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);

        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);

        //连接耗尽的时候，是否会阻塞，false回抛出异常，true阻塞直到超时
        config.setBlockWhenExhausted(true);

        JedisShardInfo info1 = new JedisShardInfo(redis1Ip, redis1Port, 1000 * 2);
        JedisShardInfo info2 = new JedisShardInfo(redis2Ip, redis2Port, 1000 * 2);

        List<JedisShardInfo> jedisShardInfoList = new ArrayList<JedisShardInfo>(2);

        jedisShardInfoList.add(info1);
        jedisShardInfoList.add(info2);

        pool = new ShardedJedisPool(config, jedisShardInfoList, Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN);
    }

    static {
        initPool();
    }

    public static ShardedJedis getShardedJedis() {
        return pool.getResource();
    }

    public static void returnBrokenResource(ShardedJedis shardedJedis) {
        pool.returnBrokenResource(shardedJedis);
    }

    public static void returnResource(ShardedJedis shardedJedis) {
        pool.returnResource(shardedJedis);
    }

    public static void main(String[] args) {
        ShardedJedis shardedJedis = pool.getResource();

        for (int i = 0; i < 10; i++) {
            shardedJedis.set("key" + i, "value" + i);
        }

        returnResource(shardedJedis);
        //临时调用，销毁连接池中的所有连接
        //pool.destroy();
        System.out.println("program is end");
    }
}
