package com.lwh.seckill.service;

import com.lwh.seckill.dto.Exposer;
import com.lwh.seckill.dto.SeckillExecution;
import com.lwh.seckill.entity.Seckill;
import com.lwh.seckill.exception.RepeatKillException;
import com.lwh.seckill.exception.SeckillCloseException;
import com.lwh.seckill.exception.SeckillException;

import java.util.List;

/**
 * 业务接口:站在使用者角度设计接口
 * 三个方面:方法定义粒度、参数、返回类型(return 类型/异常)
 */
public interface SeckillService {

    /**
     * 查询所有秒杀记录
     */
    List<Seckill> getSeckillList();

    /**
     * 查询单个秒杀记录
     */
    Seckill getById(long seckillId);

    /**
     * 秒杀开启时输出秒杀接口地址,
     * 否则输出系统时间和秒杀时间
     */
    Exposer exportSeckillUrl(long seckillId);

    /**
     * 执行秒杀操作
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException;

    /**
     * 执行秒杀操作 by 存储过程
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5);
}
