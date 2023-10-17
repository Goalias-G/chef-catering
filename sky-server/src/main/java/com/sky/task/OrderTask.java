package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    @Scheduled(cron = "0 * * * * ?")//每分钟触发一次
    public void processTimeOrder(){
log.info("定时处理超时订单{}", LocalDateTime.now());
        LocalDateTime plusMinutes = LocalDateTime.now().plusMinutes(-15);
        List<Orders> list = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, plusMinutes);
        if (list!=null&&list.size()>0){
            for (Orders order :
                    list) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }
    @Scheduled(cron = "0 0 1 * * ?")//每凌晨一点触发一次
    public void processDeliveryOrder(){
    log.info("处理派送中的订单{}",LocalDateTime.now());
        LocalDateTime plusHours = LocalDateTime.now().plusHours(-1);
        List<Orders> list = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, plusHours);
        if (list!=null&&list.size()>0){
            for (Orders order :
                    list) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
