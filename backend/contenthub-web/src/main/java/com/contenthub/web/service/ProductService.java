package com.contenthub.web.service;

import com.contenthub.common.utils.Response;

public interface ProductService {
    /**
     * 查询所有商品
     * @return {@link Response}
     */
    Response queryAllProducts();

    Response findProductById(Integer id);
}
