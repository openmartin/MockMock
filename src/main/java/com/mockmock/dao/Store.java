package com.mockmock.dao;

import com.mockmock.Settings;
import com.mockmock.mail.MailQueue;
import com.mockmock.mail.MockMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pengzili on 2017/3/23.
 */
@Service
public class Store {

    private JdbcTemplate jdbcTemplate;

    public JdbcTemplate getJdbcTemplate(Settings settings) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(settings.getJdbcUrl());
        dataSource.setUsername(settings.getJdbcUser());
        dataSource.setPassword(settings.getJdbcPwd());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate = jdbcTemplate;

        return jdbcTemplate;
    }

    public void addMail(MockMail mockMail){
        String sql = "INSERT into mock_mail(mail_from, mail_to, mail_subject, mail_raw, receive_time, attatch_filename, attatchment) values(?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, mockMail.getFrom(), mockMail.getTo(), mockMail.getSubject(), mockMail.getRawMail(),mockMail.getReceive_time(),
                mockMail.getAttacheFileName(), mockMail.getAttachment());
    }


    public ArrayList<MockMail> getPage(int start, int pageSize) {

        if (start < 0){
            start = 0;
        }
        if (pageSize < 0){
            pageSize = 100;
        }

        String sql = "SELECT * from mock_mail order by receive_time desc limit ?,?";
        List<MockMail> mail_list = jdbcTemplate.query(sql, new Integer[]{start, pageSize}, new MockMailMapper());
        MailQueue mailQueue = new MailQueue();
        ArrayList<MockMail> array_mail_list = new ArrayList(mail_list);

        return array_mail_list;
    }

    public MockMail getMail(long pk) {
        String sql = "SELECT * from mock_mail where id=?";
        return jdbcTemplate.queryForObject(sql, new MockMailMapper(), pk);
    }

    public void deleteMail(long pk) {
        String sql = "DELETE from mock_mail where id=?";
        jdbcTemplate.update(sql, pk);
    }

    public void deleteAll() {
        String sql = "DELETE from mock_mail";
        jdbcTemplate.update(sql);
    }

    class MockMailMapper implements RowMapper<MockMail> {

        public MockMail mapRow(ResultSet rs, int rowNum) throws SQLException {

            MockMail mockMail = new MockMail();
            mockMail.setId(rs.getLong("id"));
            mockMail.setFrom(rs.getString("mail_from"));
            mockMail.setFrom(rs.getString("mail_to"));
            mockMail.setSubject(rs.getString("mail_subject"));
            mockMail.setReceive_time(rs.getDate("receive_time"));

            try {
                Blob rawMailBlob =  rs.getBlob("mail_raw");
                if(rawMailBlob != null) {
                    InputStream rawMailStrem = rs.getBlob("mail_raw").getBinaryStream();
                    String rawMail = IOUtils.toString(rawMailStrem);
                    if (rawMail == null || rawMail.equals("")) {
                        // do nothing
                    }else{
                        mockMail.loadMail(rawMail);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return mockMail;
        }

    }
}
