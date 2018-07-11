package com.lwh.seckill.dao;

import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test {

    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spring/spring-dao.xml");
        SqlSessionFactoryBean sqlSessionFactory = ctx.getBean("SqlSessionFactory", SqlSessionFactoryBean.class);
        //sqlSessionFactory.

        System.out.println(sqlSessionFactory);
    }
}
