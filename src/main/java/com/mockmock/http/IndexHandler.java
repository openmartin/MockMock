package com.mockmock.http;

import com.mockmock.dao.Store;
import com.mockmock.htmlbuilder.FooterHtmlBuilder;
import com.mockmock.htmlbuilder.HeaderHtmlBuilder;
import com.mockmock.htmlbuilder.MailListHtmlBuilder;
import org.eclipse.jetty.server.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Service
public class IndexHandler extends BaseHandler
{
    private HeaderHtmlBuilder headerHtmlBuilder;
    private FooterHtmlBuilder footerHtmlBuilder;
    private MailListHtmlBuilder mailListHtmlBuilder;

    private Store store;

    @Override
    public void handle(String target, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException
    {
        if(! isMatch(target))
        {
            return;
        }

        setDefaultResponseOptions(response);

        int start = 0;
        int page = getPage(target, request);
        int pageSize = 100;

        start = pageSize*page;

        String header = headerHtmlBuilder.build();

        mailListHtmlBuilder.setMailQueue(store.getPage(start, pageSize));
        String body = mailListHtmlBuilder.build();

        String footer = footerHtmlBuilder.build();

        response.getWriter().print(header + body + footer);

        request.setHandled(true);
    }

    @Autowired
    public void setHeaderHtmlBuilder(HeaderHtmlBuilder headerHtmlBuilder) {
        this.headerHtmlBuilder = headerHtmlBuilder;
    }

    @Autowired
    public void setFooterHtmlBuilder(FooterHtmlBuilder footerHtmlBuilder) {
        this.footerHtmlBuilder = footerHtmlBuilder;
    }

    @Autowired
    public void setMailListHtmlBuilder(MailListHtmlBuilder mailListHtmlBuilder) {
        this.mailListHtmlBuilder = mailListHtmlBuilder;
    }

    @Autowired
    public void setStore(Store store){
        this.store = store;
    }

    private boolean isMatch(String target)
    {
        if(target.equals("/")){
            return true;
        }
        return false;
    }

    private int getPage(String target, Request request)
    {
        int page = 0;
        String paraPage = request.getParameter("page");
        try {
            page = Integer.valueOf(paraPage);
        } catch (Exception e) {
            page = 0;
        }

        return page;
    }

}
