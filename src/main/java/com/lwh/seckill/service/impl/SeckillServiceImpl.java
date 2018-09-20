package com.lwh.seckill.service.impl;

import com.lwh.seckill.dao.SeckillDao;
import com.lwh.seckill.dao.SuccessKilledDao;
import com.lwh.seckill.dao.cache.RedisDao;
import com.lwh.seckill.dto.Exposer;
import com.lwh.seckill.dto.SeckillExecution;
import com.lwh.seckill.entity.Seckill;
import com.lwh.seckill.entity.SuccessKilled;
import com.lwh.seckill.enums.SeckillStateEnum;
import com.lwh.seckill.exception.RepeatKillException;
import com.lwh.seckill.exception.SeckillCloseException;
import com.lwh.seckill.exception.SeckillException;
import com.lwh.seckill.service.SeckillService;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeckillServiceImpl implements SeckillService{

    private Logger logger = LoggerFactory.getLogger(SeckillServiceImpl.class);

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;

    @Autowired
    private RedisDao redisDao;

    //md5
    private final String salt = "jjf*73&+23438jfsjdfjsljd!^&*##jfdedj";

    @Override
    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, 4);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        //优化点:缓存优化,一致性建立在超时的基础上
        //1.访问redis
        Seckill seckill = redisDao.getSeckill(seckillId);

        if(seckill == null){
            //2.访问数据库
            seckill = getById(seckillId);
            if(seckill == null){
                return new Exposer(false, seckillId);
            }else {
                //3.放入redis
                redisDao.putSeckill(seckill);
            }
        }

        long startTime = seckill.getStartTime().getTime();
        long endTime = seckill.getEndTime().getTime();
        long nowTime = System.currentTimeMillis();

        if(nowTime < startTime || nowTime > endTime){
            return new Exposer(false, seckillId, nowTime, startTime, endTime);
        }

        String md5 = getMd5(seckillId);
        return new Exposer(true, md5, seckillId);
    }

    private String getMd5(long seckillId){
        String base = seckillId + "/" + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }


    @Override
    @Transactional
    /**
     * 使用注解控制事务方法的优点:
     * 1.开发团队可以达成一个约定,明确标注事务方法的编程风格
     * 2.保证事务方法的执行时间尽可能短,不要穿插其他的网络操作,RPC/HTTP请求(等方法操作时间较长或者剥离到事务方法外部)
     * 3.不是所有的方法都需要事务,如只有一条修改操作,或者只读操作不需要事务控制
     */
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || ! md5.equals(getMd5(seckillId))){
            throw new SeckillException("秒杀数据被篡改了");
        }

        //执行秒杀逻辑: 记录购买行为 + 减库存
        //将记录购买行为insert语句调整放在前面,减少update语句占用行级锁的时间
        Date nowTime = new Date();

        try {
            //记录购买行为
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if(insertCount <= 0){
                //重复秒杀
                throw new RepeatKillException("重复秒杀");
            }else {
                //减库存,热点商品竞争
                int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
                if(updateCount <= 0){
                    //没有更新到记录,秒杀结束,rollback
                    throw new SeckillCloseException("秒杀结束");
                }else {
                    //秒杀成功,commit
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException e){
            throw e;
        } catch (RepeatKillException e){
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            //所有编译期异常转化为运行期异常
            throw new SeckillException("秒杀失败:" + e.getMessage());
        }

    }

    //使用存储过程执行秒杀逻辑
    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5){
        if(md5 == null || ! md5.equals(getMd5(seckillId))){
            throw new SeckillException("秒杀数据被篡改了");
        }

        Date killTime = new Date();

        Map<String, Object> map= new HashMap<>();
        map.put("seckillId", seckillId);
        map.put("phone", userPhone);
        map.put("killTime", killTime);
        map.put("result", null);
        //执行存储过程之后,result会被赋值

        try {
            seckillDao.killByProcedure(map);
            //获取result
            int result = MapUtils.getInteger(map, "result", -2);
            if(result == 1){
                SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
            }else {
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }
}
