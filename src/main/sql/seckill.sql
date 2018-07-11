-- 秒杀执行存储过程

DELIMITER  $$ -- console ;

-- 定义存储过程
-- 参数:in 输入参数; out 输出参数
-- row_count():修改上一条sql修改类型的影响行数
CREATE PROCEDURE seckill.execute_seckill
  (in v_seckill_id bigint, in v_phone bigint,
   in v_kill_time timestamp, out r_result int)
  BEGIN
    DECLARE insert_count int DEFAULT 0;
    START TRANSACTION ;
    INSERT IGNORE into success_killed
      (seckill_id, user_phone, create_time)
    VALUES
      (v_seckill_id, v_phone, v_kill_time);

    SELECT row_count() INTO insert_count;

    IF (insert_count = 0) THEN
      ROLLBACK;
      SET r_result = -1;
    ELSEIF (insert_count < 0) THEN
      ROLLBACK;
      SET r_result = -2;
    ELSE
      UPDATE seckill
      SET number = number - 1
      WHERE seckill_id = v_seckill_id
      AND   end_time > v_kill_time
      AND   start_time < v_kill_time
      AND   number > 0;

      SELECT row_count() INTO insert_count;

      IF (insert_count = 0) THEN
        ROLLBACK;
        SET r_result = 0;
      ELSEIF (insert_count < 0) THEN
        ROLLBACK;
        SET r_result = -2;
      ELSE
        COMMIT;
        set r_result = 1;
      END IF;
    END IF;
  END
$$
-- 存储过程结束

DELIMITER ;

set @r_result = -3;
-- 执行存储过程
call execute_seckill(1000, 18362973020, now(), @r_result);

-- 获取结果
SELECT @r_result;

-- 存储过程优化: 事务行级锁持有的时间
--             不要过度依赖存储过程
--             QPS一个秒杀单6000qps