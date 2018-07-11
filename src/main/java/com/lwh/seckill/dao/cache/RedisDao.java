package com.lwh.seckill.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.lwh.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


public class RedisDao {

    private final Logger logger = LoggerFactory.getLogger(RedisDao.class);

    private JedisPool jedisPool;

    public RedisDao(String ip, int port){
        jedisPool = new JedisPool(ip, port);
    }

    //根据该schema序列化和反序列化对象
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    public Seckill getSeckill(long seckillId){
        //redis操作逻辑
        try {
            Jedis jedis = jedisPool.getResource();

            try {
                String key = "seckill:" + seckillId;
                //并没有实现内部序列化操作
                //get->byte[] ->反序列化 -> Object(Seckill)
                //采用自定义序列化方式
                byte[] bytes = jedis.get(key.getBytes());
                if(bytes != null){
                    //先获取空对象
                    Seckill seckill = schema.newMessage();
                    //将bytes中的数据依据schema赋值到seckill中
                    //此序列化效果比jdk自带的好
                    ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
                    return seckill;
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public String putSeckill(Seckill seckill){
        //set Object(seckill) -> 序列化 -> byte[]
        try {
            Jedis jedis = jedisPool.getResource();

            try {
                String key = "seckill:" + seckill.getSeckillId();
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));

                int timeout = 60 * 60;
                String result = jedis.setex(key.getBytes(), timeout, bytes);
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
