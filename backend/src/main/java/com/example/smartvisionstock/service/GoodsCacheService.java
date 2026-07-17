package com.example.smartvisionstock.service;

import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.repository.GoodsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 商品基础数据缓存服务。
 * 商品信息（SKU、名称、分类等）属于读多写少的高频数据，
 * 通过 @Cacheable 将热点读取缓存（默认内存 / redis profile 下为 Redis），
 * 显著降低数据库压力。库存发生变更时由库存相关服务通过 @CacheEvict 失效。
 */
@Service
public class GoodsCacheService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Cacheable(cacheNames = "goods", key = "#code", unless = "#result == null")
    public Optional<Goods> findByCode(String code) {
        return goodsRepository.findByCode(code);
    }

    @Cacheable(cacheNames = "goods", key = "#id", unless = "#result == null")
    public Optional<Goods> findById(Long id) {
        return goodsRepository.findById(id);
    }
}
