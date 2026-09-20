package com.contenthub.web.service;

import com.contenthub.common.domain.dos.OrderDO;
import com.contenthub.common.domain.vo.OrderProductVO;
import com.contenthub.common.utils.Response;

import java.util.List;

public interface OrderService {
    int addOrder(OrderDO orderDO);
    Response getOrderProductList(Integer userId);
}
