package com.ureka.team3.utong_scheduler.price.repository;

import com.ureka.team3.utong_scheduler.price.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceRepository extends JpaRepository<Price, String> {

}
