package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;
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

    @Override
    public void exportBusinessDate(HttpServletResponse response) {
        LocalDate dataBegin=LocalDate.now().minusDays(30);
        LocalDate dataEnd = LocalDate.now().minusDays(1);
        BusinessDataVO dataVO = workspaceService.getBusinessData(LocalDateTime.of(dataBegin, LocalTime.MIN), LocalDateTime.of(dataEnd, LocalTime.MAX));
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheetAt(0);
            sheet.getRow(1).getCell(1).setCellValue("时间："+dataBegin+"--"+dataEnd);
            sheet.getRow(3).getCell(2).setCellValue(dataVO.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(dataVO.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(dataVO.getNewUsers());
            sheet.getRow(4).getCell(2).setCellValue(dataVO.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(dataVO.getUnitPrice());
            for (int i = 0; i <30 ; i++) {
                LocalDate days = dataBegin.plusDays(i);
                BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(days, LocalTime.MIN), LocalDateTime.of(days, LocalTime.MAX));
                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(days.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);
            excel.close();
            outputStream.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
