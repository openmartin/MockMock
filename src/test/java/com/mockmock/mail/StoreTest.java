package com.mockmock.mail;

import com.mockmock.Settings;
import com.mockmock.dao.Store;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

/**
 * Created by Pengzili on 2017/3/23.
 */
public class StoreTest {

    private final String jdbcUrl = "jdbc:mysql://localhost:3306/test";
    private final String jdbcUser = "root";
    private final String jdbcPwd = "123456";

    private Settings settings;
    private Store store;

    @Before
    public void setUp(){
        settings = new Settings();
        settings.setJdbcUrl(jdbcUrl);
        settings.setJdbcUser(jdbcUser);
        settings.setJdbcPwd(jdbcPwd);

        store = new Store();
        store.getJdbcTemplate(settings);
    }

    @Test
    public void testAdd(){
        MockMail mockMail = new MockMail();
        mockMail.setFrom("test@example.com");
        mockMail.setTo("target@example.com");
        mockMail.setSubject("test0");
        mockMail.setRawMail("raw mail");
        mockMail.setReceive_time(new Date());

        store.addMail(mockMail);
        System.out.println(mockMail.getId());
    }

//    @Test
//    public void testGetMail(){
//        MockMail mockMail = store.getMail(2);
//        System.out.println(mockMail.getSubject());
//    }

    @Test
    public void testDeleteMail(){
        store.deleteMail(1);
    }

    @Test
    public void testDeleteAll(){
        store.deleteAll();
    }

    @Test
    public void testGetPage(){
        store.getPage(0, 100);
    }

}
