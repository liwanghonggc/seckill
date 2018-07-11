package com.lwh.seckill.dao;

import com.lwh.seckill.entity.SuccessKilled;
import org.apache.ibatis.annotations.Param;


public interface SuccessKilledDao {

    /**
     * 插入购买明细,可过滤重复的,因为是联合主键
     * @param seckillId
     * @param userPhone
     * @return
     */
    int insertSuccessKilled(@Param("seckillId") long seckillId, @Param("userPhone") long userPhone);

    /**
     * 根据Id查询SuccessKilled并携带秒杀产品对象实体
     * @param seckillId
     * @return
     */
    SuccessKilled queryByIdWithSeckill(@Param("seckillId") long seckillId, @Param("userPhone") long userPhone);
}

