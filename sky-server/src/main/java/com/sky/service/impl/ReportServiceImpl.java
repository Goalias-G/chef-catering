package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }
        String s1 = StringUtils.join(dateList, ",");

        List<Double> turnoverList=new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map=new HashMap<>();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover=orderMapper.sumByMap(map);
            turnover=turnover==null?0.0:turnover;
            turnoverList.add(turnover);
        }
        String s2 = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(s1)
                .turnoverList(s2)
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> newUserList=new ArrayList<>();
        List<Integer> totalUserList=new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map=new HashMap<>();
            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);
            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        String s1 = StringUtils.join(dateList, ",");
        String s2 = StringUtils.join(totalUserList, ",");
        String s3 = StringUtils.join(newUserList, ",");
        return new UserReportVO(s1,s2,s3);
    }

    @Override
    public OrderReportVO getOrderReportVO(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> allOrderList=new ArrayList<>();
        List<Integer> validOrderList=new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String,Object> map=new HashMap<>();
            map.put("begin",beginTime);
            map.put("end",endTime);
            Integer allOrder=orderMapper.countByMap(map);
            map.put("status",Orders.COMPLETED);
            Integer validOrders = orderMapper.countByMap(map);
            allOrderList.add(allOrder);
            validOrderList.add(validOrders);
        }
        String s1 = StringUtils.join(dateList, ",");
        String s2 = StringUtils.join(allOrderList, ",");
        String s3 = StringUtils.join(validOrderList, ",");
        Integer totalOrdersCount = allOrderList.stream().reduce(Integer::sum).get();
        Integer validOrdersCount = validOrderList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate=0.0;
        if (totalOrdersCount!=0) {
            orderCompletionRate = validOrdersCount.doubleValue() / totalOrdersCount;
        }
        return new OrderReportVO(s1,s2,s3,totalOrdersCount,validOrdersCount,orderCompletionRate);
    }

    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {

            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
            List<GoodsSalesDTO> list=orderMapper.getSalesTop10(beginTime,endTime);
        List<String> nameList = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String sName = StringUtils.join(nameList, ",");
        String sNumber = StringUtils.join(numberList, ",");

        return new SalesTop10ReportVO(sName,sNumber);
    }
}
