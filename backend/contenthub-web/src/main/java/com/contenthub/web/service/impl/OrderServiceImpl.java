package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.OrderDO;
import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.OrderMapper;
import com.contenthub.common.domain.mapper.OrderProductMapper;
import com.contenthub.common.domain.vo.OrderProductVO;
import com.contenthub.common.utils.Response;
import com.contenthub.web.service.OrderService;
import com.contenthub.web.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserService userService;
    @Autowired
    private OrderProductMapper orderProductMapper;
    @Override
    @Transactional
    public int addOrder(OrderDO orderDO) {
        return orderMapper.insert(orderDO);
    }

    @Override
    public Response getOrderProductList(Integer userId) {
//        UserDO user = userService.findUserByName();
//        Integer id = user.getUserId();
        return Response.success(orderProductMapper.getOrderProductList(userId));
    }
}
