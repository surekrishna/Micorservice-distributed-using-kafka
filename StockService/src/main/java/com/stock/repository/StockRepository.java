package com.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stock.entity.WareHouse;

@Repository
public interface StockRepository extends JpaRepository<WareHouse, Long> {

	Iterable<WareHouse> findByItem(String item);
}
