package com.lwh.seckill.service;

import com.lwh.seckill.dto.Exposer;
import com.lwh.seckill.dto.SeckillExecution;
import com.lwh.seckill.entity.Seckill;
import com.lwh.seckill.exception.RepeatKillException;
import com.lwh.seckill.exception.SeckillCloseException;
import com.lwh.seckill.exception.SeckillException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml",
                       "classpath:spring/spring-service.xml"})
public class SeckillServiceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Test
    public void getSeckillList() {
        List<Seckill> list = seckillService.getSeckillList();
        logger.info("list={}", list);
    }

    @Test
    public void getById() {
        long id = 1000;
        Seckill seckill = seckillService.getById(id);
        logger.info("seckill={}", seckill);

    }

    @Test
    public void exportSeckillUrl() {
        long id = 1000;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        logger.info("exposer={}", exposer);
        //exposed=true, md5='3c8b759f57edda8a62c57f79259db144', seckillId=1000, now=0, start=0, end=0
    }

    @Test
    public void executeSeckill() {
        long id = 1000;
        long phone = 18362973021L;
        String md5 = "3c8b759f57edda8a62c57f79259db144";
        try {
            SeckillExecution execution = seckillService.executeSeckill(id, phone, md5);
            logger.info("result={}", execution);
        } catch (RepeatKillException e) {
            logger.info(e.getMessage(), e);
        } catch (SeckillCloseException e){
            logger.info(e.getMessage(), e);
        }
    }

    @Test
    public void executeSeckillProcedure(){
        long seckillId = 1000;
        long phone = 18362973020L;
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        if(exposer.isExposed()){
            String md5 = exposer.getMd5();
            SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId, phone, md5);
            logger.info(execution.getStateInfo());
        }
    }
}